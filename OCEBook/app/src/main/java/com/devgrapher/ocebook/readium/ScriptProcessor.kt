package com.devgrapher.ocebook.readium

import android.util.Log
import android.webkit.WebResourceResponse

import com.devgrapher.ocebook.server.WebServer
import com.devgrapher.ocebook.util.HTMLUtil
import com.devgrapher.ocebook.util.Laz
import com.koushikdutta.async.util.Charsets.UTF_8

import org.readium.sdk.android.Package

import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
   * Created by Brent on 2/17/17.
  */

class ScriptProcessor(private val mDelegate:ReadiumContext.WebViewDelegate, private val mPackage:Package) {
    private val TAG = ScriptProcessor::class.java.toString()
    private val UTF_8 = "utf-8"

    fun interceptRequest(url:String?):WebResourceResponse? {
         if (url == null || url == "undefined") {
             Log.e(TAG, "NULL URL RESPONSE: " + url!!)
             return WebResourceResponse(null, UTF_8, ByteArrayInputStream("".toByteArray()))
         }

        val localHttpUrlPrefix = "http://" + WebServer.HTTP_HOST + ":" + WebServer.HTTP_PORT
        val isLocalHttp = url.startsWith(localHttpUrlPrefix)

        if (url.startsWith("http") && !isLocalHttp)
        {
            Laz.y({ Log.d(TAG, "HTTP (NOT LOCAL): " + url) })
            return null
        }

        val cleanedUrl = cleanResourceUrl(url, false)
        Laz.y({ Log.d(TAG, url + " => " + cleanedUrl) })

        if (cleanedUrl.matches(("\\/?\\d*\\/readium_epubReadingSystem_inject.js").toRegex()))
        {
            Log.d(TAG, "navigator.epubReadingSystem inject ...")

            // Fake script requested, this is immediately invoked after
            // epubReadingSystem hook is in place,
            // => execute js on the reader.html context to push the
            // global window.navigator.epubReadingSystem into the
            // iframe(s)

            mDelegate.evaluateJavascript(Injections.INJECT_EPUB_RSO_SCRIPT_2)

            return WebResourceResponse("text/javascript", UTF_8,
                    ByteArrayInputStream("(function(){})()".toByteArray()))
        }

        if (cleanedUrl.matches(("\\/?readium_MathJax.js").toRegex()))
        {
            Laz.yLog(TAG, "MathJax.js inject ...")

            val input = mDelegate.openAsset(Injections.PAYLOAD_MATHJAX_ASSET)
            if (input == null)
            {
                return WebResourceResponse(null, UTF_8, ByteArrayInputStream("".toByteArray()))
            }

            return WebResourceResponse("text/javascript", UTF_8, input)
        }

        if (cleanedUrl.matches(("\\/?readium_Annotations.css").toRegex()))
        {
            Laz.yLog(TAG, "annotations.css inject ...")

            val input = mDelegate.openAsset(Injections.PAYLOAD_ANNOTATIONS_CSS_ASSET)
            if (input == null)
            {
                return WebResourceResponse(null, UTF_8, ByteArrayInputStream("".toByteArray()))
            }

            return WebResourceResponse("text/css", UTF_8, input)
        }

        var mime:String? = null
        val dot = cleanedUrl.lastIndexOf('.')
        if (dot >= 0)
        {
            mime = WebServer.MIME_TYPES.get(cleanedUrl.substring(dot + 1).toLowerCase())
        }
        if (mime == null)
        {
            mime = "application/octet-stream"
        }

        val item = mPackage.getManifestItem(cleanedUrl)
        val contentType = if (item != null) item.mediaType else null
        if (mime != "application/xhtml+xml"
            && mime != "application/xml" // FORCE
            && contentType != null && contentType.isNotEmpty())
        {
            mime = contentType
        }

        if (url.startsWith("file:"))
        {
            if (item == null)
            {
            Laz.yLog(TAG, "NO MANIFEST ITEM ... " + url)
            return null
            }

            val cleanedUrlWithQueryFragment = cleanResourceUrl(url, true)
            val httpUrl = "http://" + WebServer.HTTP_HOST + ":" +
                    WebServer.HTTP_PORT.toString() + "/" +
                    cleanedUrlWithQueryFragment
            Log.e(TAG, "FILE to HTTP REDIRECT: " + httpUrl)

            try
            {
                val c = URL(httpUrl).openConnection()
                (c as HttpURLConnection).setUseCaches(false)
                if (mime == "application/xhtml+xml" || mime == "text/html")
                {
                    (c as HttpURLConnection).setRequestProperty("Accept-Ranges", "none")
                }
                val input = c.getInputStream()
                return WebResourceResponse(mime, null, input)
            }
            catch (ex:Exception) {
                    Log.e(TAG, "FAIL: " + httpUrl + " -- " + ex.message, ex)
            }

        }
        Laz.yLog(TAG, "RESOURCE FETCH ... " + url)
        return null
    }

    private fun cleanResourceUrl(url:String, preserveQueryFragment:Boolean):String {

        var cleanUrl:String? = null

        val httpUrl = WebServer.httpPrefix
        if (url.startsWith(httpUrl))
        {
            cleanUrl = url.replaceFirst((httpUrl).toRegex(), "")
        }
        else
        {
            cleanUrl = if ((url.startsWith(Injections.ASSET_PREFIX)))
                url.replaceFirst((Injections.ASSET_PREFIX).toRegex(), "")
                else url.replaceFirst(("file://").toRegex(), "")
        }

        var basePath = mPackage.basePath
        if (basePath!!.get(0) != '/')
        {
            basePath = "/" + basePath
        }
        if (cleanUrl.get(0) != '/')
        {
            cleanUrl = '/' + cleanUrl
        }
        cleanUrl = if ((cleanUrl.startsWith(basePath))) {
            cleanUrl.replaceFirst((basePath).toRegex(), "")
        } else cleanUrl

        if (cleanUrl.get(0) == '/')
        {
            cleanUrl = cleanUrl.substring(1)
        }

        if (!preserveQueryFragment)
        {
            val indexOfQ = cleanUrl.indexOf('?')
            if (indexOfQ >= 0)
            {
                cleanUrl = cleanUrl.substring(0, indexOfQ)
            }

            val indexOfSharp = cleanUrl.indexOf('#')
            if (indexOfSharp >= 0)
            {
                cleanUrl = cleanUrl.substring(0, indexOfSharp)
            }
        }

        return cleanUrl
    }

    class ScriptInjector {
        private var mEpubRsoInjectCounter:Int = 0

        fun injectEpubHtml(data:ByteArray):ByteArray {
            val htmlText = String(data, UTF_8)

            var newHtml = htmlText

             // Set up the script tags to add to the head
                        var tagsToInjectToHead = Injections.INJECT_HEAD_EPUB_RSO_1 +
             // Slightly change fake script src url with an
                                // increasing count to prevent caching of the
                                // request
                                String.format(Injections.INJECT_HEAD_EPUB_RSO_2,
            ++mEpubRsoInjectCounter)
             // Checks for the existance of MathML => request
                // MathJax payload
            if (newHtml.contains("<math") || newHtml.contains("<m:math"))
            {
                tagsToInjectToHead += Injections.INJECT_HEAD_MATHJAX
            }

            newHtml = HTMLUtil.htmlByInjectingIntoHead(newHtml, tagsToInjectToHead)

            return newHtml.toByteArray()
        }
    }
}
