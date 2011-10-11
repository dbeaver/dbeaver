/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.updater;

import java.util.List;

/**
 * Update descriptor
 */
public class ProgramDescriptor {

    private String programName;
    private String programVersion;
    private String updateTime;
    private String baseURL;
    private String releaseNotes;

    private List<UpdateSiteDescriptor> updateSites;
    private List<DistributionDescriptor> distributions;

}
