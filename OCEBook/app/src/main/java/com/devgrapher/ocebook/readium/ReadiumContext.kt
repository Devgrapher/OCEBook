package com.devgrapher.ocebook.readium

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceResponse

import com.devgrapher.ocebook.model.PaginationInfo
import com.devgrapher.ocebook.model.ReadiumJSApi
import com.devgrapher.ocebook.server.ReadimWebServer

import org.json.JSONException
import org.readium.sdk.android.Container
import org.readium.sdk.android.Package

import java.io.InputStream

/**
 * Created by Brent on 2/17/17.

 * Hold the context of package opened.
 */

class ReadiumContext(private val mWebViewDelegate: ReadiumContext.WebViewDelegate,
                     private val mEventListener: ReadiumContext.PageEventListener,
                     private val mContainer: Container) {

    private val mScriptProcessor: ScriptProcessor
    val pckg: Package
    val api: ReadiumJSApi

    val id: Long
        get() = System.identityHashCode(this).toLong()

    interface WebViewDelegate {
        fun evaluateJavascript(script: String)
        fun openAsset(fileName: String): InputStream?
        fun addJavascriptInterface(jsInterface: JsInterface, name: String)
        fun loadUrl(url: String)
    }

    interface PageEventListener {
        fun onPaginationChanged(currentPagesInfo: PaginationInfo)
        fun onSettingsApplied()
        fun onReaderInitialized()
        fun onContentLoaded()
        fun onPageLoaded()
        fun onIsMediaOverlayAvailable(available: String)
        fun onMediaOverlayStatusChanged(status: String)
        fun onMediaOverlayTTSSpeak()
        fun onMediaOverlayTTSStop()
        fun getBookmarkData(bookmarkData: String)
    }

    init {
        pckg = mContainer.defaultPackage!!

        mScriptProcessor = ScriptProcessor(mWebViewDelegate, pckg)

        api = ReadiumJSApi(object: ReadiumJSApi.JSLoader {
            override fun loadJS(javascript: String) =
                    mWebViewDelegate.loadUrl(javascript)
        })

        mWebViewDelegate.loadUrl(READER_SKELETON)
        mWebViewDelegate.addJavascriptInterface(JsInterface(), "LauncherUI")

        sWebServer.start(pckg)
    }

    fun dispose() {
        sWebServer.stop()
    }

    fun handleWebRequest(url: String): WebResourceResponse? {
        return mScriptProcessor.interceptRequest(url)
    }

    inner class JsInterface {

        @JavascriptInterface
        fun onPaginationChanged(currentPagesInfo: String) {
            try {
                val paginationInfo = PaginationInfo.fromJson(currentPagesInfo)
                mEventListener.onPaginationChanged(paginationInfo)
            } catch (e: JSONException) {
                Log.e(TAG, "" + e.message, e)
            }

        }

        @JavascriptInterface
        fun onSettingsApplied() {
            mEventListener.onSettingsApplied()
        }

        @JavascriptInterface
        fun onReaderInitialized() {
            mEventListener.onReaderInitialized()
        }

        @JavascriptInterface
        fun onContentLoaded() {
            mEventListener.onContentLoaded()
        }

        @JavascriptInterface
        fun onPageLoaded() {
            mEventListener.onPageLoaded()
        }

        @JavascriptInterface
        fun onIsMediaOverlayAvailable(available: String) {
            mEventListener.onIsMediaOverlayAvailable(available)
        }

        @JavascriptInterface
        fun onMediaOverlayStatusChanged(status: String) {
            mEventListener.onMediaOverlayStatusChanged(status)
        }

        @JavascriptInterface
        fun onMediaOverlayTTSSpeak() {
            mEventListener.onMediaOverlayTTSSpeak()
        }

        @JavascriptInterface
        fun onMediaOverlayTTSStop() {
            mEventListener.onMediaOverlayTTSStop()
        }

        @JavascriptInterface
        fun getBookmarkData(bookmarkData: String) {
            mEventListener.getBookmarkData(bookmarkData)
        }
    }

    companion object {
        private val TAG = ReadiumContext::class.java.toString()
        private val READER_SKELETON = "file:///android_asset/readium-shared-js/reader.html"

        private val sWebServer = ReadimWebServer()
    }
}
