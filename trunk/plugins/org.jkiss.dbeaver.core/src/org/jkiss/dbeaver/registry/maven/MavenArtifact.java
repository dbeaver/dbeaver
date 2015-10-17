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
import org.jkiss.dbeaver.registry.maven.versioning.DefaultArtifactVersion;
import org.jkiss.dbeaver.registry.maven.versioning.VersionRange;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maven artifact descriptor
 */
public class MavenArtifact implements IMavenIdentifier
{
    static final Log log = Log.getLog(MavenArtifact.class);

    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    public static final String FILE_JAR = "jar";
    public static final String FILE_POM = "pom";

    @NotNull
    private final MavenRepository repository;
    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;
    @Nullable
    private final String classifier;

    private final List<String> versions = new ArrayList<>();
    private String latestVersion;
    private String releaseVersion;
    private Date lastUpdate;
    private final List<MavenArtifactVersion> localVersions = new ArrayList<>();

    private transient boolean metadataLoaded = false;

    public MavenArtifact(@NotNull MavenRepository repository, @NotNull String groupId, @NotNull String artifactId, @Nullable String classifier)
    {
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
    }

    public void loadMetadata(DBRProgressMonitor monitor) throws IOException {
        latestVersion = null;
        releaseVersion = null;
        versions.clear();
        lastUpdate = null;

        String metadataPath = getBaseArtifactURL() + MAVEN_METADATA_XML;
        monitor.subTask("Load metadata " + this + "");

        try (InputStream mdStream = RuntimeUtils.openConnectionStream(metadataPath)) {
            parseMetadata(mdStream);
        } catch (XMLException e) {
            log.warn("Error parsing artifact metadata", e);
        } catch (IOException e) {
            // Metadata xml not found. It happens in rare cases. Let's try to get directory listing
            try (InputStream dirStream = RuntimeUtils.openConnectionStream(getBaseArtifactURL())) {
                parseDirectory(dirStream);
            } catch (XMLException e1) {
                log.warn("Error parsing artifact directory", e);
            }
        } finally {
            monitor.worked(1);
        }
        metadataLoaded = true;
    }

