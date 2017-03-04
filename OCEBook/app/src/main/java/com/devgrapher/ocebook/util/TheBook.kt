package com.devgrapher.ocebook.util

import android.content.Context

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object TheBook {

    val DEFAULT_BOOK_ASSET_PATH = "the_book.epub"
    private val CACHE_PATH = "theBook.epub"

    @Throws(IOException::class)
    private fun makeCache(context: Context, assetPath: String, cachePath: File): File {

        var input: InputStream? = null
        var fos: FileOutputStream? = null

        try {
            input = context.assets.open(assetPath)
            val size = input!!.available()
            val buffer = ByteArray(size)
            input.read(buffer)

            fos = FileOutputStream(cachePath)
            fos.write(buffer)
        } finally {
            if (input != null)
                input.close()
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
