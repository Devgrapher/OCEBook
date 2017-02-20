package com.devgrapher.ocebook.util;

import com.devgrapher.ocebook.model.PaginationInfo;

/**
 * Created by Brent on 2/19/17.
 */

public class PageCounts {
    private final int[] mPageCountAccumulates;
    private int mTotalCount;
    private boolean isUpdating = true;

    public PageCounts(int totalCount) {
        mPageCountAccumulates = new int[totalCount];
    }

    public void updatePage(int spineIndex, int pageCount) {
        mPageCountAccumulates[spineIndex] = mTotalCount;
        mTotalCount += pageCount;
    }

    public void updateComplete() {
        isUpdating = false;
    }

    public boolean isUpdating() {
        return isUpdating;
    }

    public int getTotalCount() {
        return mTotalCount;
    }

    public int calculateCurrentPage(int spineIndex, int spainePage) {
        if (spineIndex < mPageCountAccumulates.length) {
            return mPageCountAccumulates[spineIndex] + spainePage;
        } else {
            return 0;
        }
    }

}
