/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.registry.updater;

import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Version descriptor
 */
public class VersionDescriptor {

    private String programName;
    private Version programVersion;
    private String updateTime;
    private String baseURL;
    private String releaseNotes;

    private final List<DistributionDescriptor> distributions = new ArrayList<>();
    private final List<UpdateSiteDescriptor> updateSites = new ArrayList<>();

    public VersionDescriptor(String fileAddr)
        throws IOException
    {
        try (InputStream inputStream = WebUtils.openConnectionStream(fileAddr)) {
            Document document = XMLUtils.parseDocument(inputStream);
            parseVersionInfo(document);
        } catch (XMLException e) {
            throw new IOException("XML parse error", e);
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
        Element releaseNotesElem = XMLUtils.getChildElement(root, "release-notes");
        this.releaseNotes = releaseNotesElem == null ? "" : CommonUtils.toString(releaseNotesElem.getTextContent()).trim();

        for (Element dist : XMLUtils.getChildElementList(root, "distribution")) {
            distributions.add(new DistributionDescriptor(dist));
        }

        for (Element dist : XMLUtils.getChildElementList(root, "site")) {
            updateSites.add(new UpdateSiteDescriptor(dist));
        }
    }
}
