package com.devgrapher.ocebook.model;

/**
 * Created by Brent on 2/16/17.
 */

public class Page {
    private final int mSpineItemPageIndex;
    private final int mSpineItemPageCount;
    private final String mIdref;
    private final int mSpineItemIndex;

    public Page(int spineItemPageIndex, int spineItemPageCount,
                String idref, int spineItemIndex) {
        mSpineItemPageIndex = spineItemPageIndex;
        mSpineItemPageCount = spineItemPageCount;
        mIdref = idref;
        mSpineItemIndex = spineItemIndex;
    }

    public int getSpineItemPageIndex() {
        return mSpineItemPageIndex;
    }

    public int getSpineItemPageCount() {
        return mSpineItemPageCount;
    }

    public String getIdref() {
        return mIdref;
    }

    public int getSpineItemIndex() {
        return mSpineItemIndex;
    }

    @Override
    public String toString() {
        return "Page [spineItemPageIndex=" + mSpineItemPageIndex
                + ", spineItemPageCount=" + mSpineItemPageCount + ", idref="
                + mIdref + ", spineItemIndex=" + mSpineItemIndex + "]";
    }
}
