package com.devgrapher.ocebook;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.devgrapher.ocebook.model.OpenPageRequest;
import com.devgrapher.ocebook.model.Page;
import com.devgrapher.ocebook.model.PaginationInfo;
import com.devgrapher.ocebook.model.ViewerSettings;
import com.devgrapher.ocebook.readium.ObjectHolder;
import com.devgrapher.ocebook.readium.ReadiumContext;
import com.devgrapher.ocebook.util.Laz;
import com.devgrapher.ocebook.util.PaginationPrefs;
import com.devgrapher.ocebook.util.WebViewMotionHandler;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.SpineItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WebViewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WebViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebViewFragment extends Fragment {
    private static final String TAG = WebViewFragment.class.toString();
    private static final String ARG_CONTAINER_ID = "container";

    private OnFragmentInteractionListener mListener;
    private Container mContainer;
    private PaginationPrefs mLastPageinfo;
    // protected to be accessed in HiddenRendererFragment
    protected ReadiumContext mReadiumCtx;
    protected ViewerSettings mViewerSettings;
    protected Context mContext;

    private WebView mWebView;

    public WebViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param containerId container id held by ObjectHolder.
     * @return A new instance of fragment WebViewFragment.
     */
    public static WebViewFragment newInstance(long containerId) {
        WebViewFragment fragment = new WebViewFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_CONTAINER_ID, containerId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mContainer = ObjectHolder.getInstance().getContainer(
                    getArguments().getLong(ARG_CONTAINER_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);
        mWebView = (WebView) view.findViewById(R.id.webView);

        initWebView();
        initReadium();

        mViewerSettings = new ViewerSettings(
                ViewerSettings.SyntheticSpreadMode.AUTO,
                ViewerSettings.ScrollMode.AUTO, 100, 20);

        return view;
    }

    private void initWebView() {
        if (App.isDebugging()) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.setWebViewClient(new ReadiumWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setOnTouchListener(
                new WebViewMotionHandler(mContext, new WebViewMotionHandler.OnMotionListener() {
                    @Override
                    public void onMoveNextPage() {
                        mReadiumCtx.getApi().openPageRight();
                    }

                    @Override
                    public void onMovePreviousPage() {
                        mReadiumCtx.getApi().openPageLeft();
                    }

                    @Override
                    public void onOpenMenu() {
                        mListener.onOpenMenu();
                    }
                }));
    }

    private void initReadium() {
        mReadiumCtx = new ReadiumContext(new ReadiumContext.WebViewDelegate() {
                @Override
                public void evaluateJavascript(String script) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "WebView evaluateJavascript: " + script + "");
                        mWebView.evaluateJavascript(script,
                                str -> Log.d(TAG, "WebView evaluateJavascript RETURN: " + str));
                    });
                }

                @Override
                public InputStream openAsset(String fileName) {
                    try {
                        return mContext.getAssets().open(fileName);
                    } catch (IOException e) {
                        Log.e(TAG, "Asset Open Fail! : " + fileName);
                        return null;
                    }
                }

                @Override
                public void addJavascriptInterface(ReadiumContext.JsInterface jsInterface, String name) {
                    mWebView.addJavascriptInterface(jsInterface, name);
                }

                @Override
                public void loadUrl(String url) {
                    mWebView.loadUrl(url);
                }

            }, createPageEventListener(), mContainer);

        ObjectHolder.getInstance().putContext(mReadiumCtx.getId(), mReadiumCtx);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mContext = activity.getApplicationContext();
            if (activity instanceof OnFragmentInteractionListener) {
                mListener = (OnFragmentInteractionListener) activity;
            } else {
                throw new RuntimeException(activity.toString()
                        + " must implement OnFragmentInteractionListener");
            }

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (getActivity().isFinishing()) {
            mReadiumCtx.dispose();
        }
        ObjectHolder.getInstance().removeContext(mReadiumCtx.getId());

        ((ViewGroup) mWebView.getParent()).removeView(mWebView);
        mWebView.removeAllViews();
        mWebView.clearCache(true);
        mWebView.clearHistory();
        mWebView.destroy();
    }

    public void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onPackageOpen(ReadiumContext readiumContext);
        void onPageChanged(int pageIndex, int spineIndex);
        void onOpenMenu();
    }

    public final class ReadiumWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Laz.y(()-> Log.d(TAG, "onPageStarted: " + url));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Laz.y(()-> Log.d(TAG, "onPageFinished: " + url));
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            Laz.y(()-> Log.d(TAG, "onLoadResource: " + url));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Laz.y(()-> Log.d(TAG, "shouldOverrideUrlLoading: " + req.getUrl()));
            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Laz.y(()-> Log.d(TAG, "-------- interceptRequest: " + req.getUrl().toString()));

            WebResourceResponse res = mReadiumCtx.handleWebRequest(req.getUrl().toString());

            if (res == null) {
                return super.shouldInterceptRequest(view, req);
            }

            return res;
        }
    }

    // Create PageEventListener which delivers web browsing events.
    // This method is meant to be overrided in HiddenReadererFragment.
    public ReadiumContext.PageEventListener createPageEventListener() {
        return new ReadiumContext.PageEventListener() {

            @Override
            public void onReaderInitialized() {
                runOnUiThread(() -> {
                    final Package pckg = mReadiumCtx.getPackage();

                    // Get last open page number.
                    mLastPageinfo = new PaginationPrefs(mContext);
                    SpineItem spine = pckg.getSpineItems().get(mLastPageinfo.getSpineIndex());

                    mReadiumCtx.getApi().openBook(pckg, mViewerSettings,
                            OpenPageRequest.fromIdref(spine.getIdRef()));

                    mListener.onPackageOpen(mReadiumCtx);
                });
            }

            @Override public void onContentLoaded() {}
            @Override public void onPageLoaded() {}
            @Override public void onIsMediaOverlayAvailable(String available) {}
            @Override public void onMediaOverlayStatusChanged(String status) {}
            @Override public void onMediaOverlayTTSSpeak() {}
            @Override public void onMediaOverlayTTSStop() {}
            @Override public void getBookmarkData(String bookmarkData) {}
            @Override public void onSettingsApplied() {}

            @Override
            public void onPaginationChanged(PaginationInfo currentPagesInfo) {
                Laz.y(()-> Log.d(TAG, "onPaginationChanged: " + currentPagesInfo));

                List<Page> openPages = currentPagesInfo.getOpenPages();
                if (openPages.isEmpty())
                    return;

                runOnUiThread(() -> {
                    final Package pckg = mReadiumCtx.getPackage();
                    final Page page = openPages.get(0);

                    SpineItem spineItem = pckg.getSpineItem(page.getIdref());
                    boolean isFixedLayout = spineItem.isFixedLayout(pckg);
                    mWebView.getSettings().setBuiltInZoomControls(isFixedLayout);
                    mWebView.getSettings().setDisplayZoomControls(false);

                    if (mLastPageinfo != null) {
                        int newPage = mLastPageinfo.recalculateSpinePage(
                                page.getSpineItemPageCount());
                        Laz.y(()-> Log.d("PaginationPref", "" + newPage));

                        mReadiumCtx.getApi().openSpineItemPage(spineItem.getIdRef(), newPage);

                        // Set null not to reuse.
                        mLastPageinfo = null;
                    } else {
                        PaginationPrefs.save(mContext,
                                page.getSpineItemIndex(),
                                page.getSpineItemPageIndex(),
                                page.getSpineItemPageCount());
                    }

                    mListener.onPageChanged(page.getSpineItemPageIndex(), page.getSpineItemIndex());
                });
            }
        };
    }

}