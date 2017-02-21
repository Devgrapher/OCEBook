package com.devgrapher.ocebook.util;

import android.util.Log;

import com.devgrapher.ocebook.App;

/**
 * Logging class
 */
public class Laz {
    /**
     * Delays building up a log string by taking the code with lambda.
     * @param logging Logging code fragment that needs some processing to build up log string.
     */
    public static void y(Runnable logging) {
        if (!App.isDebugging())
            return;
        logging.run();
    }

    /**
     * Convenience replacement of Log.d
     * @param tag
     * @param msg
     */
    public static void yLog(String tag, String msg) {
        if (!App.isDebugging())
            return;
        Log.d(tag, msg);
    }
}
