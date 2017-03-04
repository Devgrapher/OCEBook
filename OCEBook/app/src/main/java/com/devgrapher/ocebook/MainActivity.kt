package com.devgrapher.ocebook

import android.app.FragmentManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
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
import org.readium.sdk.android.components.navigation.NavigationElement
import org.readium.sdk.android.components.navigation.NavigationPoint

import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SdkErrorHandler, WebViewFragment.OnFragmentInteractionListener, HiddenRendererFragment.OnFragmentInteractionListener {

    private val TAG = MainActivity::class.java.toString()
    private var mContainer: Container? = null
    private var mReadiumCtx: ReadiumContext? = null
    private var mPageCounts: PageCounts? = null
    private var mCurrentSpinePage: Int = 0
    private var mCurrentSpine: Int = 0

    private var mTocNavView: NavigationView? = null
    private var mPageInfoTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // Init navigation drawer
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
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

        mTocNavView = findViewById(R.id.nav_view) as NavigationView
        mTocNavView!!.setNavigationItemSelectedListener(this)

        mPageInfoTextView = findViewById(R.id.tv_page_info) as TextView

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
                    cachePath = TheBook.makeCacheForce(applicationContext, ASSET_BOOK_PATH)
                } else {
                    cachePath = TheBook.makeCacheIfNecessary(
                            applicationContext, ASSET_BOOK_PATH)
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

        Laz.y { Log.d(MainActivity::class.java.toString(), mContainer!!.name) }

        return true
    }

    fun enableFullScreen(enable: Boolean) {
        val decorView = window.decorView
        var uiOptions = decorView.systemUiVisibility
        if (enable) {
            uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
        } else {
            uiOptions = uiOptions and View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
        }
        decorView.systemUiVisibility = uiOptions
    }

    fun openNavigataionDrawer(open: Boolean) {
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
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            openNavigataionDrawer(false)
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
        val id = item.itemId

        mReadiumCtx?.let { readiumCtx ->
            TocHelper.getAt(readiumCtx.pckg, id).let { nav: NavigationPoint? ->
                readiumCtx.api.openContentUrl(
                        nav!!.content!!,
                        readiumCtx.pckg.tableOfContents.sourceHref)
            }
        }

        openNavigataionDrawer(false)
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

        mTocNavView!!.menu.removeGroup(R.id.menu_group_toc)

        val index = AtomicInteger(0)
        for (p in TocHelper.flatTableOfContents(readiumContext.pckg)) {
            mTocNavView!!.menu.add(
                    R.id.menu_group_toc,
                    index.getAndIncrement(),
                    Menu.NONE,
                    p.title)
        }

        (mTocNavView!!.findViewById(R.id.tv_nav_book_title) as TextView).text = readiumContext.pckg.title
    }

    override fun onPageChanged(pageIndex: Int, spineIndex: Int) {
        mCurrentSpine = spineIndex
        mCurrentSpinePage = pageIndex

        if (mPageCounts != null && !mPageCounts!!.isUpdating) {
            mPageInfoTextView!!.setText(String.format(Locale.getDefault(), "%d / %d",
                    mPageCounts!!.calculateCurrentPage(spineIndex, pageIndex) + 1,
                    mPageCounts!!.totalCount))
        }
    }

    override fun onOpenMenu() {
        openNavigataionDrawer(true)
    }

    override fun onBrowsePageInProgress(spineIndex: Int, totalSpine: Int, pageCount: Int) {
        val percent = ((spineIndex + 1) / totalSpine.toFloat() * 100).toInt()
        mPageInfoTextView!!.text = "%d %%".format(percent)

        // reset
        if (spineIndex == 0) {
            mPageCounts = PageCounts(totalSpine)
        }

        // Store page counts for each spine.
        mPageCounts!!.updatePage(spineIndex, pageCount)

        // if done
        if (percent == 100) {
            mPageCounts!!.updateComplete()
            onPageChanged(mCurrentSpinePage, mCurrentSpine)
        }
    }

    companion object {

        private var sContainerId: Long = 0

        val ASSET_BOOK_PATH = "the_book.epub"
    }
}
