//  Copyright (c) 2014 Readium Foundation and/or its licensees. All rights reserved.
//  Redistribution and use in source and binary forms, with or without modification, 
//  are permitted provided that the following conditions are met:
//  1. Redistributions of source code must retain the above copyright notice, this 
//  list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright notice, 
//  this list of conditions and the following disclaimer in the documentation and/or 
//  other materials provided with the distribution.
//  3. Neither the name of the organization nor the names of its contributors may be 
//  used to endorse or promote products derived from this software without specific 
//  prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
//  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
//  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
//  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
//  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
//  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
//  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
//  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
//  OF THE POSSIBILITY OF SUCH DAMAGE

package com.devgrapher.ocebook.model

import org.json.JSONException
import org.json.JSONObject

class ViewerSettings(val syntheticSpreadMode: ViewerSettings.SyntheticSpreadMode, val scrollMode: ViewerSettings.ScrollMode, val fontSize: Int, val columnGap: Int) {

    enum class SyntheticSpreadMode {
        AUTO,
        DOUBLE,
        SINGLE
    }

    enum class ScrollMode {
        AUTO,
        DOCUMENT,
        CONTINUOUS
    }

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        val json = JSONObject()

        var syntheticSpread = ""
        when (syntheticSpreadMode) {
            ViewerSettings.SyntheticSpreadMode.AUTO -> syntheticSpread = "auto"
            ViewerSettings.SyntheticSpreadMode.DOUBLE -> syntheticSpread = "double"
            ViewerSettings.SyntheticSpreadMode.SINGLE -> syntheticSpread = "single"
        }
        json.put("syntheticSpread", syntheticSpread)

        var scroll = ""
        when (scrollMode) {

            ViewerSettings.ScrollMode.AUTO -> scroll = "auto"
            ViewerSettings.ScrollMode.DOCUMENT -> scroll = "scroll-doc"
            ViewerSettings.ScrollMode.CONTINUOUS -> scroll = "scroll-continuous"
        }
        json.put("scroll", scroll)

        json.put("fontSize", fontSize)
        json.put("columnGap", columnGap)
        return json
    }

    override fun toString(): String {
        return "ViewerSettings{" +
                "mSyntheticSpreadMode=" + syntheticSpreadMode +
                ", mScrollMode=" + scrollMode +
                ", mFontSize=" + fontSize +
                ", mColumnGap=" + columnGap +
                '}'
    }

}
