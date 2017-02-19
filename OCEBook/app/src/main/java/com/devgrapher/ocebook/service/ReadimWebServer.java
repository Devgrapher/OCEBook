package com.devgrapher.ocebook.service;

import android.util.Log;

import com.devgrapher.ocebook.readium.ScriptProcessor;

import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;

/**
 * Created by Brent on 2/19/17.
 */

public class ReadimWebServer {
    private static final String TAG = ReadimWebServer.class.toString();
    private ScriptProcessor.ScriptInjector mScriptInjector = new ScriptProcessor.ScriptInjector();
    private WebServer mWebServer;

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

                    return mScriptInjector.injectEpubHtml(data);
                }
            };

    public void start(Package pckg) {
        mWebServer = new WebServer(WebServer.HTTP_HOST, WebServer.HTTP_PORT, pckg, dataPreProcessor);
        mWebServer.startServer();
    }

    public void stop() {
        mWebServer.stop();
    }
}
