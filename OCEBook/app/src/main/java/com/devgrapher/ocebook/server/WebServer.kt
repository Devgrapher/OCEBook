/*
 * EpubServer.java
 * SDKLauncher-Android
 *
 * Created by Yonathan Teitelbaum (Mantano) on 2013-09-03.
 */
 //  Copyright (c) 2014 Readium Foundation and/or its licensees. All rights reserved.
 //  Redistribution and use in source and binary forms, with or without modification,
 //  are permitted provided that the following conditions are met:
 //  1. Redistributions of source code must retain the above copyright notice, this
 //  list of conditions and the following disclaimer.
 //  2. Redistributions in binary form must reproduce the above copyright notice,
 //  this list of conditions and the following disclaimer in the documentation and/or
 //  other materials provided with the distribution.
 //  3. Neither the name of the organization nor the names of its contributors may be
 //  used to endorse or promote products derived from this software without specific
 //  prior written permission.
 //
 //  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 //  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 //  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 //  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 //  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 //  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 //  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 //  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 //  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 //  OF THE POSSIBILITY OF SUCH DAMAGE

package com.devgrapher.ocebook.server

import android.util.Log

import com.devgrapher.ocebook.util.ByteRangeInputStream
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback

import org.readium.sdk.android.ManifestItem
import org.readium.sdk.android.Package
import org.readium.sdk.android.PackageResource
import org.readium.sdk.android.util.ResourceInputStream

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone

/**
   * This small web server will serve media files such as audio and video.
  */
