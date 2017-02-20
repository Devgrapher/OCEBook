package com.devgrapher.ocebook.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Brent on 2/20/17.
 */

public class PaginationPrefs {
    private static final String TAG = PaginationPrefs.class.toString();
    private static final String PREF_SPINE_INDEX = "pref_spine_index";
    private static final String PREF_SPINE_PAGE = "pref_spine_page";
    private static final String PREF_SPINE_TOTAL_PAGE = "pref_spine_total_page";
    private final SharedPreferences mPrefs;

    public PaginationPrefs() {
        mPrefs = null;
    }

    public PaginationPrefs(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Laz.y(()-> Log.d(TAG, String.format("%d, %d, %d",
                getSpineIndex(),
                getSpinePage(),
                getSpineTotalPage())));
    }

    public static void save(Context context, int spineIndex, int spinePage, int spineTotalPage) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);

        prefs.edit().putInt(PREF_SPINE_INDEX, spineIndex)
                .putInt(PREF_SPINE_PAGE, spinePage)
                .putInt(PREF_SPINE_TOTAL_PAGE, spineTotalPage)
                .apply();
    }

    public int getSpineIndex() {
        if (mPrefs == null)
            return 0;
        return mPrefs.getInt(PREF_SPINE_INDEX, 0);
    }

    public int getSpinePage() {
        if (mPrefs == null)
            return 0;
        return mPrefs.getInt(PREF_SPINE_PAGE, 0);
    }

    public int getSpineTotalPage() {
        if (mPrefs == null)
            return 0;
        return mPrefs.getInt(PREF_SPINE_TOTAL_PAGE, 1);
    }

    /**
     * @return new page in the spine based on new total page count.
     */
    public int recalculateSpinePage(int newTotalSpinePage)  {
        float spineTotalPageCount = getSpineTotalPage();
        if (spineTotalPageCount == 0) {
            // Prevent divide by zero.
            spineTotalPageCount = 1;
        }

        int newPage = (int) (newTotalSpinePage * (getSpinePage() / spineTotalPageCount));

        // Prevent out of range.
        if (newPage >= newTotalSpinePage) {
            newPage = newTotalSpinePage - 1;
        }
        return newPage;
    }
}
