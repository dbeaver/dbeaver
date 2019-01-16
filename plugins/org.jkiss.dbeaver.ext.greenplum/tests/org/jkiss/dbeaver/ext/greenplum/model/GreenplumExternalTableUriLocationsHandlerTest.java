package org.jkiss.dbeaver.ext.greenplum.model;

import org.junit.Assert;
import org.junit.Test;


public class GreenplumExternalTableUriLocationsHandlerTest {
    @Test
    public void getCommaSeparatedList_whenConstructedWithALineFeedSeparatedListOfLocations_shouldReturnCommaSeparatedString() {
        GreenplumExternalTableUriLocationsHandler handler =
                new GreenplumExternalTableUriLocationsHandler("location1\nlocation2", '\n');
        Assert.assertEquals("location1,location2", handler.getCommaSeparatedList());
    }

    @Test
    public void getLineFeedSeparatedList_whenConstructedWithALineFeedSeparatedListOfLocations_shouldReturnLineFeedSeparatedString() {
        GreenplumExternalTableUriLocationsHandler handler =
                new GreenplumExternalTableUriLocationsHandler("location1\nlocation2", '\n');
        Assert.assertEquals("location1\nlocation2", handler.getLineFeedSeparatedList());
    }
}