package com.devgrapher.ocebook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.devgrapher.ocebook.model.ContainerHolder;
import com.devgrapher.ocebook.model.OpenPageRequest;
import com.devgrapher.ocebook.model.Page;
import com.devgrapher.ocebook.model.PaginationInfo;
import com.devgrapher.ocebook.model.ReadiumJSApi;
import com.devgrapher.ocebook.model.ViewerSettings;
import com.devgrapher.ocebook.util.EpubConstants;
import com.devgrapher.ocebook.util.EpubServer;
import com.devgrapher.ocebook.util.HTMLUtil;

import org.json.JSONException;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    private static final String READER_SKELETON = "file:///android_asset/readium-shared-js/reader.html";

    private Package mPackage;
    private OnFragmentInteractionListener mListener;
    private ViewerSettings mViewerSettings;
    private ReadiumJSApi mReadiumJSApi;
    private EpubServer mServer;
    private List<NavigationPoint> mNavPoints;

    private WebView mWebView;


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
            Container container = ContainerHolder.getInstance().get(
                    getArguments().getLong(ARG_CONTAINER_ID));

            mPackage = container.getDefaultPackage();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);
        mWebView = (WebView) view.findViewById(R.id.webView);

        initWebView();

        mServer = new EpubServer(EpubServer.HTTP_HOST, EpubServer.HTTP_PORT,
                mPackage, dataPreProcessor);
        mServer.startServer();

        mViewerSettings = new ViewerSettings(
                ViewerSettings.SyntheticSpreadMode.AUTO,
                ViewerSettings.ScrollMode.AUTO, 100, 20);

        mReadiumJSApi = new ReadiumJSApi(new ReadiumJSApi.JSLoader() {
            @Override
            public void loadJS(String javascript) {
                mWebView.loadUrl(javascript);
            }
        });

        return view;
    }

    private void initWebView() {
        if ((getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)
                != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mWebView.loadUrl(READER_SKELETON);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.setWebViewClient(new EpubWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.addJavascriptInterface(new EpubInterface(), "LauncherUI");

        //TODO: TEST 작성
        mNavPoints = flatNavigationElement(mPackage.getTableOfContents())
                .filter(e -> e instanceof NavigationPoint)
                .map(e -> (NavigationPoint)e)
                .collect(Collectors.toList());
    }

    public Stream<NavigationElement> flatNavigationElement(final NavigationElement elem) {
        return Stream.concat(
                Stream.of(elem),
                elem.getChildren().stream().flatMap(e -> flatNavigationElement(e)));
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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

        mServer.stop();
        mWebView.loadUrl(READER_SKELETON);
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    public final class EpubWebViewClient extends WebViewClient {

        private static final String HTTP = "http";
        private static final String UTF_8 = "utf-8";

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

        private void evaluateJavascript(final String script) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "WebView evaluateJavascript: " + script + "");
                    mWebView.evaluateJavascript(script,
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String str) {
                                    Log.d(TAG, "WebView evaluateJavascript RETURN: " + str);
                                }
                            });
                }
            });
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          String url) {
            Log.d(TAG, "-------- shouldInterceptRequest: " + url);

            if (url != null && url != "undefined") {

                String localHttpUrlPrefix = "http://" + EpubServer.HTTP_HOST
                        + ":" + EpubServer.HTTP_PORT;
                boolean isLocalHttp = url.startsWith(localHttpUrlPrefix);

                // Uri uri = Uri.parse(url);
                // uri.getScheme()

                if (url.startsWith("http") && !isLocalHttp) {
                    Log.d(TAG, "HTTP (NOT LOCAL): " + url);
                    return super.shouldInterceptRequest(view, url);
                }

                String cleanedUrl = cleanResourceUrl(url, false);
                Log.d(TAG, url + " => " + cleanedUrl);

                if (cleanedUrl
                        .matches("\\/?\\d*\\/readium_epubReadingSystem_inject.js")) {
                    Log.d(TAG, "navigator.epubReadingSystem inject ...");

                    // Fake script requested, this is immediately invoked after
                    // epubReadingSystem hook is in place,
                    // => execute js on the reader.html context to push the
                    // global window.navigator.epubReadingSystem into the
                    // iframe(s)

                    evaluateJavascript(EpubConstants.INJECT_EPUB_RSO_SCRIPT_2);

                    return new WebResourceResponse("text/javascript", UTF_8,
                            new ByteArrayInputStream(
                                    "(function(){})()".getBytes()));
                }

                if (cleanedUrl.matches("\\/?readium_MathJax.js")) {
                    Log.d(TAG, "MathJax.js inject ...");

                    InputStream is = null;
                    try {
                        is = getContext().getAssets().open(EpubConstants.PAYLOAD_MATHJAX_ASSET);
                    } catch (IOException e) {

                        Log.e(TAG, "MathJax.js asset fail!");

                        return new WebResourceResponse(null, UTF_8,
                                new ByteArrayInputStream("".getBytes()));
                    }

                    return new WebResourceResponse("text/javascript", UTF_8, is);
                }

                if (cleanedUrl.matches("\\/?readium_Annotations.css")) {
                    Log.d(TAG, "annotations.css inject ...");

                    InputStream is = null;
                    try {
                        is = getContext().getAssets().open(EpubConstants.PAYLOAD_ANNOTATIONS_CSS_ASSET);
                    } catch (IOException e) {

                        Log.e(TAG, "annotations.css asset fail!");

                        return new WebResourceResponse(null, UTF_8,
                                new ByteArrayInputStream("".getBytes()));
                    }

                    return new WebResourceResponse("text/css", UTF_8, is);
                }

                String mime = null;
                int dot = cleanedUrl.lastIndexOf('.');
                if (dot >= 0) {
                    mime = EpubServer.MIME_TYPES.get(cleanedUrl.substring(
                            dot + 1).toLowerCase());
                }
                if (mime == null) {
                    mime = "application/octet-stream";
                }

                ManifestItem item = mPackage.getManifestItem(cleanedUrl);
                String contentType = item != null ? item.getMediaType() : null;
                if (mime != "application/xhtml+xml"
                        && mime != "application/xml" // FORCE
                        && contentType != null && contentType.length() > 0) {
                    mime = contentType;
                }

                if (url.startsWith("file:")) {
                    if (item == null) {
                        Log.e(TAG, "NO MANIFEST ITEM ... " + url);
                        return super.shouldInterceptRequest(view, url);
                    }

                    String cleanedUrlWithQueryFragment = cleanResourceUrl(url,
                            true);
                    String httpUrl = "http://" + EpubServer.HTTP_HOST + ":"
                            + EpubServer.HTTP_PORT + "/"
                            + cleanedUrlWithQueryFragment;
                    Log.e(TAG, "FILE to HTTP REDIRECT: " + httpUrl);

                    try {
                        URLConnection c = new URL(httpUrl).openConnection();
                        ((HttpURLConnection) c).setUseCaches(false);
                        if (mime == "application/xhtml+xml"
                                || mime == "text/html") {
                            ((HttpURLConnection) c).setRequestProperty(
                                    "Accept-Ranges", "none");
                        }
                        InputStream is = c.getInputStream();
                        return new WebResourceResponse(mime, null, is);
                    } catch (Exception ex) {
                        Log.e(TAG,
                                "FAIL: " + httpUrl + " -- " + ex.getMessage(),
                                ex);
                    }
                }
                Log.d(TAG, "RESOURCE FETCH ... " + url);
                return super.shouldInterceptRequest(view, url);
            }

            Log.e(TAG, "NULL URL RESPONSE: " + url);
            return new WebResourceResponse(null, UTF_8,
                    new ByteArrayInputStream("".getBytes()));
        }

        private String cleanResourceUrl(String url, boolean preserveQueryFragment) {

            String cleanUrl = null;

            String httpUrl = "http://" + EpubServer.HTTP_HOST + ":"
                    + EpubServer.HTTP_PORT;
            if (url.startsWith(httpUrl)) {
                cleanUrl = url.replaceFirst(httpUrl, "");
            } else {
                cleanUrl = (url.startsWith(EpubConstants.ASSET_PREFIX)) ? url.replaceFirst(
                        EpubConstants.ASSET_PREFIX, "") : url.replaceFirst("file://", "");
            }

            String basePath = mPackage.getBasePath();
            if (basePath.charAt(0) != '/') {
                basePath = '/' + basePath;
            }
            if (cleanUrl.charAt(0) != '/') {
                cleanUrl = '/' + cleanUrl;
            }
            cleanUrl = (cleanUrl.startsWith(basePath)) ? cleanUrl.replaceFirst(
                    basePath, "") : cleanUrl;

            if (cleanUrl.charAt(0) == '/') {
                cleanUrl = cleanUrl.substring(1);
            }

            if (!preserveQueryFragment) {
                int indexOfQ = cleanUrl.indexOf('?');
                if (indexOfQ >= 0) {
                    cleanUrl = cleanUrl.substring(0, indexOfQ);
                }

                int indexOfSharp = cleanUrl.indexOf('#');
                if (indexOfSharp >= 0) {
                    cleanUrl = cleanUrl.substring(0, indexOfSharp);
                }
            }

            return cleanUrl;
        }
    }

    public class EpubInterface {

        @JavascriptInterface
        public void onPaginationChanged(String currentPagesInfo) {
            Log.d(TAG, "onPaginationChanged: " + currentPagesInfo);
            try {
                PaginationInfo paginationInfo = PaginationInfo
                        .fromJson(currentPagesInfo);
                List<Page> openPages = paginationInfo.getOpenPages();
                if (!openPages.isEmpty()) {
                    final Page page = openPages.get(0);
                }
            } catch (JSONException e) {
                Log.e(TAG, "" + e.getMessage(), e);
            }
        }

        @JavascriptInterface
        public void onSettingsApplied() {
        }

        @JavascriptInterface
        public void onReaderInitialized() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mNavPoints.size() == 0)
                        return;

                    NavigationPoint nav = mNavPoints.get(0);
                    OpenPageRequest openPageRequest =
                            OpenPageRequest.fromContentUrl(nav.getContent(),
                                    mPackage.getTableOfContents().getSourceHref());

                    mReadiumJSApi.openBook(mPackage, mViewerSettings, openPageRequest);
                }
            });

        }

        @JavascriptInterface
        public void onContentLoaded() {
        }

        @JavascriptInterface
        public void onPageLoaded() {
        }

        @JavascriptInterface
        public void onIsMediaOverlayAvailable(String available) {
        }

        @JavascriptInterface
        public void onMediaOverlayStatusChanged(String status) {
        }

        @JavascriptInterface
        public void onMediaOverlayTTSSpeak() {
        }

        @JavascriptInterface
        public void onMediaOverlayTTSStop() {
        }

        @JavascriptInterface
        public void getBookmarkData(final String bookmarkData) {
        }
    }

    private final EpubServer.DataPreProcessor dataPreProcessor = new EpubServer.DataPreProcessor() {
        private int mEpubRsoInjectCounter;

        @Override
        public byte[] handle(byte[] data, String mime, String uriPath,
                             ManifestItem item) {
            if (mime == null
                    || (mime != "text/html" && mime != "application/xhtml+xml")) {
                return null;
            }

            Log.d(TAG, "PRE-PROCESSED HTML: " + uriPath);

            String htmlText = new String(data, Charset.forName("UTF-8"));

            String newHtml = htmlText;

            // Set up the script tags to add to the head
            String tagsToInjectToHead = EpubConstants.INJECT_HEAD_EPUB_RSO_1
                    // Slightly change fake script src url with an
                    // increasing count to prevent caching of the
                    // request
                    + String.format(EpubConstants.INJECT_HEAD_EPUB_RSO_2,
                    ++mEpubRsoInjectCounter);
            // Checks for the existance of MathML => request
            // MathJax payload
            if (newHtml.contains("<math") || newHtml.contains("<m:math")) {
                tagsToInjectToHead += EpubConstants.INJECT_HEAD_MATHJAX;
            }

            newHtml = HTMLUtil.htmlByInjectingIntoHead(newHtml,
                    tagsToInjectToHead);

            // Log.d(TAG, "HTML head inject: " + newHtml);

            return newHtml.getBytes();
        }
    };
}