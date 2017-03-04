package com.devgrapher.ocebook.util

import android.content.Context

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object TheBook {

    private val CACHE_PATH = "theBook.epub"

    @Throws(IOException::class)
    private fun makeCache(context: Context, assetPath: String, cachePath: File): File {

        var `is`: InputStream? = null
        var fos: FileOutputStream? = null

        try {
            `is` = context.assets.open(assetPath)
            val size = `is`!!.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)

            fos = FileOutputStream(cachePath)
            fos.write(buffer)
        } finally {
            if (`is` != null)
                `is`.close()
            if (fos != null)
                fos.close()
        }
        return cachePath
    }

    @Throws(IOException::class)
    fun makeCacheIfNecessary(context: Context, assetFilePath: String): File {

        val cache = File(context.filesDir, CACHE_PATH)

        if (cache.exists()) {
            return cache
        } else {
            return makeCache(context, assetFilePath, cache)
        }
    }

    @Throws(IOException::class)
    fun makeCacheForce(context: Context, assetFilePath: String): File {

        val cache = File(context.filesDir, CACHE_PATH)
        cache.delete()

        return makeCache(context, assetFilePath, cache)
    }
}
