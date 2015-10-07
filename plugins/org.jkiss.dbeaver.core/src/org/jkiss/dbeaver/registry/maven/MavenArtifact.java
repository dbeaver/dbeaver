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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Maven artifact descriptor
 */
public class MavenArtifact
{
    static final Log log = Log.getLog(MavenArtifact.class);

    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    public static final String FILE_JAR = "jar";
    public static final String FILE_POM = "pom";

    private final MavenRepository repository;
    private final String groupId;
    private final String artifactId;
    private List<String> versions = new ArrayList<String>();
    private String latestVersion;
    private String releaseVersion;
    private Date lastUpdate;

    private List<MavenLocalVersion> localVersions = new ArrayList<MavenLocalVersion>();
    private String activeVersion;

    public MavenArtifact(MavenRepository repository, String groupId, String artifactId)
    {
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public void loadMetadata() throws IOException {
        latestVersion = null;
        releaseVersion = null;
        versions.clear();
        lastUpdate = null;

        String metadataPath = getArtifactDir() + MAVEN_METADATA_XML;
        try (InputStream mdStream = RuntimeUtils.openConnectionStream(metadataPath)) {
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

    public List<MavenLocalVersion> getLocalVersions() {
        return localVersions;
    }

    public String getActiveVersion() {
        return activeVersion;
    }

    public void setActiveVersion(String activeVersion) {
        this.activeVersion = activeVersion;
    }

    private String getArtifactDir() {
        String dir = (groupId + "/" + artifactId).replace('.', '/');
        return repository.getUrl() + dir + "/";
    }

    public String getFileURL(String version, String fileType) {
        return getArtifactDir() + version + "/" + getVersionFileName(version, fileType);
    }

    @NotNull
    private String getVersionFileName(String version, String fileType) {
        return artifactId + "-" + version + "." + fileType;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId;
    }

    @Nullable
    public MavenLocalVersion getActiveLocalVersion() {
        return getLocalVersion(activeVersion);
    }

    @Nullable
    public MavenLocalVersion getLocalVersion(String versionStr) {
        if (CommonUtils.isEmpty(activeVersion)) {
            return null;
        }
        for (MavenLocalVersion version : localVersions) {
            if (version.getVersion().equals(versionStr)) {
                return version;
            }
        }
        return null;
    }

    public MavenLocalVersion makeLocalVersion(DBRProgressMonitor monitor, String versionStr, boolean setActive) throws IllegalArgumentException {
        MavenLocalVersion version = getLocalVersion(versionStr);
        if (version == null) {
            if (!versions.contains(versionStr)) {
                // No version info. Some artifacts do not have older versions in metadata.xml so just warn
                log.debug("Artifact '" + artifactId + "' do not have version '" + versionStr + "' info in metadata");
            }
            version = new MavenLocalVersion(this, versionStr, getVersionFileName(versionStr, FILE_JAR), new Date());
            version.getMetaData(monitor);
            localVersions.add(version);
        }
        if (setActive) {
            activeVersion = versionStr;
        }
        return version;
    }

    void addLocalVersion(MavenLocalVersion version) {
        localVersions.add(version);
    }

    private void removeLocalVersion(MavenLocalVersion version) {
        localVersions.remove(version);
    }

    public MavenLocalVersion resolveVersion(DBRProgressMonitor monitor, String versionRef) throws IOException {
        monitor.beginTask("Download Maven artifact '" + this + "'", 3);
        try {
            monitor.subTask("Download metadata from " + repository.getUrl());
            loadMetadata();
            monitor.worked(1);

            String versionInfo = versionRef;
            List<String> allVersions = versions;
            if (versionInfo.equals(MavenArtifactReference.VERSION_PATTERN_RELEASE)) {
                versionInfo = releaseVersion;
                if (!CommonUtils.isEmpty(versionInfo) && isBetaVersion(versionInfo)) {
                    versionInfo = null;
                }
            } else if (versionInfo.equals(MavenArtifactReference.VERSION_PATTERN_LATEST)) {
                versionInfo = latestVersion;
            } else {
                if (versionInfo.startsWith("[") && versionInfo.endsWith("]")) {
                    // Regex - find most recent version matching this pattern
                    String regex = versionInfo.substring(1, versionInfo.length() - 1);
                    try {
                        Pattern versionPattern = Pattern.compile(regex);
                        List<String> versions = new ArrayList<String>(allVersions);
                        for (Iterator<String> iter = versions.iterator(); iter.hasNext(); ) {
                            if (!versionPattern.matcher(iter.next()).matches()) {
                                iter.remove();
                            }
                        }
                        versionInfo = findLatestVersion(versions);
                    } catch (Exception e) {
                        throw new IOException("Bad version pattern: " + regex);
                    }
                }
            }
            if (CommonUtils.isEmpty(versionInfo)) {
                if (allVersions.isEmpty()) {
                    throw new IOException("Artifact '" + this + "' has empty version list");
                }
                // Use latest version
                versionInfo = findLatestVersion(allVersions);
            }
            monitor.subTask("Download binaries for version " + versionInfo);
            MavenLocalVersion localVersion = getActiveLocalVersion();
            if (localVersion != null && !localVersion.getCacheFile().exists()) {
                log.debug("Drop previous local version '" + localVersion + "' - jar seems to be dropped");
                removeLocalVersion(localVersion);
                localVersion = null;
            }
            if (localVersion == null) {
                localVersion = makeLocalVersion(monitor, versionInfo, true);
            }
            monitor.worked(1);
            monitor.subTask("Save repository cache");
            repository.flushCache();
            monitor.worked(1);

            return localVersion;
        } finally {
            monitor.done();
        }
    }

    private static boolean isBetaVersion(String versionInfo) {
        return versionInfo.contains("beta") || versionInfo.contains("alpha");
    }

    private String findLatestVersion(List<String> allVersions) {
        String latest = null;
        for (String version : allVersions) {
            if (isBetaVersion(version)) {
                continue;
            }
            if (latest == null || compareVersions(version, latest) > 0) {
                latest = version;
            }
        }
        return latest;
    }

    private int compareVersions(String v1, String v2) {
        StringTokenizer st1 = new StringTokenizer(v1, ".-_");
        StringTokenizer st2 = new StringTokenizer(v2, ".-_");
        while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
            String t1 = st1.nextToken();
            String t2 = st2.nextToken();
            try {
                int cmp = Integer.parseInt(t1) - Integer.parseInt(t2);
                if (cmp != 0) {
                    return cmp;
                }
            } catch (NumberFormatException e) {
                // Non-numeric versions - use lexicographical compare
                int cmp = t1.compareTo(t2);
                if (cmp != 0) {
                    return cmp;
                }
            }
        }
        if (st1.hasMoreTokens()) {
            return 1;
        } else if (st2.hasMoreTokens()) {
            return -1;
        } else {
            return 0;
        }
    }


}
