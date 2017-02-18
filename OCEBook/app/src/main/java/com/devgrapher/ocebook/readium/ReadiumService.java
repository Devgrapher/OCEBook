package com.devgrapher.ocebook.readium;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;

import com.devgrapher.ocebook.model.PaginationInfo;
import com.devgrapher.ocebook.model.ReadiumJSApi;

import org.json.JSONException;
import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;

import java.io.InputStream;

/**
 * Created by Brent on 2/17/17.
 */

public class ReadiumService {
    private static final String TAG = ReadiumService.class.toString();
    private static final String READER_SKELETON = "file:///android_asset/readium-shared-js/reader.html";

    private ScriptProcessor mScriptProcessor;
    private Package mPackage;
    private WebServer mServer;
    private ReadiumJSApi mJSApi;

    private WebViewDelegate mWebViewDelegate;
    private PageEventListener mEventListener;

    public interface WebViewDelegate {
        void evaluateJavascript(final String script);
        InputStream openAsset(String fileName);
        void addJavascriptInterface(JsInterface jsInterface, String name);
        void loadUrl(String url);
    }

    public interface PageEventListener {
        default void onPaginationChanged(PaginationInfo currentPagesInfo) {}
        default void onSettingsApplied() {}
        default void onReaderInitialized() {}
        default void onContentLoaded() {}
        default void onPageLoaded() {}
        default void onIsMediaOverlayAvailable(String available) {}
        default void onMediaOverlayStatusChanged(String status) {}
        default void onMediaOverlayTTSSpeak() {}
        default void onMediaOverlayTTSStop() {}
        default void getBookmarkData(final String bookmarkData) {}
    }

    private final WebServer.DataPreProcessor dataPreProcessor =
            new WebServer.DataPreProcessor() {

        @Override
        public byte[] handle(byte[] data, String mime, String uriPath,
                             ManifestItem item) {
            if (mime == null
                    || (!mime.equals("text/html") && !mime.equals("application/xhtml+xml"))) {
                return null;
            }
            Log.d(TAG, "PRE-PROCESSED HTML: " + uriPath);

            return mScriptProcessor.injectEpubHtml(data);
        }
    };

    private static final ReadiumService INSTANCE = new ReadiumService();

    public static ReadiumService getInstance() {
        return INSTANCE;
    }

    public static ReadiumJSApi getApi() {
        return getInstance().mJSApi;
    }

    public void start(WebViewDelegate delegate,
                      PageEventListener pageEventListener,
                      Package pckg) {
        mWebViewDelegate = delegate;
        mPackage = pckg;
        mEventListener = pageEventListener;

        mScriptProcessor = new ScriptProcessor(mWebViewDelegate, mPackage);

        mJSApi = new ReadiumJSApi(new ReadiumJSApi.JSLoader() {
            @Override
            public void loadJS(String javascript) {
                mWebViewDelegate.loadUrl(javascript);
            }
        });

        mWebViewDelegate.loadUrl(READER_SKELETON);
        mWebViewDelegate.addJavascriptInterface(new JsInterface(), "LauncherUI");

        mServer = new WebServer(WebServer.HTTP_HOST,
                WebServer.HTTP_PORT,
                mPackage,
                dataPreProcessor);

        mServer.startServer();
    }

    public void stop() {
        //delegate.loadUrl(READER_SKELETON);
        mServer.stop();
    }

    public WebResourceResponse handleWebRequest(String url) {
        return mScriptProcessor.interceptRequest(url);
    }

    public class JsInterface {

        @JavascriptInterface
        public void onPaginationChanged(String currentPagesInfo) {
            try {
                PaginationInfo paginationInfo = PaginationInfo.fromJson(currentPagesInfo);
                mEventListener.onPaginationChanged(paginationInfo);
            } catch (JSONException e) {
                Log.e(TAG, "" + e.getMessage(), e);
            }
        }

        @JavascriptInterface
        public void onSettingsApplied() {
            mEventListener.onSettingsApplied();
        }

        @JavascriptInterface
        public void onReaderInitialized() {
            mEventListener.onReaderInitialized();
        }

        @JavascriptInterface
        public void onContentLoaded() {
            mEventListener.onContentLoaded();
        }

        @JavascriptInterface
        public void onPageLoaded() {
            mEventListener.onPageLoaded();
        }

        @JavascriptInterface
        public void onIsMediaOverlayAvailable(String available) {
            mEventListener.onIsMediaOverlayAvailable(available);
        }

        @JavascriptInterface
        public void onMediaOverlayStatusChanged(String status) {
            mEventListener.onMediaOverlayStatusChanged(status);
        }

        @JavascriptInterface
        public void onMediaOverlayTTSSpeak() {
            mEventListener.onMediaOverlayTTSSpeak();
        }

        @JavascriptInterface
        public void onMediaOverlayTTSStop() {
            mEventListener.onMediaOverlayTTSStop();
        }

        @JavascriptInterface
        public void getBookmarkData(final String bookmarkData) {
            mEventListener.getBookmarkData(bookmarkData);
        }
    }
}
