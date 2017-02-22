package com.devgrapher.ocebook.readium;

import org.readium.sdk.android.Package;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;
import org.readium.sdk.android.components.navigation.NavigationTable;

import java.util.ArrayList;

/**
 * Help to navigate through the Table of Contents.
 * This flattens children tree and caches it.
 */

public class TocHelper {
    public static NavigationPoint getAt(Package pckg, int idx) {
        Object[] toc = flatTableOfContents(pckg).toArray();
        if (idx >= toc.length)
            return null;
        return (NavigationPoint) toc[idx];
    }

    public static ArrayList<NavigationPoint> flatTableOfContents(Package pckg) {
        ArrayList<NavigationPoint> flatten = new ArrayList<>();

        flatNavigationElement(pckg.getTableOfContents(), flatten);
        return flatten;
    }

    private static void flatNavigationElement(NavigationElement table,
                                       ArrayList<NavigationPoint> flatten) {
        for (NavigationElement e: table.getChildren()) {
            if (e instanceof NavigationTable)
                flatNavigationElement(e, flatten);
            else if (e instanceof NavigationPoint)
                flatten.add((NavigationPoint)e);
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
