package com.devgrapher.ocebook.readium

import org.readium.sdk.android.Package
import org.readium.sdk.android.components.navigation.NavigationElement
import org.readium.sdk.android.components.navigation.NavigationPoint
import org.readium.sdk.android.components.navigation.NavigationTable

import java.util.ArrayList

/**
 * Help to navigate through the Table of Contents.
 * This flattens children tree and caches it.
 */

object TocHelper {
    fun getAt(pckg: Package, idx: Int): NavigationPoint? {
        val toc = flatTableOfContents(pckg).toTypedArray()
        if (idx >= toc.size)
            return null
        return toc[idx]
    }

    fun flatTableOfContents(pckg: Package): ArrayList<NavigationPoint> {
        val flatten = ArrayList<NavigationPoint>()

        flatNavigationElement(pckg.tableOfContents, flatten)
        return flatten
    }

    private fun flatNavigationElement(table: NavigationElement,
                                      flatten: ArrayList<NavigationPoint>) {
        for (e in table.getChildren()) {
            if (e is NavigationTable)
                flatNavigationElement(e, flatten)
            else if (e is NavigationPoint)
                flatten.add(e)
        }
    }
    /* target sdk 24 or higher
    public static NavigationPoint getAt(Package pckg, int idx) {
        Object[] toc = flatTableOfContents(pckg).toArray();
        if (idx >= toc.length)
            return null;
        return (NavigationPoint) toc[idx];
    }

    public static Stream<NavigationPoint> flatTableOfContents(Package pckg) {
        return flatNavigationElement(pckg.getTableOfContents())
                .filter(e -> e instanceof NavigationPoint)
                .map(e -> (NavigationPoint)e);
    }

    private static Stream<NavigationElement> flatNavigationElement(final NavigationElement elem) {
        return Stream.concat(
                Stream.of(elem),
                elem.getChildren().stream().flatMap(e -> flatNavigationElement(e)));
    }
 */
}
