package com.devgrapher.ocebook.model

import android.util.Log

import org.json.JSONException
import org.json.JSONObject
import org.readium.sdk.android.Package

class ReadiumJSApi(private val mJSLoader: ReadiumJSApi.JSLoader) {

    interface JSLoader {
        fun loadJS(javascript: String)
    }

    fun bookmarkCurrentPage() {
        loadJS("window.LauncherUI.getBookmarkData(ReadiumSDK.reader.bookmarkCurrentPage());")
    }

    fun openPageLeft() {
        loadJS("ReadiumSDK.reader.openPageLeft();")
    }

    fun openPageRight() {
        loadJS("ReadiumSDK.reader.openPageRight();")
    }

    fun openBook(pckg: Package, viewerSettings: ViewerSettings,
                 openPageRequestData: OpenPageRequest) {
        val openBookData = JSONObject()
        try {
            openBookData.put("package", pckg.toJSON())
            openBookData.put("settings", viewerSettings.toJSON())
            openBookData.put("openPageRequest", openPageRequestData.toJSON())
        } catch (e: JSONException) {
            Log.e(TAG, "" + e.message, e)
        }

        loadJSOnReady("ReadiumSDK.reader.openBook(" + openBookData.toString() + ");")
    }

    fun updateSettings(viewerSettings: ViewerSettings) {
        try {
            loadJSOnReady("ReadiumSDK.reader.updateSettings(" + viewerSettings.toJSON().toString() + ");")
        } catch (e: JSONException) {
            Log.e(TAG, "" + e.message, e)
        }

    }

    fun openContentUrl(href: String, baseUrl: String) {
        loadJSOnReady("ReadiumSDK.reader.openContentUrl(\"$href\", \"$baseUrl\");")
    }

    fun openSpineItemPage(idRef: String, page: Int) {
        loadJSOnReady("ReadiumSDK.reader.openSpineItemPage(\"$idRef\", $page);")
    }

    fun openSpineItemElementCfi(idRef: String, elementCfi: String) {
        loadJSOnReady("ReadiumSDK.reader.openSpineItemElementCfi(\"$idRef\",\"$elementCfi\");")
    }

    fun nextMediaOverlay() {
        loadJSOnReady("ReadiumSDK.reader.nextMediaOverlay();")
    }

    fun previousMediaOverlay() {
        loadJSOnReady("ReadiumSDK.reader.previousMediaOverlay();")
    }

    fun toggleMediaOverlay() {
        loadJSOnReady("ReadiumSDK.reader.toggleMediaOverlay();")
    }


    private fun loadJSOnReady(jScript: String) {
        loadJS("$(document).ready(function () {$jScript});")
    }

    private fun loadJS(jScript: String) {
        //Log.i(TAG, "loadJS: "+jScript);
        mJSLoader.loadJS("javascript:(function(){$jScript})()")
    }

    companion object {

        private val TAG = "ReadiumJSApi"
    }
}
