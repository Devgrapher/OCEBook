package com.devgrapher.ocebook.readium;

import android.util.Log;
import android.webkit.WebResourceResponse;

import com.devgrapher.ocebook.server.WebServer;
import com.devgrapher.ocebook.util.HTMLUtil;

import org.readium.sdk.android.ManifestItem;
import org.readium.sdk.android.Package;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * Created by Brent on 2/17/17.
 */

public class ScriptProcessor {
    private static final String TAG = ScriptProcessor.class.toString();
    private static final String UTF_8 = "utf-8";

    private ReadiumContext.WebViewDelegate mDelegate;
    private int mEpubRsoInjectCounter;
    private Package mPackage;


    public ScriptProcessor(ReadiumContext.WebViewDelegate delegate, Package pckg) {
        mDelegate = delegate;
        mPackage = pckg;
    }

    public WebResourceResponse interceptRequest(String url) {
        if (url == null && url.equals("undefined")) {
            Log.e(TAG, "NULL URL RESPONSE: " + url);
            return new WebResourceResponse(null, UTF_8,
                    new ByteArrayInputStream("".getBytes()));
        }

        String localHttpUrlPrefix = "http://" + WebServer.HTTP_HOST
                + ":" + WebServer.HTTP_PORT;
        boolean isLocalHttp =  url.startsWith(localHttpUrlPrefix);

        if (url.startsWith("http") && !isLocalHttp) {
            Log.d(TAG, "HTTP (NOT LOCAL): " + url);
            return null;
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

            mDelegate.evaluateJavascript(Constants.INJECT_EPUB_RSO_SCRIPT_2);

            return new WebResourceResponse("text/javascript", UTF_8,
                    new ByteArrayInputStream(
                            "(function(){})()".getBytes()));
        }

        if (cleanedUrl.matches("\\/?readium_MathJax.js")) {
            Log.d(TAG, "MathJax.js inject ...");

            InputStream is = mDelegate.openAsset(Constants.PAYLOAD_MATHJAX_ASSET);
            if (is == null) {
                return new WebResourceResponse(null, UTF_8,
                        new ByteArrayInputStream("".getBytes()));
            }

            return new WebResourceResponse("text/javascript", UTF_8, is);
        }

        if (cleanedUrl.matches("\\/?readium_Annotations.css")) {
            Log.d(TAG, "annotations.css inject ...");

            InputStream is = mDelegate.openAsset(Constants.PAYLOAD_ANNOTATIONS_CSS_ASSET);
            if (is == null) {
                return new WebResourceResponse(null, UTF_8,
                        new ByteArrayInputStream("".getBytes()));
            }

            return new WebResourceResponse("text/css", UTF_8, is);
        }

        String mime = null;
        int dot = cleanedUrl.lastIndexOf('.');
        if (dot >= 0) {
            mime = WebServer.MIME_TYPES.get(cleanedUrl.substring(
                    dot + 1).toLowerCase());
        }
        if (mime == null) {
            mime = "application/octet-stream";
        }

        ManifestItem item = mPackage.getManifestItem(cleanedUrl);
        String contentType = item != null ? item.getMediaType() : null;
        if (!mime.equals("application/xhtml+xml")
                && !mime.equals("application/xml") // FORCE
                && contentType != null && contentType.length() > 0) {
            mime = contentType;
        }

        if (url.startsWith("file:")) {
            if (item == null) {
                Log.e(TAG, "NO MANIFEST ITEM ... " + url);
                return null;
            }

            String cleanedUrlWithQueryFragment = cleanResourceUrl(url, true);
            String httpUrl = "http://" + WebServer.HTTP_HOST + ":"
                    + WebServer.HTTP_PORT + "/"
                    + cleanedUrlWithQueryFragment;
            Log.e(TAG, "FILE to HTTP REDIRECT: " + httpUrl);

            try {
                URLConnection c = new URL(httpUrl).openConnection();
                ((HttpURLConnection) c).setUseCaches(false);
                if (mime.equals("application/xhtml+xml")
                        || mime.equals("text/html")) {
                    ((HttpURLConnection) c).setRequestProperty(
                            "Accept-Ranges", "none");
                }
                InputStream is = c.getInputStream();
                return new WebResourceResponse(mime, null, is);
            } catch (Exception ex) {
                Log.e(TAG, "FAIL: " + httpUrl + " -- " + ex.getMessage(), ex);
            }
        }
        Log.d(TAG, "RESOURCE FETCH ... " + url);
        return null;
    }

    private String cleanResourceUrl(String url, boolean preserveQueryFragment) {

        String cleanUrl = null;

        String httpUrl = WebServer.getHttpPrefix();
        if (url.startsWith(httpUrl)) {
            cleanUrl = url.replaceFirst(httpUrl, "");
        } else {
            cleanUrl = (url.startsWith(Constants.ASSET_PREFIX)) ? url.replaceFirst(
                    Constants.ASSET_PREFIX, "") : url.replaceFirst("file://", "");
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

    public static class ScriptInjector {
        private int mEpubRsoInjectCounter;

        public byte[] injectEpubHtml(byte[] data) {
            String htmlText = new String(data, Charset.forName(UTF_8));

            String newHtml = htmlText;

            // Set up the script tags to add to the head
            String tagsToInjectToHead = Constants.INJECT_HEAD_EPUB_RSO_1
                    // Slightly change fake script src url with an
                    // increasing count to prevent caching of the
                    // request
                    + String.format(Constants.INJECT_HEAD_EPUB_RSO_2,
                    ++mEpubRsoInjectCounter);
            // Checks for the existance of MathML => request
            // MathJax payload
            if (newHtml.contains("<math") || newHtml.contains("<m:math")) {
                tagsToInjectToHead += Constants.INJECT_HEAD_MATHJAX;
            }

            newHtml = HTMLUtil.htmlByInjectingIntoHead(newHtml,
                    tagsToInjectToHead);

            return newHtml.getBytes();
        }
    }
}
