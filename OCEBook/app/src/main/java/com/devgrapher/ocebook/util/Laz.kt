package com.devgrapher.ocebook.util

import android.util.Log

import com.devgrapher.ocebook.App

/**
 * Logging class
 */
object Laz {
    /**
     * Delays building up a log string by taking the code with lambda.
     * @param logging Logging code fragment that needs some processing to build up log string.
     */
    fun y(logging: () -> Unit) {
        if (!App.isDebugging)
            return
        logging.invoke()
    }

    /**
     * Convenience replacement of Log.d
     * @param tag
     * *
     * @param msg
     */
    fun yLog(tag: String, msg: String) {
        if (!App.isDebugging)
            return
        Log.d(tag, msg)
    }
}
