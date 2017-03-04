package com.devgrapher.ocebook

import com.devgrapher.ocebook.readium.TocHelper

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.readium.sdk.android.Package
import org.readium.sdk.android.components.navigation.NavigationElement
import org.readium.sdk.android.components.navigation.NavigationPoint
import org.readium.sdk.android.components.navigation.NavigationTable

import java.util.ArrayList
import java.util.stream.Collectors

import org.junit.Assert.assertTrue
import org.mockito.Mockito.`when`

/**
 * Instrumentation test, which will execute on an Android device.

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */

@RunWith(MockitoJUnitRunner::class)
class TOCHelperUnitTest {
    internal var flatElem: MutableList<NavigationElement>

    init {
        flatElem = ArrayList<NavigationElement>()
    }

    @Mock
    internal var mPackage: Package? = null

    private fun createSample(): NavigationTable {
        val root = NavigationTable("table", "root", "/")
        root.children.add(NavigationPoint("1", "1"))
        root.children.add(NavigationPoint("2", "2"))

        val sub = NavigationTable("talbe", "sub", "/sub")
        sub.children.add(NavigationPoint("sub/1", "sub/1"))
        root.children.add(sub)

        val subsub = NavigationTable("talbe", "subsub", "/sub/sub")
        subsub.children.add(NavigationPoint("subsub/1", "subsub/1"))
        sub.children.add(subsub)

        return root
    }

    private fun flatNavigationByForeach(table: NavigationElement): List<NavigationElement> {
        for (e in table.children) {
            if (e is NavigationTable)
                flatNavigationByForeach(e)
            else
                flatElem.add(e)
        }
        return flatElem
    }

    /**
     * Confirm that FP implementation is equal to foreach implementation.
     * @throws Exception
     */
    @Ignore("This test will be ignored")
    @Test
    @Throws(Exception::class)
    fun flatNavigation() {
        `when`(mPackage!!.tableOfContents)
                .thenReturn(createSample())
        /*
        List<NavigationElement> fromFP = TocHelper.flatTableOfContents(mPackage).collect(Collectors.toList());
        List<NavigationElement> fromForeach = flatNavigationByForeach(mPackage.getTableOfContents());

        assertTrue(fromFP.containsAll(fromForeach));*/
    }
}
