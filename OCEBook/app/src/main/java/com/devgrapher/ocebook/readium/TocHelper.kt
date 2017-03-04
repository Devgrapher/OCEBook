package com.devgrapher.ocebook.readium

import org.readium.sdk.android.Package
import org.readium.sdk.android.components.navigation.NavigationElement
import org.readium.sdk.android.components.navigation.NavigationPoint

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

    fun flatTableOfContents(pckg: Package): List<NavigationPoint> {
        return flatNavigationElement(pckg.tableOfContents)
                .filter{ e -> e is NavigationPoint }
                .map { e -> e as NavigationPoint }
    }

    fun flatNavigationElement(elem: NavigationElement): List<NavigationElement> {
        return listOf<NavigationElement>(elem)
                .plus(elem.children.flatMap { e -> flatNavigationElement(e) })
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
