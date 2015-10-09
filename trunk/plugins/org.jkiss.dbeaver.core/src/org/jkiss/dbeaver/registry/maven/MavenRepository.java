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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Maven repository manager.
 */
public class MavenRepository
{
    static final Log log = Log.getLog(MavenRepository.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mavenRepository";

    public static final String METADATA_CACHE_FILE = "metadata-cache.xml";

    public static final String TAG_CACHE = "cache";
    public static final String TAG_ARTIFACT = "artifact";
    public static final String TAG_VERSION = "version";
    public static final String TAG_DEPENDENCY = "dependency";
    public static final String TAG_EXCLUDE = "exclude";

    public static final String ATTR_NAME = "name";
    public static final String ATTR_URL = "url";
    public static final String ATTR_GROUP_ID = "groupId";
    public static final String ATTR_ARTIFACT_ID = "artifactId";
    public static final String ATTR_ACTIVE_VERSION = "activeVersion";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_PATH = "path";
    public static final String ATTR_SCOPE = "scope";
    public static final String ATTR_OPTIONAL = "optional";
    public static final String ATTR_PARENT = "parent";
    public static final String ATTR_UPDATE_TIME = "updateTime";

    private static final DateFormat UPDATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private String id;
    private String name;
    private String url;
    private boolean local;
    private boolean predefined = false;

    private transient volatile boolean needsToSave = false;

    private List<MavenArtifact> cachedArtifacts = new ArrayList<>();

    public MavenRepository(IConfigurationElement config)
    {
        this(
            config.getAttribute(RegistryConstants.ATTR_ID),
            config.getAttribute(RegistryConstants.ATTR_NAME),
            config.getAttribute(RegistryConstants.ATTR_URL),
            false);
        this.predefined = true;
    }

    MavenRepository(String id, String name, String url, boolean local) {
        this.id = id;
        this.name = name;
        if (!url.endsWith("/")) url += "/";
        this.url = url;
        this.local = local;
    }

    public void flushCache() {
        needsToSave = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isPredefined() {
        return predefined;
    }

    public boolean isLocal() {
        return local;
    }

    @Nullable
    public synchronized MavenArtifact findArtifact(@NotNull String groupId, @NotNull String artifactId, boolean resolve) {
        for (MavenArtifact artifact : cachedArtifacts) {
            if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)) {
                return artifact;
            }
        }
        if (resolve) {
            // Not cached - look in remote repository
            MavenArtifact artifact = new MavenArtifact(this, groupId, artifactId);
            try {
                artifact.loadMetadata(VoidProgressMonitor.INSTANCE);
            } catch (IOException e) {
                log.debug("Artifact [" + artifact + "] not found in repository [" + getUrl() + "]");
                return null;
            }
            cachedArtifacts.add(artifact);
            return artifact;
        }
        return null;
    }

