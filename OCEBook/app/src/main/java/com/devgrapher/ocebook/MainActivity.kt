package com.devgrapher.ocebook

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast

import com.devgrapher.ocebook.readium.ObjectHolder
import com.devgrapher.ocebook.readium.ReadiumContext
import com.devgrapher.ocebook.readium.TocHelper
import com.devgrapher.ocebook.util.Laz
import com.devgrapher.ocebook.util.PageCounts
import com.devgrapher.ocebook.util.TheBook

import org.readium.sdk.android.Container
import org.readium.sdk.android.EPub3
import org.readium.sdk.android.SdkErrorHandler
import org.readium.sdk.android.components.navigation.NavigationPoint

import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SdkErrorHandler, WebViewFragment.OnFragmentInteractionListener, HiddenRendererFragment.OnFragmentInteractionListener {

    private val TAG = MainActivity::class.java.toString()
    private var mContainer: Container? = null
    private var mReadiumCtx: ReadiumContext? = null
    private var mPageCounts: PageCounts? = null
    private var mCurrentSpinePage: Int = 0
    private var mCurrentSpine: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Init navigation drawer
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                enableFullScreen(false)
            }
            override fun onDrawerClosed(drawerView: View) {
                enableFullScreen(true)
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
        toggle.syncState()

        // Set fulls screen layout
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        decorView.systemUiVisibility = uiOptions
        enableFullScreen(true)

        nav_view.setNavigationItemSelectedListener(this)

        if (!PrepareBook()) return

        val fragmentManager = fragmentManager
        if (fragmentManager.findFragmentById(R.id.container_web_fragment) == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.container_web_fragment, WebViewFragment.newInstance(sContainerId))
                    .commit()

            fragmentManager.beginTransaction()
                    .add(R.id.container_web_fragment, HiddenRendererFragment.newInstance(sContainerId))
                    .commit()
        }
    }

    private fun PrepareBook(): Boolean {
        // Check if the book opened before.
        mContainer = ObjectHolder.instance.getContainer(sContainerId)
        if (mContainer == null) {
            val cachePath: File?

            try {
                if (App.isDebugging) {
                    cachePath = TheBook.makeCacheForce(applicationContext,
                            TheBook.DEFAULT_BOOK_ASSET_PATH)
                } else {
                    cachePath = TheBook.makeCacheIfNecessary(applicationContext,
                            TheBook.DEFAULT_BOOK_ASSET_PATH)
                }
            } catch (e: IOException) {
                e.printStackTrace()

                Toast.makeText(applicationContext,
                        getString(R.string.error_file_open),
                        Toast.LENGTH_LONG).show()
                return false
            }

            mContainer = EPub3.openBook(cachePath.toString())
                    .apply {
                        sContainerId = this.nativePtr
                        ObjectHolder.instance.putContainer(sContainerId, this)
            }
        }

        Laz.yLog(TAG, mContainer?.name ?: "no container")

        return true
    }

    fun enableFullScreen(enable: Boolean) {
        val decorView = window.decorView
        val uiOptions : Int
        if (enable) {
            uiOptions = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_FULLSCREEN
        } else {
            uiOptions = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
        }
        decorView.systemUiVisibility = uiOptions
    }

    fun openNavigationDrawer(open: Boolean) {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (open) {
            drawer.openDrawer(GravityCompat.START)
        } else {
            drawer.closeDrawer(GravityCompat.START)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            mContainer?.let {
                ObjectHolder.instance.removeContainer(mContainer!!.nativePtr)
                EPub3.closeBook(mContainer!!)
                sContainerId = 0
                mContainer = null
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            openNavigationDrawer(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.reader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        mReadiumCtx?.let { readiumCtx ->
            TocHelper.getAt(readiumCtx.pckg, item.itemId)?.let {
                nav: NavigationPoint ->
                        readiumCtx.api.openContentUrl(
                                nav.content,
                                readiumCtx.pckg.tableOfContents.sourceHref)
            }
        }

        openNavigationDrawer(false)
        return true
    }

    override fun handleSdkError(message: String, isSevereEpubError: Boolean): Boolean {
        Log.w(TAG, "SDKError(" + (if (isSevereEpubError) "w" else "i") + ")" + message)
        if (isSevereEpubError) {
            runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
        }
        return true
    }

    override fun onPackageOpen(readiumContext: ReadiumContext) {
        mReadiumCtx = readiumContext

        nav_view.menu.removeGroup(R.id.menu_group_toc)

        val index = AtomicInteger(0)
        for (p in TocHelper.flatTableOfContents(readiumContext.pckg)) {
            nav_view.menu.add(
                    R.id.menu_group_toc,
                    index.getAndIncrement(),
                    Menu.NONE,
                    p.title)
        }

        tv_nav_book_title.text = readiumContext.pckg.title
    }

    override fun onPageChanged(pageIndex: Int, spineIndex: Int) {
        mCurrentSpine = spineIndex
        mCurrentSpinePage = pageIndex

        mPageCounts?.let {
            if (!it.isUpdating) {
                tv_page_info.text = "%d / %d".format(
                        it.calculateCurrentPage(spineIndex, pageIndex) + 1,
                        it.totalCount)
            }
        }
    }

    override fun onOpenMenu() {
        openNavigationDrawer(true)
    }

    override fun onBrowsePageInProgress(spineIndex: Int, totalSpine: Int, pageCount: Int) {
        val percent = ((spineIndex + 1) / totalSpine.toFloat() * 100).toInt()
        tv_page_info.text = "%d %%".format(percent)

        // reset
        if (spineIndex == 0) {
            mPageCounts = PageCounts(totalSpine)
        }

        // Store page counts for each spine.
        mPageCounts?.run {
            updatePage(spineIndex, pageCount)
        }

        // if done
        if (percent == 100) {
            mPageCounts?.let {
                it.updateComplete()
                onPageChanged(mCurrentSpinePage, mCurrentSpine)
            }
        }
    }

    companion object {
        private var sContainerId: Long = 0
    }
}
