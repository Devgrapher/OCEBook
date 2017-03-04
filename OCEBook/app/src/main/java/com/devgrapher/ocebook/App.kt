package com.devgrapher.ocebook

import android.app.Application
import android.content.pm.ApplicationInfo

/**
 * Created by Brent on 2/21/17.
 */

class App : Application() {
    init {
        sInstance = this
    }

    companion object {

        private var sInstance: App? = null

        val isDebugging: Boolean
            get() {
                return (sInstance?.applicationInfo?.flags ?: 0 and
                        ApplicationInfo.FLAG_DEBUGGABLE) != 0
            }
    }
}
