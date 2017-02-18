package com.devgrapher.ocebook;

import android.Manifest;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.devgrapher.ocebook.model.ContainerHolder;
import com.devgrapher.ocebook.readium.ReadiumService;
import com.devgrapher.ocebook.readium.TocHelper;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.EPub3;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.SdkErrorHandler;
import org.readium.sdk.android.components.navigation.NavigationPoint;

import java.util.concurrent.atomic.AtomicInteger;


public class ReaderActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SdkErrorHandler,
        WebViewFragment.OnFragmentInteractionListener {

    private final String TAG = ReaderActivity.class.toString();
    private Container mContainer;
    private NavigationView mTocNavView;


    // TODO: 외부에서 값을 받아와야 함
    private final String BOOK_PATH = "/sdcard/ocebook/alice3.epub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mTocNavView = (NavigationView) findViewById(R.id.nav_view);
        mTocNavView.setNavigationItemSelectedListener(this);

        if (!checkPermissions())
            return;

        mContainer = EPub3.openBook(BOOK_PATH);
        if (mContainer == null)
            return;

        Log.d(ReaderActivity.class.toString(), mContainer.getName());
        Long id = mContainer.getNativePtr();
        ContainerHolder.getInstance().put(id, mContainer);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.container_web_fragment, WebViewFragment.newInstance(id))
                .commit();


    }

    private boolean checkPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

        if (mContainer != null) {
            ContainerHolder.getInstance().remove(mContainer.getNativePtr());
            EPub3.closeBook(mContainer);
            mContainer = null;
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Package pckg = mContainer.getDefaultPackage();
        NavigationPoint nav = TocHelper.getAt(pckg, id);
        if (nav != null) {
            ReadiumService.getApi().openContentUrl(
                    nav.getContent(), pckg.getTableOfContents().getSourceHref());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
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
    public void onPackageOpen(Package pckg) {
        mTocNavView.getMenu().removeGroup(R.id.menu_group_toc);

        AtomicInteger index = new AtomicInteger(0);
        TocHelper.flatTableOfContents(pckg)
                .forEach(e -> {
                    mTocNavView.getMenu().add(
                            R.id.menu_group_toc,
                            index.getAndIncrement(),
                            Menu.NONE,
                            e.getTitle());
        });

        ((TextView) mTocNavView.findViewById(R.id.tv_nav_book_title))
                .setText(pckg.getTitle());
    }
}
