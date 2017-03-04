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

package com.devgrapher.ocebook.util

import org.readium.sdk.android.util.ResourceInputStream

import java.io.IOException
import java.io.InputStream

class ByteRangeInputStream(protected val ris: ResourceInputStream, private val isRange: Boolean,
                           private val criticalSectionSynchronizedLock: Any) : InputStream() {

    private var requestedOffset: Long = 0
    private var alreadyRead: Long = 0

    private var isOpen = true

    @Throws(IOException::class)
    override fun close() {
        isOpen = false
        synchronized(criticalSectionSynchronizedLock) {
            ris.close()
        }
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (isOpen) {
            val buffer = ByteArray(1)
            if (read(buffer) == 1) {
                return buffer[0].toInt()
            }
        }
        return -1
    }

    @Throws(IOException::class)
    override fun available(): Int {
        var available: Int = 0
        synchronized(criticalSectionSynchronizedLock) {
            available = ris.available()
        }
        var remaining = available - alreadyRead
        if (remaining < 0) {
            remaining = 0
        }
        return remaining.toInt()
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
        if (isRange) {
            requestedOffset = alreadyRead + byteCount
        } else if (byteCount != 0L) {
            synchronized(criticalSectionSynchronizedLock) {
                return ris.skip(byteCount)
            }
        }
        return byteCount
    }

    @Synchronized @Throws(IOException::class)
    override fun reset() {
        if (isRange) {
            requestedOffset = 0
            alreadyRead = 0
        } else {
            synchronized(criticalSectionSynchronizedLock) {
                ris.reset()
            }
        }
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, offset: Int, len: Int): Int {
        if (offset != 0) {
            throw IOException("Offset parameter can only be zero")
        }
        if (len == 0 || !isOpen) {
            return -1
        }
        var read: Int = 0

        synchronized(criticalSectionSynchronizedLock) {

            if (isRange) {

                read = ris.getRangeBytesX(requestedOffset + alreadyRead, len.toLong(), b).toInt()

            } else {
                read = ris.readX(len.toLong(), b).toInt()
            }
        }

        alreadyRead += read.toLong()
        if (read == 0) {
            read = -1
        }
        return read
    }
}
