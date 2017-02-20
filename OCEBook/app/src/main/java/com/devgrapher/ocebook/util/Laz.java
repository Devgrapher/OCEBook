package com.devgrapher.ocebook.util;

import android.util.Log;

import com.devgrapher.ocebook.BuildConfig;

/**
 * Logging class
 */
public class Laz {
    /**
     * Delays building up a log string by taking the code with lambda.
     * @param logging Logging code fragment that needs some processing to build up log string.
     */
    public static void y(Runnable logging) {
        if (!BuildConfig.DEBUG)
            return;
        logging.run();
    }

    /**
     * Convenience replacement of Log.d
     * @param tag
     * @param msg
     */
    public static void yLog(String tag, String msg) {
        if (!BuildConfig.DEBUG)
            return;
        Log.d(tag, msg);
    }
}
