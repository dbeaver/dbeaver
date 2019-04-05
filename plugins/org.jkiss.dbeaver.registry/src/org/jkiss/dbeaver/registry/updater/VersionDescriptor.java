/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.osgi.framework.Version;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

    public VersionDescriptor(DBPPlatform platform, String fileAddr)
        throws IOException {
        try (InputStream inputStream = WebUtils.openConnection(fileAddr, platform.getWorkspace().getWorkspaceId()).getInputStream()) {
            parseVersionInfo(inputStream);
        } catch (XMLException e) {
            throw new IOException("XML parse error", e);
        }
    }

    public String getProgramName() {
        return programName;
    }

    public Version getProgramVersion() {
        return programVersion;
    }

    public void setProgramVersion(Version programVersion) {
        this.programVersion = programVersion;
    }

    public String getPlainVersion() {
        return programVersion.getMajor() + "." + programVersion.getMinor() + "." + programVersion.getMicro();
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Date getVersionReleaseTimestamp() {
        try {
            return new SimpleDateFormat("dd.MM.yyyy").parse(updateTime);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public Collection<DistributionDescriptor> getDistributions() {
        return distributions;
    }

    public Collection<UpdateSiteDescriptor> getUpdateSites() {
        return updateSites;
    }

    private void parseVersionInfo(InputStream inputStream) throws IOException, XMLException {
        SAXReader parser = new SAXReader(inputStream);
        SAXListener dsp = new SAXListener() {
            private String lastTag;
            private StringBuilder textBuffer = new StringBuilder();

            @Override
            public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
                lastTag = localName;
                textBuffer.setLength(0);
            }

            @Override
            public void saxText(SAXReader reader, String data) throws XMLException {
                textBuffer.append(data);
            }

            @Override
            public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
                final String text = textBuffer.toString();
                switch (lastTag) {
                    case "name":
                        programName = text;
                        break;
                    case "number":
                        programVersion = Version.parseVersion(text);
                        break;
                    case "date":
                        updateTime = text;
                        break;
                    case "base-url":
                        baseURL = text;
                        break;
                    case "release-notes":
                        releaseNotes = text;
                        break;
                }
                textBuffer.setLength(0);
            }
        };
        parser.parse(dsp);
    }

}