class WebServer(internal var mHostName:String,
                 internal var mPortNumber:Int,
                 private val dataPreProcessor:WebServer.DataPreProcessor)
    : HttpServerRequestCallback {

    interface DataPreProcessor {
        fun handle(data: ByteArray, mime: String, uriPath: String, item: ManifestItem): ByteArray?
    }

    private val mHTTPDateFormat:SimpleDateFormat

    internal var pckg:Package? = null
        private set

    internal var mHttpServer:AsyncHttpServer
    internal var mAsyncServer:AsyncServer

    init{
        this.mHttpServer = AsyncHttpServer()
        this.mAsyncServer = AsyncServer()
        this.mHTTPDateFormat = SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        this.mHTTPDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))

        mHttpServer.get(".*", this)
    }

    fun startServer(pckg:Package) {
        this.pckg = pckg

        try
        {
            mAsyncServer.listen(InetAddress.getByName(mHostName), mPortNumber,
            mHttpServer.getListenCallback())
        }
        catch (e:UnknownHostException) {
            Log.e(TAG, "" + e.message)
        }
    }

    fun stop() {
        mHttpServer.stop()
        mAsyncServer.stop()
        pckg = null
    }

    private val criticalSectionSynchronizedLock = Any()

    override fun onRequest(request:AsyncHttpServerRequest,
                                  response:AsyncHttpServerResponse) {

        var uri = request.getPath()

        Log.d(TAG, request.getMethod() + " '" + uri + "' ")

        var e = request.getHeaders().getMultiMap().keys.iterator()
        while (e.hasNext())
        {
            val value = e.next()
            Log.d(TAG, "  HDR: '" + value + "' = '" + request.getHeaders().get(value) + "'")
        }
        e = request.getQuery().keys.iterator()
        while (e.hasNext())
        {
            val value = e.next()
            Log.d(TAG, "  PRM: '" + value + "' = '" + request.getQuery().get(value) + "'")
        }

        val httpPrefix = httpPrefix
        val iHttpPrefix = uri.indexOf(httpPrefix)
        uri = if (iHttpPrefix == 0) uri.substring(httpPrefix.length) else uri
        uri = if (uri.startsWith("/")) uri.substring(1) else uri

        val indexOfQ = uri.indexOf('?')
        if (indexOfQ >= 0)
        {
            uri = uri.substring(0, indexOfQ)
        }

        val indexOfSharp = uri.indexOf('#')
        if (indexOfSharp >= 0)
        {
            uri = uri.substring(0, indexOfSharp)
        }

        if (pckg == null)
        {
            response.code(503)
            response.send("Error 503, package not found")
            return
        }

        var contentLength = -1
        synchronized (criticalSectionSynchronizedLock) {
            contentLength = pckg!!.getArchiveInfoSize(uri)
        }

        if (contentLength <= 0)
        {
            response.code(404)
            response.send("Error 404, file not found.")
            return
        }

        var mime:String? = null
        val dot = uri.lastIndexOf('.')
        if (dot >= 0)
        {
            mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase())
        }
        if (mime == null)
        {
            mime = "application/octet-stream"
        }

        val item = pckg!!.getManifestItem(uri)
        val contentType = if (item != null) item.mediaType else null
        if (mime != "application/xhtml+xml"
                && mime != "application/xml" // FORCE
                && contentType != null && contentType.isNotEmpty())
        {
            mime = contentType
        }

        val packageResource = pckg!!.getResourceAtRelativePath(uri)

        val isHTML = mime == "text/html" || mime == "application/xhtml+xml"

        val now = Calendar.getInstance()
        val expires = now.clone() as Calendar
        expires.add(Calendar.DAY_OF_MONTH, 10)
        response.getHeaders().add("Cache-control", "no-transform,public,max-age=3000,s-maxage=9000")
        response.getHeaders().add("Last-Modified", this.mHTTPDateFormat.format(now.getTime()))
        response.getHeaders().add("Expires", this.mHTTPDateFormat.format(expires.getTime()))

        if (isHTML)
        {
         //Pre-process HTML data as a whole
            var data:ByteArray = packageResource.readDataFull()

            val data_ = dataPreProcessor.handle(data, mime, uri, item)
            if (data_ != null)
            {
                data = data_
            }

            response.setContentType(mime)
            response.sendStream(ByteArrayInputStream(data), data.size.toLong())
        } else {
            val isRange = request.getHeaders().get("range") != null

            var `is`:ResourceInputStream? = null
            synchronized (criticalSectionSynchronizedLock) {
                Log.d(TAG, "NEW STREAM:" + request.getPath())
                `is` = packageResource.getInputStream(isRange) as ResourceInputStream?

                val updatedContentLength = packageResource.contentLength
                if (updatedContentLength != contentLength)
                {
                    Log.e(TAG, "UPDATED CONTENT LENGTH! " + updatedContentLength +
                            "<--" + contentLength)
                }
            }

            val bis = ByteRangeInputStream(`is`!!, isRange, criticalSectionSynchronizedLock)

            try
            {
                response.sendStream(bis, bis.available().toLong())
            }
            catch (ex:IOException) {
                response.code(500)
                response.end()
                Log.e(TAG, ex.toString())
            }
        }
    }

    companion object {
        private val TAG = "WebServer"
        val HTTP_HOST = "127.0.0.1"
        val HTTP_PORT = 33380
        /**
           * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
          */
        val MIME_TYPES:Map<String, String>

        init{
            val tmpMap = HashMap<String, String>()
            tmpMap.put("html", "application/xhtml+xml") // FORCE
            tmpMap.put("xhtml", "application/xhtml+xml") // FORCE
            tmpMap.put("xml", "application/xml") // FORCE
            tmpMap.put("htm", "text/html")
            tmpMap.put("css", "text/css")
            tmpMap.put("java", "text/x-java-source, text/java")
            tmpMap.put("txt", "text/plain")
            tmpMap.put("asc", "text/plain")
            tmpMap.put("gif", "image/gif")
            tmpMap.put("jpg", "image/jpeg")
            tmpMap.put("jpeg", "image/jpeg")
            tmpMap.put("png", "image/png")
            tmpMap.put("mp3", "audio/mpeg")
            tmpMap.put("m3u", "audio/mpeg-url")
            tmpMap.put("mp4", "video/mp4") // could be audio!
            tmpMap.put("ogv", "video/ogg")
            tmpMap.put("flv", "video/x-flv")
            tmpMap.put("mov", "video/quicktime")
            tmpMap.put("swf", "application/x-shockwave-flash")
            tmpMap.put("js", "application/javascript")
            tmpMap.put("pdf", "application/pdf")
            tmpMap.put("doc", "application/msword")
            tmpMap.put("ogg", "application/x-ogg")
            tmpMap.put("zip", "application/octet-stream")
            tmpMap.put("exe", "application/octet-stream")
            tmpMap.put("class", "application/octet-stream")
            tmpMap.put("webm", "video/webm")
            MIME_TYPES = Collections.unmodifiableMap<String, String>(tmpMap)
        }

        val httpPrefix:String

        get() {
            return "http://" + HTTP_HOST + ":" + HTTP_PORT
        }
    }
}
