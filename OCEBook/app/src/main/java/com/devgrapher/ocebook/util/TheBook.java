package com.devgrapher.ocebook.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TheBook {

    private static final String CACHE_PATH = "theBook.epub";

    private static File makeCache(Context context, String assetPath, File cachePath)
            throws IOException {

        InputStream is = null;
        FileOutputStream fos = null;

        try {
            is = context.getAssets().open(assetPath);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);

            fos = new FileOutputStream(cachePath);
            fos.write(buffer);
        } finally {
            if (is != null)
                is.close();
            if (fos != null)
                fos.close();
        }
        return cachePath;
    }

    public static File makeCacheIfNecessary(Context context, String assetFilePath)
            throws IOException {

        File cache = new File(context.getFilesDir(), CACHE_PATH);

        if (cache.exists()) {
            return cache;
        } else {
            return makeCache(context, assetFilePath, cache);
        }
    }

    public static File makeCacheForce(Context context, String assetFilePath)
            throws IOException {

        File cache = new File(context.getFilesDir(), CACHE_PATH);
        cache.delete();

        return  makeCache(context, assetFilePath, cache);
    }
}
