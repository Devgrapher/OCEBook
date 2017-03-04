package com.devgrapher.ocebook.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList

/**
 * Created by Brent on 2/16/17.
 */

class PaginationInfo(val pageProgressionDirection: String,
                     val isFixedLayout: Boolean, val spineItemCount: Int) {
    val openPages = ArrayList<Page>()

    fun getOpenPages(): MutableList<Page> {
        return openPages
    }

    companion object {

        @Throws(JSONException::class)
        fun fromJson(jsonString: String): PaginationInfo {
            val json = JSONObject(jsonString)
            val paginationInfo = PaginationInfo(json.optString("pageProgressionDirection", "ltr"),
                    json.optBoolean("isFixedLayout"),
                    json.optInt("spineItemCount"))
            val openPages = json.getJSONArray("openPages")
            for (i in 0..openPages.length() - 1) {
                val p = openPages.getJSONObject(i)
                val page = Page(p.optInt("spineItemPageIndex"), p.optInt("spineItemPageCount"),
                        p.optString("idref"), p.optInt("spineItemIndex"))
                paginationInfo.openPages.add(page)
            }
            return paginationInfo
        }
    }
}
