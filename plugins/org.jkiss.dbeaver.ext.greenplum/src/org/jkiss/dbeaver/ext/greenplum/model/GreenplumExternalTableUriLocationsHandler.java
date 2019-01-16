package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GreenplumExternalTableUriLocationsHandler {
    private final List<String> uriLocations;

    public GreenplumExternalTableUriLocationsHandler(String uriLocationsString, char separator) {
        this.uriLocations = CommonUtils.splitString(uriLocationsString, separator);
    }

    public String getCommaSeparatedList() {
        return CommonUtils.joinStrings(",", this.uriLocations);
    }

    public String getLineFeedSeparatedList() {
        return CommonUtils.joinStrings("\n", this.uriLocations);
    }

    public Stream<String> stream() {
        return this.uriLocations.stream();
    }
}
