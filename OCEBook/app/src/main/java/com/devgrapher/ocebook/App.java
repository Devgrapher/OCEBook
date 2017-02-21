package com.devgrapher.ocebook;

import android.app.Application;
import android.content.pm.ApplicationInfo;

/**
 * Created by Brent on 2/21/17.
 */

public class App extends Application {

    private static App sInstance;

    public App() {
        super();
        sInstance = this;
    }

    public static boolean isDebugging() {
        if (sInstance == null) return false;

        return (sInstance.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