    synchronized void resetArtifactCache(@NotNull String groupId, @NotNull String artifactId) {
        for (Iterator<MavenArtifact> iterator = cachedArtifacts.iterator(); iterator.hasNext(); ) {
            MavenArtifact artifact = iterator.next();
            if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)) {
                iterator.remove();
            }
        }
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

    private static class DependencyResolveInfo {
        String path;
        MavenArtifactDependency.Scope scope;
        boolean optional;

        DependencyResolveInfo(String path, MavenArtifactDependency.Scope scope, boolean optional) {
            this.path = path;
            this.scope = scope;
            this.optional = optional;
        }

        @Override
        public String toString() {
            return path;
        }
    }

    private static class VersionResolveInfo {
        MavenLocalVersion localVersion;
        String parentPath;
        List<DependencyResolveInfo> dependencies = new ArrayList<>();

        public VersionResolveInfo(MavenLocalVersion localVersion, String parentPath) {
            this.localVersion = localVersion;
            this.parentPath = parentPath;
        }

        @Override
        public String toString() {
            return localVersion.toString();
        }
    }

    synchronized void loadCache() {
        File cacheFile = new File(getLocalCacheDir(), METADATA_CACHE_FILE);
        if (!cacheFile.exists()) {
            return;
        }
        final List<VersionResolveInfo> lateResolutions = new ArrayList<>();
        try {
            InputStream mdStream = new FileInputStream(cacheFile);
            try {
                SAXReader reader = new SAXReader(mdStream);
                reader.parse(new SAXListener() {
                    MavenArtifact lastArtifact;
                    VersionResolveInfo lastVersionResolveInfo;
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
                            Date updateTime = new Date();
                            try {
                                updateTime = UPDATE_TIME_FORMAT.parse(atts.getValue(ATTR_UPDATE_TIME));
                            } catch (ParseException e) {
                                // ignore
                            }
                            String versionNumber = atts.getValue(ATTR_VERSION);
                            MavenLocalVersion version = new MavenLocalVersion(
                                lastArtifact,
                                versionNumber,
                                updateTime);
                            lastArtifact.addLocalVersion(version);

                            MavenArtifactVersion lastVersion = new MavenArtifactVersion(version, lastArtifact.getArtifactId(), versionNumber);
                            version.setMetaData(lastVersion);
                            lastVersionResolveInfo = new VersionResolveInfo(version, atts.getValue(ATTR_PARENT));
                            lateResolutions.add(lastVersionResolveInfo);
                        } else if (TAG_DEPENDENCY.equals(localName) && lastVersionResolveInfo != null) {
                            MavenArtifactDependency.Scope scope = MavenArtifactDependency.Scope.COMPILE;
                            String scopeString = atts.getValue(ATTR_SCOPE);
                            if (scopeString != null) {
                                try {
                                    scope = MavenArtifactDependency.Scope.valueOf(scopeString.toUpperCase(Locale.ENGLISH));
                                } catch (IllegalArgumentException e) {
                                    log.debug(e);
                                }
                            }
                            lastVersionResolveInfo.dependencies.add(new DependencyResolveInfo(
                                atts.getValue(ATTR_PATH),
                                scope,
                                CommonUtils.getBoolean(atts.getValue(ATTR_OPTIONAL), false)
                            ));
                        }
                    }
                    @Override
                    public void saxText(SAXReader reader, String data) throws XMLException {
                    }

                    @Override
                    public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
                        if (TAG_ARTIFACT.equals(localName)) {
                            lastArtifact = null;
                        } else if (TAG_VERSION.equals(localName)) {
                            lastVersionResolveInfo = null;
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

        // Perform late resolution
        for (VersionResolveInfo vri : lateResolutions) {
            if (vri.parentPath != null) {
                MavenLocalVersion parentVersion = resolveLocalVersion(vri.parentPath);
                if (parentVersion != null) {
                    vri.localVersion.getMetaData().setParent(parentVersion);
                }
            }
            for (DependencyResolveInfo dri : vri.dependencies) {
                MavenLocalVersion depVersion = resolveLocalVersion(dri.path);
                if (depVersion != null) {
                    vri.localVersion.getMetaData().addDependency(
                        new MavenArtifactDependency(
                            depVersion.getArtifact().getGroupId(),
                            depVersion.getArtifact().getArtifactId(),
                            depVersion.getVersion(),
                            dri.scope,
                            dri.optional));
                }
            }
        }
    }

    private MavenLocalVersion resolveLocalVersion(String path) {
        MavenArtifactReference parentRef = new MavenArtifactReference(path);
        MavenArtifact parentArtifact = MavenRegistry.getInstance().findArtifact(parentRef);
        if (parentArtifact == null) {
            log.warn("Can't resolve artifact " + parentRef);
            return null;
        } else {
            MavenLocalVersion localVersion = parentArtifact.getLocalVersion(parentRef.getVersion());
            if (localVersion == null) {
                log.warn("Can't resolve artifact version " + parentRef);
            }
            return localVersion;
        }
    }

    void saveCacheIfNeeded() {
        if (needsToSave) {
            saveCache();
            needsToSave = false;
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
                try (XMLBuilder.Element e = xml.startElement(TAG_CACHE)) {
                    xml.addAttribute(ATTR_NAME, name);
                    xml.addAttribute(ATTR_URL, url);

                    for (MavenArtifact artifact : cachedArtifacts) {
                        if (CommonUtils.isEmpty(artifact.getLocalVersions())) {
                            continue;
                        }
                        try (XMLBuilder.Element e1 = xml.startElement(TAG_ARTIFACT)) {
                            xml.addAttribute(ATTR_GROUP_ID, artifact.getGroupId());
                            xml.addAttribute(ATTR_ARTIFACT_ID, artifact.getArtifactId());
                            xml.addAttribute(ATTR_ACTIVE_VERSION, artifact.getActiveVersion());
                            for (MavenLocalVersion version : artifact.getLocalVersions()) {
                                try (XMLBuilder.Element e2 = xml.startElement(TAG_VERSION)) {
                                    xml.addAttribute(ATTR_VERSION, version.getVersion());
                                    xml.addAttribute(ATTR_UPDATE_TIME, UPDATE_TIME_FORMAT.format(version.getUpdateTime()));

                                    MavenArtifactVersion metaData = version.getMetaData();
                                    if (metaData != null) {
                                        MavenLocalVersion parentReference = metaData.getParent();
                                        if (parentReference != null) {
                                            xml.addAttribute(ATTR_PARENT, parentReference.getPath());
                                        }
                                        List<MavenArtifactDependency> dependencies = metaData.getDependencies();
                                        if (dependencies != null) {
                                            for (MavenArtifactDependency dependency : dependencies) {
                                                try (XMLBuilder.Element e3 = xml.startElement(TAG_DEPENDENCY)) {
                                                    xml.addAttribute(ATTR_PATH, dependency.getPath());
                                                    if (dependency.getScope() != MavenArtifactDependency.Scope.COMPILE) {
                                                        xml.addAttribute(ATTR_SCOPE, dependency.getScope().name().toLowerCase(Locale.ENGLISH));
                                                    }
                                                    if (dependency.isOptional()) {
                                                        xml.addAttribute(ATTR_OPTIONAL, true);
                                                    }
                                                    List<MavenArtifactReference> exclusions = dependency.getExclusions();
                                                    if (exclusions != null) {
                                                        for (MavenArtifactReference ex : exclusions) {
                                                            try (XMLBuilder.Element e4 = xml.startElement(TAG_EXCLUDE)) {
                                                                xml.addAttribute(ATTR_PATH, ex.getPath());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                xml.flush();
            } finally {
                IOUtils.close(out);
            }
        } catch (IOException e) {
            log.warn("Error saving local Maven cache", e);
        }
    }

    @Override
    public String toString() {
        return url;
    }
}
