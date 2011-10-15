/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.updater;

import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Version descriptor
 */
public class VersionDescriptor {

    private String programName;
    private String programVersion;
    private String updateTime;
    private String baseURL;
    private String releaseNotes;

    private final List<DistributionDescriptor> distributions = new ArrayList<DistributionDescriptor>();
    private final List<UpdateSiteDescriptor> updateSites = new ArrayList<UpdateSiteDescriptor>();

    public VersionDescriptor(String fileAddr)
        throws IOException
    {
        URL url = new URL(fileAddr);
        URLConnection connection = url.openConnection();
        try {
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            try {
                InputStream inputStream = connection.getInputStream();
                try {
                    Document document = XMLUtils.parseDocument(inputStream);
                    parseVersionInfo(document);
                } finally {
                    ContentUtils.close(inputStream);
                }
            } catch (XMLException e) {
                throw new IOException("XML parse error", e);
            }
        }
        finally {
            connection.connect();
        }
    }

    public String getProgramName()
    {
        return programName;
    }

    public String getProgramVersion()
    {
        return programVersion;
    }

    public String getUpdateTime()
    {
        return updateTime;
    }

    public String getBaseURL()
    {
        return baseURL;
    }

    public String getReleaseNotes()
    {
        return releaseNotes;
    }

    public Collection<DistributionDescriptor> getDistributions()
    {
        return distributions;
    }

    public Collection<UpdateSiteDescriptor> getUpdateSites()
    {
        return updateSites;
    }

    private void parseVersionInfo(Document document)
    {
        Element root = document.getDocumentElement();
        programName = XMLUtils.getChildElementBody(root, "name");
        programVersion = XMLUtils.getChildElementBody(root, "number");
        updateTime = XMLUtils.getChildElementBody(root, "date");
        baseURL = XMLUtils.getChildElementBody(root, "base-url");
        releaseNotes = XMLUtils.getChildElementBody(root, "release-notes");

        for (Element dist : XMLUtils.getChildElementList(root, "distribution")) {
            distributions.add(new DistributionDescriptor(dist));
        }

        for (Element dist : XMLUtils.getChildElementList(root, "site")) {
            updateSites.add(new UpdateSiteDescriptor(dist));
        }
    }
}
