package com.devgrapher.ocebook;

import android.content.Context;

import com.devgrapher.ocebook.readium.TocHelper;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.readium.sdk.android.Package;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;
import org.readium.sdk.android.components.navigation.NavigationTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(MockitoJUnitRunner.class)
public class TOCHelperUnitTest {
    List<NavigationElement> flatElem;

    @Mock
    Package mPackage;

    private NavigationTable createSample() {
        flatElem = new ArrayList<>();

        NavigationTable root = new NavigationTable("table", "root", "/");
        root.getChildren().add(new NavigationPoint("1", "1"));
        root.getChildren().add(new NavigationPoint("2", "2"));

        NavigationTable sub = new NavigationTable("talbe", "sub", "/sub");
        sub.getChildren().add(new NavigationPoint("sub/1", "sub/1"));
        root.getChildren().add(sub);

        NavigationTable subsub = new NavigationTable("talbe", "subsub", "/sub/sub");
        subsub.getChildren().add(new NavigationPoint("subsub/1", "subsub/1"));
        sub.getChildren().add(subsub);

        return root;
    }

    private List<NavigationElement> flatNavigationByForeach(NavigationElement table) {
        for (NavigationElement e: table.getChildren()) {
            if (e instanceof NavigationTable)
                flatNavigationByForeach(e);
            else
                flatElem.add(e);
        }
        return flatElem;
    }

    /**
     * Confirm that FP implementation is equal to foreach implementation.
     * @throws Exception
     */
    @Ignore("This test will be ignored")
    @Test
    public void flatNavigation() throws Exception {
        when(mPackage.getTableOfContents())
                .thenReturn(createSample());

        List<NavigationElement> fromFP = TocHelper.flatTableOfContents(mPackage).collect(Collectors.toList());
        List<NavigationElement> fromForeach = flatNavigationByForeach(mPackage.getTableOfContents());

        assertTrue(fromFP.containsAll(fromForeach));
    }
}
