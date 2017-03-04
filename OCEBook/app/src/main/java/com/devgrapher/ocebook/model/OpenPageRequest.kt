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

class OpenPageRequest private constructor(private val idref: String?, private val spineItemPageIndex: Int?,
                                          private val elementCfi: String?, private val contentRefUrl: String?, private val sourceFileHref: String?, private val elementId: String?) {

    @Throws(JSONException::class)
    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put("idref", idref)
        json.put("spineItemPageIndex", spineItemPageIndex)
        json.put("elementCfi", elementCfi)
        json.put("contentRefUrl", contentRefUrl)
        json.put("sourceFileHref", sourceFileHref)
        json.put("elementId", elementId)
        return json
    }

    companion object {

        fun fromIdref(idref: String): OpenPageRequest {
            return OpenPageRequest(idref, 0, null, null, null, null)
        }

        fun fromIdrefAndIndex(idref: String, spineItemPageIndex: Int): OpenPageRequest {
            return OpenPageRequest(idref, spineItemPageIndex, null, null, null, null)
        }

        fun fromIdrefAndCfi(idref: String, elementCfi: String): OpenPageRequest {
            return OpenPageRequest(idref, null, elementCfi, null, null, null)
        }

        fun fromContentUrl(contentRefUrl: String, sourceFileHref: String): OpenPageRequest {
            return OpenPageRequest(null, null, null, contentRefUrl, sourceFileHref, null)
        }

        fun fromElementId(idref: String, elementId: String): OpenPageRequest {
            return OpenPageRequest(idref, null, null, null, null, elementId)
        }

        @Throws(JSONException::class)
        fun fromJSON(data: String): OpenPageRequest {
            val json = JSONObject(data)
            val spineItemPageIndex = if (json.has("spineItemPageIndex")) json.getInt("spineItemPageIndex") else null
            return OpenPageRequest(json.optString("idref", null), spineItemPageIndex,
                    // get elementCfi and then contentCFI (from bookmarkData) if it was empty
                    json.optString("elementCfi", json.optString("contentCFI", null)),
                    json.optString("contentRefUrl", null),
                    json.optString("sourceFileHref", null), json.optString("elementId", null))
        }
    }
}
