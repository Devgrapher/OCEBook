package com.devgrapher.ocebook.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

/**
 * Created by Brent on 2/20/17.
 */

class PaginationPrefs {
    private val mPrefs: SharedPreferences?

    constructor() {
        mPrefs = null
    }

    constructor(context: Context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        Laz.y {
            Log.d(TAG, String.format("%d, %d, %d",
                    spineIndex,
                    spinePage,
                    spineTotalPage))
        }
    }

    val spineIndex: Int
        get() {
            if (mPrefs == null)
                return 0
            return mPrefs.getInt(PREF_SPINE_INDEX, 0)
        }

    val spinePage: Int
        get() {
            if (mPrefs == null)
                return 0
            return mPrefs.getInt(PREF_SPINE_PAGE, 0)
        }

    val spineTotalPage: Int
        get() {
            if (mPrefs == null)
                return 0
            return mPrefs.getInt(PREF_SPINE_TOTAL_PAGE, 1)
        }

    /**
     * @return new page in the spine based on new total page count.
     */
    fun recalculateSpinePage(newTotalSpinePage: Int): Int {
        var spineTotalPageCount = spineTotalPage.toFloat()
        if (spineTotalPageCount == 0f) {
            // Prevent divide by zero.
            spineTotalPageCount = 1f
        }

        var newPage = (newTotalSpinePage * (spinePage / spineTotalPageCount)).toInt()

        // Prevent out of range.
        if (newPage >= newTotalSpinePage) {
            newPage = newTotalSpinePage - 1
        }
        return newPage
    }

    companion object {
        private val TAG = PaginationPrefs::class.java.toString()
        private val PREF_SPINE_INDEX = "pref_spine_index"
        private val PREF_SPINE_PAGE = "pref_spine_page"
        private val PREF_SPINE_TOTAL_PAGE = "pref_spine_total_page"

        fun save(context: Context, spineIndex: Int, spinePage: Int, spineTotalPage: Int) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            prefs.edit().putInt(PREF_SPINE_INDEX, spineIndex)
                    .putInt(PREF_SPINE_PAGE, spinePage)
                    .putInt(PREF_SPINE_TOTAL_PAGE, spineTotalPage)
                    .apply()
        }
    }
}
