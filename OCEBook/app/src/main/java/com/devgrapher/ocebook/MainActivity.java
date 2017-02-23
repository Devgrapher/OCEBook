package com.devgrapher.ocebook;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.devgrapher.ocebook.readium.ObjectHolder;
import com.devgrapher.ocebook.readium.ReadiumContext;
import com.devgrapher.ocebook.readium.TocHelper;
import com.devgrapher.ocebook.util.Laz;
import com.devgrapher.ocebook.util.PageCounts;
import com.devgrapher.ocebook.util.TheBook;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.SdkErrorHandler;
import org.readium.sdk.android.components.navigation.NavigationPoint;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SdkErrorHandler,
        WebViewFragment.OnFragmentInteractionListener,
        HiddenRendererFragment.OnFragmentInteractionListener {

    private final String TAG = MainActivity.class.toString();
    private Container mContainer;
    private ReadiumContext mReadiumCtx;
    private PageCounts mPageCounts;
    private int mCurrentSpinePage;
    private int mCurrentSpine;

    private static long sContainerId;

    private NavigationView mTocNavView;
    private TextView mPageInfoTextView;

    public static final String ASSET_BOOK_PATH = "the_book.epub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Init navigation drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override public void onDrawerSlide(View drawerView, float slideOffset) {}
            @Override public void onDrawerOpened(View drawerView) {
                enableFullScreen(false);
            }
            @Override public void onDrawerClosed(View drawerView) {
                enableFullScreen(true);
            }
            @Override public void onDrawerStateChanged(int newState) {}
        });
        toggle.syncState();

        // Set fulls screen layout
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
        enableFullScreen(true);

        mTocNavView = (NavigationView) findViewById(R.id.nav_view);
        mTocNavView.setNavigationItemSelectedListener(this);

        mPageInfoTextView = (TextView) findViewById(R.id.tv_page_info);

        if (!PrepareBook()) return;

        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentById(R.id.container_web_fragment) == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.container_web_fragment, WebViewFragment.newInstance(sContainerId))
                    .commit();

            fragmentManager.beginTransaction()
                    .add(R.id.container_web_fragment, HiddenRendererFragment.newInstance(sContainerId))
                    .commit();
        }
    }

    private boolean PrepareBook() {
        // Check if the book opened before.
        mContainer = ObjectHolder.getInstance().getContainer(sContainerId);
        if (mContainer == null) {
            File cachePath = null;

            try {
                if (App.isDebugging()) {
                    cachePath = TheBook.makeCacheForce(getApplicationContext(), ASSET_BOOK_PATH);
                } else {
                    cachePath = TheBook.makeCacheIfNecessary(
                            getApplicationContext(), ASSET_BOOK_PATH);
                }
            } catch (IOException e) {
                e.printStackTrace();

                Toast.makeText(getApplicationContext(),
                        getString(R.string.error_file_open),
                        Toast.LENGTH_LONG).show();
                return false;
            }

            mContainer = EPub3.openBook(cachePath.toString());
            if (mContainer == null)
                return false;

            sContainerId = mContainer.getNativePtr();
            ObjectHolder.getInstance().putContainer(sContainerId, mContainer);
        }

        Laz.y(()-> Log.d(MainActivity.class.toString(), mContainer.getName()));

        return true;
    }

    public void enableFullScreen(boolean enable) {
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        if (enable) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        } else {
            uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        decorView.setSystemUiVisibility(uiOptions);
    }

    public void openNavigataionDrawer(boolean open) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (open) {
            drawer.openDrawer(GravityCompat.START);
        }
        else {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            if (mContainer != null) {
                ObjectHolder.getInstance().removeContainer(mContainer.getNativePtr());
                EPub3.closeBook(mContainer);
                sContainerId = 0;
                mContainer = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            openNavigataionDrawer(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.reader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        NavigationPoint nav = TocHelper.getAt(mReadiumCtx.getPackage(), id);
        if (nav != null && mReadiumCtx != null) {
            mReadiumCtx.getApi().openContentUrl(
                    nav.getContent(),mReadiumCtx.getPackage().getTableOfContents().getSourceHref());
        }

        openNavigataionDrawer(false);
        return true;
    }

    @Override
    public boolean handleSdkError(String message, boolean isSevereEpubError) {
        Log.w(TAG, "SDKError(" + (isSevereEpubError ? "w" : "i") + ")" + message);
        if (isSevereEpubError) {
            runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            });
        }
        return true;
    }

    @Override
    public void onPackageOpen(ReadiumContext readiumContext) {
        mReadiumCtx = readiumContext;

        mTocNavView.getMenu().removeGroup(R.id.menu_group_toc);

        AtomicInteger index = new AtomicInteger(0);
        for (NavigationPoint p:
                TocHelper.flatTableOfContents(readiumContext.getPackage())) {
            mTocNavView.getMenu().add(
                    R.id.menu_group_toc,
                    index.getAndIncrement(),
                    Menu.NONE,
                    p.getTitle());
        }

        ((TextView) mTocNavView.findViewById(R.id.tv_nav_book_title))
                .setText(readiumContext.getPackage().getTitle());
    }

    @Override
    public void onPageChanged(int pageIndex, int spineIndex) {
        mCurrentSpine = spineIndex;
        mCurrentSpinePage = pageIndex;

        if (mPageCounts != null && !mPageCounts.isUpdating()) {
            mPageInfoTextView.setText(String.format(Locale.getDefault(), "%d / %d",
                    mPageCounts.calculateCurrentPage(spineIndex, pageIndex) + 1,
                    mPageCounts.getTotalCount()));
        }
    }

    @Override
    public void onOpenMenu() {
        openNavigataionDrawer(true);
    }

    @Override
    public void onBrowsePageInProgress(int spineIndex, int totalSpine, int pageCount) {
        int percent = (int) ((spineIndex + 1) / (float) totalSpine * 100);
        mPageInfoTextView.setText("" + percent + "%");

        // reset
        if (spineIndex == 0) {
            mPageCounts = new PageCounts(totalSpine);
        }

        // Store page counts for each spine.
        mPageCounts.updatePage(spineIndex, pageCount);

        // if done
        if (percent == 100) {
            mPageCounts.updateComplete();
            onPageChanged(mCurrentSpinePage, mCurrentSpine);
        }
    }
}
