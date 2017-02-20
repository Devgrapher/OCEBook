package com.devgrapher.ocebook.readium;

import org.readium.sdk.android.Package;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;

import java.util.stream.Stream;

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
}
