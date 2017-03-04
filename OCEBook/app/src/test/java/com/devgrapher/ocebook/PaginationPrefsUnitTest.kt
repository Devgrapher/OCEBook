package com.devgrapher.ocebook

import com.devgrapher.ocebook.util.PaginationPrefs

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner

import org.junit.Assert.assertEquals
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy


@RunWith(MockitoJUnitRunner::class)
class PaginationPrefsUnitTest {

    @Test
    @Throws(Exception::class)
    fun recalculateSpinePage() {
        run {
            val prefs = spy<PaginationPrefs>(PaginationPrefs::class.java)
            doReturn(2).`when`(prefs).spineTotalPage
            doReturn(0).`when`(prefs).spinePage

            assertEquals(0, prefs.recalculateSpinePage(2).toLong())
            assertEquals(0, prefs.recalculateSpinePage(4).toLong())
        }
        run {
            val prefs = spy<PaginationPrefs>(PaginationPrefs::class.java)
            doReturn(4).`when`(prefs).spineTotalPage
            // last page
            doReturn(3).`when`(prefs).spinePage

            assertEquals(1, prefs.recalculateSpinePage(2).toLong())
            assertEquals(4, prefs.recalculateSpinePage(6).toLong())
        }
    }
}
