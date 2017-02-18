package com.devgrapher.ocebook.readium;

import android.util.Log;

import org.readium.sdk.android.Package;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Brent on 2/17/17.
 *
 * Help to navigate through the Table of Contents.
 * This flattens children tree and caches it.
 */

public class TocManager {
    private List<NavigationPoint> mToc;
    private final Package mPackage;

    public TocManager(Package pckg) {
        mPackage = pckg;
        mToc = flatNavigationElement(mPackage.getTableOfContents())
                .filter(e -> e instanceof NavigationPoint)
                .map(e -> (NavigationPoint)e)
                .collect(Collectors.toList());
        mToc.forEach(e -> Log.d("######", e.getTitle()));
    }

    public Stream<NavigationElement> flatNavigationElement(final NavigationElement elem) {
        return Stream.concat(
                Stream.of(elem),
                elem.getChildren().stream().flatMap(e -> flatNavigationElement(e)));
    }

    public Stream<NavigationPoint> getTableOfContents() {
        return mToc.stream();
    }
}
