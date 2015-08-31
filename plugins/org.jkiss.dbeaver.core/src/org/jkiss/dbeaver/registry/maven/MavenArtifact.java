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
package org.jkiss.dbeaver.registry.maven;

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Maven artifact descriptor
 */
public class MavenArtifact
{
    static final Log log = Log.getLog(MavenArtifact.class);

    private final MavenRepository repository;
    private final String groupId;
    private final String artifactId;
    private List<String> versions = new ArrayList<String>();
    private String latestVersion;
    private String releaseVersion;
    private Date lastUpdate;

    public MavenArtifact(MavenRepository repository, String groupId, String artifactId)
        throws IOException
    {
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;

        String metadataPath = getArtifactDir() + "maven-metadata.xml";
        URL url = new URL(metadataPath);
        URLConnection connection = url.openConnection();
        connection.connect();
        InputStream mdStream = connection.getInputStream();
        try {
            SAXReader reader = new SAXReader(mdStream);
            reader.parse(new SAXListener() {
                public String lastTag;

                @Override
                public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
                    lastTag = localName;

                }

                @Override
                public void saxText(SAXReader reader, String data) throws XMLException {
                    if ("version".equals(lastTag)) {
                        versions.add(data);
                    } else if ("latest".equals(lastTag)) {
                        latestVersion = data;
                    } else if ("release".equals(lastTag)) {
                        releaseVersion = data;
                    } else if ("lastUpdate".equals(lastTag)) {
                        try {
                            lastUpdate = new Date(Long.parseLong(data));
                        } catch (NumberFormatException e) {
                            log.warn(e);
                        }
                    }
                }

                @Override
                public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
                    lastTag = null;
                }
            });
        } catch (XMLException e) {
            log.warn("Error parsing artifact metadata", e);
        } finally {
            mdStream.close();
        }
    }

    public MavenRepository getRepository() {
        return repository;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public List<String> getVersions() {
        return versions;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    private String getArtifactDir() {
        String dir = (groupId + "/" + artifactId).replace('.', '/');
        return repository.getUrl() + dir + "/";
    }

    public URL getFileURL(String version) throws MalformedURLException {
        String fileURL = getArtifactDir() + version + "/" + artifactId + ".jar";
        return new URL(fileURL);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }
}
