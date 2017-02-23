package com.devgrapher.ocebook.readium;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;

import com.devgrapher.ocebook.model.PaginationInfo;
import com.devgrapher.ocebook.model.ReadiumJSApi;
import com.devgrapher.ocebook.server.ReadimWebServer;

import org.json.JSONException;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;

import java.io.InputStream;

/**
 * Created by Brent on 2/17/17.
 *
 * Hold the context of package opened.
 */

public class ReadiumContext {
    private static final String TAG = ReadiumContext.class.toString();
    private static final String READER_SKELETON = "file:///android_asset/readium-shared-js/reader.html";

    private static ReadimWebServer sWebServer = new ReadimWebServer();

    private final ScriptProcessor mScriptProcessor;
    private final Container mContainer;
    private final Package mPackage;
    private final ReadiumJSApi mJSApi;

    private final WebViewDelegate mWebViewDelegate;
    private final PageEventListener mEventListener;

    public ReadiumJSApi getApi() {
        return mJSApi;
    }

    public Package getPackage() {
        return mPackage;
    }

    public long getId() {
        return System.identityHashCode(this);
    }

    public interface WebViewDelegate {
        void evaluateJavascript(final String script);
        InputStream openAsset(String fileName);
        void addJavascriptInterface(JsInterface jsInterface, String name);
        void loadUrl(String url);
    }

    public interface PageEventListener {
        void onPaginationChanged(PaginationInfo currentPagesInfo);
        void onSettingsApplied();
        void onReaderInitialized();
        void onContentLoaded();
        void onPageLoaded();
        void onIsMediaOverlayAvailable(String available);
        void onMediaOverlayStatusChanged(String status);
        void onMediaOverlayTTSSpeak();
        void onMediaOverlayTTSStop();
        void getBookmarkData(final String bookmarkData);
    }

    public ReadiumContext(WebViewDelegate delegate,
                          PageEventListener pageEventListener,
                          Container container) {
        mWebViewDelegate = delegate;
        mContainer = container;
        mPackage = mContainer.getDefaultPackage();
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

        sWebServer.start(mPackage);
    }

    public void dispose() {
        sWebServer.stop();
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
