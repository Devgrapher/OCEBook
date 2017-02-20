package com.devgrapher.ocebook;

import com.devgrapher.ocebook.util.PaginationPrefs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PaginationPrefsUnitTest {

    @Test
    public void recalculateSpinePage() throws Exception {
        {
            PaginationPrefs prefs = spy(PaginationPrefs.class);
            doReturn(2).when(prefs).getSpineTotalPage();
            doReturn(0).when(prefs).getSpinePage();

            assertEquals(0, prefs.recalculateSpinePage(2));
            assertEquals(0, prefs.recalculateSpinePage(4));
        }
        {
            PaginationPrefs prefs = spy(PaginationPrefs.class);
            doReturn(4).when(prefs).getSpineTotalPage();
            // last page
            doReturn(3).when(prefs).getSpinePage();

            assertEquals(1, prefs.recalculateSpinePage(2));
            assertEquals(4, prefs.recalculateSpinePage(6));
        }
    }
}
