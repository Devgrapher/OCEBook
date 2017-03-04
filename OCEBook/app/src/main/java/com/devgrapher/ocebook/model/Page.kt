package com.devgrapher.ocebook.model

/**
 * Created by Brent on 2/16/17.
 */

class Page(val spineItemPageIndex: Int, val spineItemPageCount: Int,
           val idref: String, val spineItemIndex: Int) {

    override fun toString(): String {
        return "Page [spineItemPageIndex=" + spineItemPageIndex +
                ", spineItemPageCount=" + spineItemPageCount + ", idref=" +
                idref + ", spineItemIndex=" + spineItemIndex + "]"
    }
}
