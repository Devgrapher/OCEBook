package com.devgrapher.ocebook.util

/**
 * Created by Brent on 2/19/17.
 */

class PageCounts(totalCount: Int) {
    private val mPageCountAccumulates: IntArray
    var totalCount: Int = 0
        private set
    var isUpdating = true
        private set

    init {
        mPageCountAccumulates = IntArray(totalCount)
    }

    fun updatePage(spineIndex: Int, pageCount: Int) {
        mPageCountAccumulates[spineIndex] = totalCount
        totalCount += pageCount
    }

    fun updateComplete() {
        isUpdating = false
    }

    fun calculateCurrentPage(spineIndex: Int, spainePage: Int): Int {
        if (spineIndex < mPageCountAccumulates.size) {
            return mPageCountAccumulates[spineIndex] + spainePage
        } else {
            return 0
        }
    }

}