    private void parseDirectory(InputStream dirStream) throws IOException, XMLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyStream(dirStream, baos, 10000);
        String dir = baos.toString();
        Pattern hrefPattern = Pattern.compile("a href=\"(.+)/?\"");
        Matcher matcher = hrefPattern.matcher(dir);
        while (matcher.find()) {
            String href = matcher.group(1);
            while (href.endsWith("/")) {
                href = href.substring(0, href.length() - 1);
            }
            int divPos = href.lastIndexOf('/');
            if (divPos != -1) {
                href = href.substring(divPos + 1);
            }
            if (href.equals("..")) {
                continue;
            }
            versions.add(href);
        }
    }

    private void parseMetadata(InputStream mdStream) throws IOException, XMLException {
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
    }

    @NotNull
    public MavenRepository getRepository() {
        return repository;
    }

    @NotNull
    public String getGroupId() {
        return groupId;
    }

    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    @Nullable
    public String getClassifier() {
        return classifier;
    }

    @NotNull
    @Override
    public String getVersion() {
        return "";
    }

    @NotNull
    @Override
    public String getId() {
        return MavenArtifactReference.makeId(this);
    }

    @Nullable
    public Collection<String> getAvailableVersion(DBRProgressMonitor monitor) throws IOException {
        if (CommonUtils.isEmpty(versions) && !metadataLoaded) {
            loadMetadata(monitor);
        }
        return versions;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    private String getBaseArtifactURL() {
        String dir = groupId.replace('.', '/') + "/" + artifactId;
        return repository.getUrl() + dir + "/";
    }

    public String getFileURL(String version, String fileType) {
        return getBaseArtifactURL() + version + "/" + getVersionFileName(version, fileType);
    }

    @NotNull
    String getVersionFileName(@NotNull String version, @NotNull String fileType) {
        StringBuilder sb = new StringBuilder();
        sb.append(artifactId).append("-").append(version);
        if (FILE_JAR.equals(fileType) && !CommonUtils.isEmpty(classifier)) {
            sb.append('-').append(classifier);
        }
        sb.append(".").append(fileType);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getId();
    }

//    @Nullable
//    public MavenArtifactVersion getActiveVersion() {
//        return getVersion(activeVersion);
//    }

    @Nullable
    public MavenArtifactVersion getVersion(String versionStr) {
        for (MavenArtifactVersion version : localVersions) {
            if (version.getVersion().equals(versionStr)) {
                return version;
            }
        }
        return null;
    }

    private MavenArtifactVersion makeLocalVersion(DBRProgressMonitor monitor, String versionStr, boolean setActive) throws IllegalArgumentException, IOException {
        MavenArtifactVersion version = getVersion(versionStr);
        if (version == null) {
            version = new MavenArtifactVersion(monitor, this, versionStr);
            localVersions.add(version);
        }
        return version;
    }

    public MavenArtifactVersion resolveVersion(DBRProgressMonitor monitor, String versionRef) throws IOException {
        if (CommonUtils.isEmpty(versionRef)) {
            throw new IOException("Empty artifact " + this + " version");
        }
        char firstChar = versionRef.charAt(0), lastChar = versionRef.charAt(versionRef.length() - 1);
        boolean predefinedVersion =
            versionRef.equals(MavenArtifactReference.VERSION_PATTERN_RELEASE) ||
            versionRef.equals(MavenArtifactReference.VERSION_PATTERN_LATEST) ||
            versionRef.equals(MavenArtifactReference.VERSION_PATTERN_SNAPSHOT);
        boolean lookupVersion =
            firstChar == '[' || firstChar == '(' || firstChar == '{' ||
            lastChar == ']' || lastChar == ')' || lastChar == '}' ||
            versionRef.contains(",") ||
            predefinedVersion;

        if (lookupVersion && !metadataLoaded) {
            loadMetadata(monitor);
        }

        String versionInfo;
        if (lookupVersion) {
            List<String> allVersions = versions;
            switch (versionRef) {
                case MavenArtifactReference.VERSION_PATTERN_RELEASE:
                    versionInfo = releaseVersion;
                    if (!CommonUtils.isEmpty(versionInfo) && isBetaVersion(versionInfo)) {
                        versionInfo = null;
                    }
                    break;
                case MavenArtifactReference.VERSION_PATTERN_LATEST:
                    versionInfo = latestVersion;
                    break;
                default:
                    if (versionRef.startsWith("{") && versionRef.endsWith("}")) {
                        // Regex - find most recent version matching this pattern
                        String regex = versionRef.substring(1, versionRef.length() - 1);
                        try {
                            Pattern versionPattern = Pattern.compile(regex);
                            List<String> versions = new ArrayList<>(allVersions);
                            for (Iterator<String> iter = versions.iterator(); iter.hasNext(); ) {
                                if (!versionPattern.matcher(iter.next()).matches()) {
                                    iter.remove();
                                }
                            }
                            versionInfo = findLatestVersion(versions);
                        } catch (Exception e) {
                            throw new IOException("Bad version pattern: " + regex);
                        }
                    } else {
                        versionInfo = getVersionFromSpec(versionRef);
                    }
                    break;
            }
            if (versionInfo == null) {
                if (allVersions.isEmpty()) {
                    throw new IOException("Artifact '" + this + "' has empty version list");
                }
                // Use latest version
                versionInfo = findLatestVersion(allVersions);
            }
        } else {
            if (versionRef.startsWith("[") || versionRef.startsWith("(")) {
                versionInfo = getVersionFromSpec(versionRef);
            } else {
                versionInfo = versionRef;
            }
        }

        MavenArtifactVersion localVersion = getVersion(versionInfo);
        if (localVersion == null) {
            localVersion = makeLocalVersion(monitor, versionInfo, lookupVersion);
        }

        return localVersion;
    }

    public static boolean versionMatches(String version, String versionSpec) {
        try {
            if (versionSpec.startsWith("{") && versionSpec.endsWith("}")) {
                Pattern versionPattern = Pattern.compile(versionSpec.substring(1, versionSpec.length() - 1));
                return versionPattern.matcher(version).matches();
            } else {
                return VersionRange.createFromVersionSpec(versionSpec).containsVersion(new DefaultArtifactVersion(version));
            }
        } catch (Exception e) {
            log.debug(e);
            return false;
        }
    }

    @Nullable
    private String getVersionFromSpec(String versionRef) throws IOException {
        String versionInfo;
        try {
            VersionRange range = VersionRange.createFromVersionSpec(versionRef);
            if (range.getRecommendedVersion() != null) {
                versionInfo = range.getRecommendedVersion().toString();
            } else if (!range.getRestrictions().isEmpty()) {
                versionInfo = range.getRestrictions().get(0).getLowerBound().toString();
            } else {
                versionInfo = null;
            }
        } catch (Exception e) {
            throw new IOException("Bad version pattern: " + versionRef, e);
        }
        return versionInfo;
    }

    private static boolean isBetaVersion(String versionInfo) {
        return versionInfo.contains("beta") || versionInfo.contains("alpha");
    }

    private static String findLatestVersion(List<String> allVersions) {
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

    public static int compareVersions(String v1, String v2) {
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
