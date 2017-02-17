package com.devgrapher.ocebook.readium;

import android.util.Log;

import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;

/**
 * Created by Brent on 2/17/17.
 */

public class ReadiumServer {
    private static final String TAG = ReadiumServer.class.toString();
    private WebServer mServer;
    private ScriptProcessor mScriptProcessor;
    private Package mPackage;

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

    public ReadiumServer(ScriptProcessor processor, Package pckg) {
        mScriptProcessor = processor;
        mPackage = pckg;
        mServer = new WebServer(WebServer.HTTP_HOST, WebServer.HTTP_PORT,
                mPackage, dataPreProcessor);
    }

    public void start() {
        mServer.startServer();
    }

    public void stop() {
        mServer.stop();
    }
}
