package com.devgrapher.ocebook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.devgrapher.ocebook.readium.ReadiumContext;
import com.devgrapher.ocebook.model.ContainerHolder;
import com.devgrapher.ocebook.model.OpenPageRequest;
import com.devgrapher.ocebook.model.Page;
import com.devgrapher.ocebook.model.PaginationInfo;
import com.devgrapher.ocebook.model.ViewerSettings;

import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.SpineItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;


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
    private static final String ARG_CONTAINER_ID = "package";

    private OnFragmentInteractionListener mListener;
    private ViewerSettings mViewerSettings;
    private Container mContainer;
    private ReadiumContext mReadiumCtx;

    private WebView mWebView;
    private TextView mPageInfoTextView;

    public WebViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param containerId container id held by ContainerHolder.
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
            mContainer = ContainerHolder.getInstance().get(
                    getArguments().getLong(ARG_CONTAINER_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);
        mWebView = (WebView) view.findViewById(R.id.webView);
        mPageInfoTextView = (TextView) view.findViewById(R.id.tv_page_info);

        initWebView();
        intReadium();

        mViewerSettings = new ViewerSettings(
                ViewerSettings.SyntheticSpreadMode.AUTO,
                ViewerSettings.ScrollMode.AUTO, 100, 20);

        return view;
    }


    private void initWebView() {
        if ((getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)
                != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.setWebViewClient(new ReadiumWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());

        mWebView.setOnTouchListener((View v, MotionEvent event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        if (event.getX() < v.getWidth() / 2) {
                            Log.d(TAG, "Touch left");
                            mReadiumCtx.getApi().openPageLeft();
                        } else {
                            Log.d(TAG, "Touch right");
                            mReadiumCtx.getApi().openPageRight();
                        }
                }
                return false;
        });
    }

    private void intReadium() {
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
                        return getContext().getAssets().open(fileName);
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

            }, new ReadiumPageEventListener(), mContainer);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
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

        mReadiumCtx.stop();
        //TODO: mWebView.loadUrl(READER_SKELETON);
        ((ViewGroup) mWebView.getParent()).removeView(mWebView);
        mWebView.removeAllViews();
        mWebView.clearCache(true);
        mWebView.clearHistory();
        mWebView.destroy();
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
    }

    public final class ReadiumWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "onPageStarted: " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "onPageFinished: " + url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            Log.d(TAG, "onLoadResource: " + url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "shouldOverrideUrlLoading: " + url);
            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Log.d(TAG, "-------- interceptRequest: " + req.getUrl().toString());

            WebResourceResponse res = mReadiumCtx.handleWebRequest(req.getUrl().toString());

            if (res == null) {
                return super.shouldInterceptRequest(view, req);
            }

            return res;
        }
    }

    public final class ReadiumPageEventListener implements ReadiumContext.PageEventListener {

        @Override
        public void onReaderInitialized() {
            getActivity().runOnUiThread(() -> {
                final Package pkcg = mReadiumCtx.getPackage();
                pkcg.getSpineItems().stream()
                        .findAny()
                        .ifPresent(item -> {
                            mReadiumCtx.getApi().openBook(pkcg, mViewerSettings,
                                    OpenPageRequest.fromIdref(item.getIdRef()));
                            mListener.onPackageOpen(mReadiumCtx);
                        });
            });
        }

        @Override
        public void onPaginationChanged(PaginationInfo currentPagesInfo) {
            Log.d(TAG, "onPaginationChanged: " + currentPagesInfo);
            List<Page> openPages = currentPagesInfo.getOpenPages();
            if (openPages.isEmpty())
                return;

            getActivity().runOnUiThread(() -> {
                final Page page = openPages.get(0);
                mPageInfoTextView.setText(String.format(Locale.getDefault(), "%d / %d",
                        page.getSpineItemPageIndex() + 1,
                        page.getSpineItemPageCount()));
                SpineItem spineItem = mReadiumCtx.getPackage().getSpineItem(page
                        .getIdref());
                boolean isFixedLayout = spineItem
                        .isFixedLayout(mReadiumCtx.getPackage());
                mWebView.getSettings().setBuiltInZoomControls(
                        isFixedLayout);
                mWebView.getSettings()
                        .setDisplayZoomControls(false);
            });
        }
    }

}