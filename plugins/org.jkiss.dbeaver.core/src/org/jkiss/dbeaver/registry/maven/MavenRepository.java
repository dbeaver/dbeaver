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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Maven repository manager.
 */
public class MavenRepository
{
    static final Log log = Log.getLog(MavenRepository.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mavenRepository";

    public static final String METADATA_CACHE_FILE = "metadata-cache.xml";

    private static final String TAG_CACHE = "cache";
    private static final String TAG_ARTIFACT = "artifact";
    private static final String TAG_VERSION = "version";

    public static final String ATTR_NAME = "name";
    public static final String ATTR_URL = "url";
    public static final String ATTR_GROUP_ID = "groupId";
    public static final String ATTR_ARTIFACT_ID = "artifactId";
    public static final String ATTR_ACTIVE_VERSION = "activeVersion";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_FILE = "file";
    public static final String ATTR_UPDATE_TIME = "updateTime";

    private String id;
    private String name;
    private String url;

    private List<MavenArtifact> cachedArtifacts = new ArrayList<MavenArtifact>();

    public MavenRepository(IConfigurationElement config)
    {
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.name = config.getAttribute(RegistryConstants.ATTR_NAME);
        this.url = config.getAttribute(RegistryConstants.ATTR_URL);

        loadCache();
    }

    public void flushCache() {
        saveCache();
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Nullable
    public MavenArtifact getArtifact(@NotNull String groupId, @NotNull String artifactId, boolean create) {
        for (MavenArtifact artifact : cachedArtifacts) {
            if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)) {
                return artifact;
            }
        }
        if (create) {
            MavenArtifact artifact = new MavenArtifact(this, groupId, artifactId);
            addCachedArtifact(artifact);
            return artifact;
        } else {
            return null;
        }
    }

    private synchronized void addCachedArtifact(@NotNull MavenArtifact artifact) {
        cachedArtifacts.add(artifact);
//        saveCache();
    }

    File getLocalCacheDir()
    {
        File homeFolder = new File(DBeaverActivator.getInstance().getStateLocation().toFile(), "maven/" + id + "/");
        if (!homeFolder.exists()) {
            if (!homeFolder.mkdirs()) {
                log.warn("Can't create maven repository '" + name + "' cache folder '" + homeFolder + "'");
            }
        }

        return homeFolder;
    }

    synchronized private void loadCache() {
        File cacheFile = new File(getLocalCacheDir(), METADATA_CACHE_FILE);
        if (!cacheFile.exists()) {
            return;
        }
        try {
            InputStream mdStream = new FileInputStream(cacheFile);
            try {
                SAXReader reader = new SAXReader(mdStream);
                reader.parse(new SAXListener() {
                    public MavenArtifact lastArtifact;
                    @Override
                    public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
                        if (TAG_ARTIFACT.equals(localName)) {
                            lastArtifact = new MavenArtifact(
                                MavenRepository.this,
                                atts.getValue(ATTR_GROUP_ID),
                                atts.getValue(ATTR_ARTIFACT_ID));
                            lastArtifact.setActiveVersion(atts.getValue(ATTR_ACTIVE_VERSION));
                            cachedArtifacts.add(lastArtifact);
                        } else if (TAG_VERSION.equals(localName) && lastArtifact != null) {
                            MavenLocalVersion version = new MavenLocalVersion(
                                lastArtifact,
                                atts.getValue(ATTR_VERSION),
                                atts.getValue(ATTR_FILE),
                                new Date(Long.parseLong(atts.getValue(ATTR_UPDATE_TIME))));
                            lastArtifact.addLocalVersion(version);
                        }
                    }
                    @Override
                    public void saxText(SAXReader reader, String data) throws XMLException {
                    }

                    @Override
                    public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
                        if (TAG_ARTIFACT.equals(localName)) {
                            lastArtifact = null;
                        }
                    }
                });
            } catch (XMLException e) {
                log.warn("Error parsing cached Maven repository '" + id + "'", e);
            } finally {
                IOUtils.close(mdStream);
            }
        } catch (IOException e) {
            log.warn("IO error while reading cached Maven repository '" + id + "'", e);
        }
    }

    synchronized private void saveCache() {
        try {
            File cacheDir = getLocalCacheDir();
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    throw new IOException("Can't create cache directory '" + cacheDir.getAbsolutePath() + "'");
                }
            }
            File cacheFile = new File(cacheDir, METADATA_CACHE_FILE);
            OutputStream out = new FileOutputStream(cacheFile);
            try {
                XMLBuilder xml = new XMLBuilder(out, "utf-8");
                xml.setButify(true);
                xml.startElement(TAG_CACHE);
                xml.addAttribute(ATTR_NAME, name);
                xml.addAttribute(ATTR_URL, url);

                for (MavenArtifact artifact : cachedArtifacts) {
                    if (CommonUtils.isEmpty(artifact.getLocalVersions())) {
                        continue;
                    }
                    xml.startElement(TAG_ARTIFACT);
                    xml.addAttribute(ATTR_GROUP_ID, artifact.getGroupId());
                    xml.addAttribute(ATTR_ARTIFACT_ID, artifact.getArtifactId());
                    xml.addAttribute(ATTR_ACTIVE_VERSION, artifact.getActiveVersion());
                    for (MavenLocalVersion version : artifact.getLocalVersions()) {
                        xml.startElement(TAG_VERSION);
                        xml.addAttribute(ATTR_VERSION, version.getVersion());
                        xml.addAttribute(ATTR_FILE, version.getFileName());
                        xml.addAttribute(ATTR_UPDATE_TIME, version.getUpdateTime().getTime());
                        xml.endElement();
                    }
                    xml.endElement();
                }

                xml.endElement();

                xml.flush();
            } finally {
                IOUtils.close(out);
            }
        } catch (IOException e) {
            log.warn("Error saving local Maven cache", e);
        }
    }

}
