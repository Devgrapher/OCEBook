package com.devgrapher.ocebook.server

import android.util.Log

import com.devgrapher.ocebook.readium.ScriptProcessor
import com.devgrapher.ocebook.util.Laz

import org.readium.sdk.android.ManifestItem
import org.readium.sdk.android.Package

/**
 * Created by Brent on 2/19/17.
 */

class ReadimWebServer {
    private val mScriptInjector = ScriptProcessor.ScriptInjector()

    private val dataPreProcessor = object: WebServer.DataPreProcessor {
        override fun handle(data: ByteArray, mime: String, uriPath: String, item: ManifestItem): ByteArray? {
            if (mime != "text/html" && mime != "application/xhtml+xml")
                return null

            Laz.y { Log.d(TAG, "PRE-PROCESSED HTML: " + uriPath) }
            return mScriptInjector.injectEpubHtml(data)
        }
    }

    private val mWebServer = WebServer(WebServer.HTTP_HOST, WebServer.HTTP_PORT, dataPreProcessor)

    fun start(pckg: Package) {
        if (mWebServer.pckg === pckg)
            return

        mWebServer.startServer(pckg)
        Laz.yLog(TAG, "Start Server")
    }

    fun stop() {
        mWebServer.stop()
        Laz.yLog(TAG, "Stop Server")
    }

    companion object {
        private val TAG = ReadimWebServer::class.java.toString()
    }
}
