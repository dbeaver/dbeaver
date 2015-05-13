/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.registry.updater;

import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.osgi.framework.Version;
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

    public static final String DEFAULT_VERSION_URL = "http://dbeaver.jkiss.org/product/version.xml";

    private String programName;
    private Version programVersion;
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
            // nothing
        }
    }

    public String getProgramName()
    {
        return programName;
    }

    public Version getProgramVersion()
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
        programVersion = Version.parseVersion(XMLUtils.getChildElementBody(root, "number"));
        updateTime = XMLUtils.getChildElementBody(root, "date");
        baseURL = XMLUtils.getChildElementBody(root, "base-url");
        releaseNotes = CommonUtils.toString(XMLUtils.getChildElementBody(root, "release-notes")).trim();

        for (Element dist : XMLUtils.getChildElementList(root, "distribution")) {
            distributions.add(new DistributionDescriptor(dist));
        }

        for (Element dist : XMLUtils.getChildElementList(root, "site")) {
            updateSites.add(new UpdateSiteDescriptor(dist));
        }
    }
}
