/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.maven;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.maven.versioning.DefaultArtifactVersion;
import org.jkiss.dbeaver.registry.maven.versioning.VersionRange;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.utils.VersionUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maven artifact descriptor
 */
public class MavenArtifact implements IMavenIdentifier
{
    private static final Log log = Log.getLog(MavenArtifact.class);

    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    public static final String FILE_JAR = "jar";
    public static final String FILE_POM = "pom";
    public static final String PACKAGING_BUNDLE = "bundle";
    public static final String PACKAGING_MAVEN_PLUGIN = "maven-plugin";

    @NotNull
    private final MavenRepository repository;
    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;
    @Nullable
    private final String classifier;
    @Nullable
    private final String fallbackVersion;

    private final List<String> versions = new ArrayList<>();
    private String latestVersion;
    private String releaseVersion;
    private Date lastUpdate;
    private final List<String> snapshotVersions = new ArrayList<>();
    private final List<MavenArtifactVersion> localVersions = new ArrayList<>();

    private transient boolean metadataLoaded = false;

    public MavenArtifact(
        @NotNull MavenRepository repository,
        @NotNull String groupId,
        @NotNull String artifactId,
        @Nullable String classifier,
        @Nullable String fallbackVersion)
    {
        this.repository = repository;
        this.groupId = CommonUtils.trim(groupId);
        this.artifactId = CommonUtils.trim(artifactId);
        this.classifier = CommonUtils.trim(classifier);
        this.fallbackVersion = CommonUtils.trim(fallbackVersion);
    }


    public void loadMetadata(DBRProgressMonitor monitor) throws IOException {
        loadMetadata(monitor, null);
    }

    public void loadMetadata(DBRProgressMonitor monitor, String version) throws IOException {
        latestVersion = null;
        releaseVersion = null;
        versions.clear();
        snapshotVersions.clear();
        lastUpdate = null;
        String metadataPath = getBaseArtifactURL();
        // The repository is a SNAPSHOT repo, artifact metadata is stored inside the version
        if (version != null) {
            metadataPath += version + "/";
        }
        metadataPath += MAVEN_METADATA_XML;
        monitor.subTask("Load metadata " + this);

        try (InputStream mdStream = WebUtils.openConnection(monitor, metadataPath, getRepository().getAuthInfo(), null).getInputStream()) {
            parseMetadata(mdStream);
        } catch (XMLException e) {
            log.warn("Error parsing artifact metadata", e);
        } catch (IOException e) {
            // Metadata xml not found. It happens in rare cases. Let's try to get directory listing
            try (InputStream dirStream = WebUtils.openConnection(monitor, getBaseArtifactURL(), getRepository().getAuthInfo(), null).getInputStream()) {
                parseDirectory(dirStream);
            } catch (XMLException e1) {
                log.warn("Error parsing artifact directory", e);
            }
        } finally {
            removeIgnoredVersions();
            monitor.worked(1);
        }
        metadataLoaded = true;
    }

    private void removeIgnoredVersions() {
        versions.removeIf(version ->
            MavenRegistry.getInstance().isVersionIgnored(groupId + ":" + artifactId + ":" + version));
    }

    private void parseDirectory(InputStream dirStream) throws IOException, XMLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyStream(dirStream, baos);
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
            public boolean insideSnapshotVersion = false;
            public String currentSnapshotVersion;
            boolean invalid = false;

            @Override
            public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
                if ("snapshotVersion".equals(localName)) {
                    insideSnapshotVersion = true;
                }
                lastTag = localName;
            }

            @Override
            public void saxText(SAXReader reader, String data) throws XMLException {
                if (insideSnapshotVersion) {
                    if ("value".equals(lastTag)) {
                        currentSnapshotVersion = data;
                    } else if ("classifier".equals(lastTag)
                        && !data.equals(classifier)) {
                        invalid = true;
                    } else if ("extension".equals(lastTag) && !"jar".equals(data)) {
                        invalid = true;
                    }
                } else {
                    if ("version".equals(lastTag)) {
                        versions.add(data);
                    } else if ("snapshotVersion".equals(lastTag)) {
                        insideSnapshotVersion = true;
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
            }

            @Override
            public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
                if (localName.equals("snapshotVersion")) {
                    if (!invalid) {
                        snapshotVersions.add(currentSnapshotVersion);
                    }
                    currentSnapshotVersion = null;
                    insideSnapshotVersion = false;
                    invalid = false;
                }
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
    @Override
    public String getClassifier() {
        return classifier;
    }

    @Nullable
    public String getFallbackVersion() {
        return fallbackVersion;
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
    public Collection<String> getAvailableVersions(DBRProgressMonitor monitor, String versionSpec) throws IOException {
        if (CommonUtils.isEmpty(versions) && !metadataLoaded) {
            loadMetadata(monitor, null);
        }
        if (!isVersionPattern(versionSpec)) {
            return versions;
        }
        // Filter versions according to spec
        Pattern versionPattern = null;
        VersionRange versionRange = null;
        if (versionSpec.startsWith("{") && versionSpec.endsWith("}")) {
            // Regex - find most recent version matching this pattern
            try {
                versionPattern = Pattern.compile(versionSpec.substring(1, versionSpec.length() - 1));
            } catch (Exception e) {
                log.error("Bad version pattern: " + versionSpec);
            }
        } else {
            try {
                versionRange = VersionRange.createFromVersionSpec(versionSpec);
            } catch (Exception e) {
                log.error("Bad version specification: " + versionSpec);
            }
        }

        List<String> filtered = new ArrayList<>();
        for (String version : versions) {
            boolean matches;
            if (versionPattern != null) {
                matches = versionPattern.matcher(version).matches();
            } else if (versionRange != null) {
                matches = versionRange.containsVersion(new DefaultArtifactVersion(version));
            } else {
                matches = true;
            }
            if (matches) {
                filtered.add(version);
            }
        }
        return filtered;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    private String getBaseArtifactURL() {
        String dir = groupId.replace('.', '/') + "/" + artifactId;
        return repository.getUrl() + dir + "/";
    }

    String getFileURL(String version, String fileType, boolean snapshotVersion) {
        if (snapshotVersion) {
            return getBaseArtifactURL() + versions.get(0) + "/" + getVersionFileName(
                VersionUtils.findLatestVersion(snapshotVersions),
                fileType
            );
        }

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

    @Nullable
    public MavenArtifactVersion getVersion(String versionStr) {
        for (MavenArtifactVersion version : localVersions) {
            if (CommonUtils.equalObjects(version.getVersion(), versionStr)) {
                return version;
            }
        }
        return null;
    }

    private MavenArtifactVersion makeLocalVersion(
        DBRProgressMonitor monitor,
        String versionStr,
        boolean setActive,
        boolean resolveOptionalDependencies,
        boolean snapshotVersion
    ) throws IllegalArgumentException, IOException {
        MavenArtifactVersion version = getVersion(versionStr);
        if (version == null) {
            version = new MavenArtifactVersion(monitor, this, versionStr, resolveOptionalDependencies, snapshotVersion);
            localVersions.add(version);
        }
        return version;
    }

    public MavenArtifactVersion resolveVersion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String versionRef,
        boolean resolveOptionalDependencies) throws IOException
    {
        if (CommonUtils.isEmpty(versionRef)) {
            throw new IOException("Empty artifact " + this + " version");
        }
        boolean predefinedVersion = versionRef.equals(MavenArtifactReference.VERSION_PATTERN_RELEASE)
            || versionRef.equals(MavenArtifactReference.VERSION_PATTERN_LATEST) || (
            versionRef.equals(MavenArtifactReference.VERSION_PATTERN_SNAPSHOT) && !getRepository().isSnapshot());
        boolean snapshotVersion = repository.isSnapshot() && versionRef.contains(MavenArtifactReference.VERSION_PATTERN_SNAPSHOT);
        if (snapshotVersion) {
            loadMetadata(monitor, versionRef);
        }
        boolean lookupVersion = predefinedVersion || isVersionPattern(versionRef) || snapshotVersion;
        if (lookupVersion && !metadataLoaded) {
            loadMetadata(monitor);
        }

        String versionInfo;
        if (lookupVersion) {
            List<String> allVersions = versions;

            switch (versionRef) {
                case MavenArtifactReference.VERSION_PATTERN_RELEASE:
                    versionInfo = releaseVersion;
                    if (!CommonUtils.isEmpty(versionInfo) && VersionUtils.isBetaVersion(versionInfo)) {
                        versionInfo = null;
                    }
                    break;
                case MavenArtifactReference.VERSION_PATTERN_LATEST:
                    versionInfo = latestVersion;
                    break;
                default:
                    if (snapshotVersion) {
                        if (snapshotVersions.isEmpty()) {
                            throw new IOException("Artifact '" + this + "' has empty snapshot version list");
                        }
                        versionInfo = VersionUtils.findLatestVersion(snapshotVersions);
                        break;
                    }
                    if (versionRef.startsWith("{") && versionRef.endsWith("}")) {
                        // Regex - find most recent version matching this pattern
                        String regex = versionRef.substring(1, versionRef.length() - 1);
                    try {
                            Pattern versionPattern = Pattern.compile(regex);
                            List<String> versions = new ArrayList<>(allVersions);
                            versions.removeIf(s -> !versionPattern.matcher(s).matches());
                            versionInfo = VersionUtils.findLatestVersion(versions);
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
                versionInfo = VersionUtils.findLatestVersion(allVersions);
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
            try {
                localVersion = makeLocalVersion(monitor, versionInfo, lookupVersion, resolveOptionalDependencies, snapshotVersion);
            } catch (IOException e) {
                // Some IO error - not fatal
                log.debug("Error loading version info: " + e.getMessage());
            }
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

    private static boolean isVersionPattern(String versionSpec) {
        if (versionSpec.isEmpty()) {
            return false;
        }
        char firstChar = versionSpec.charAt(0), lastChar = versionSpec.charAt(versionSpec.length() - 1);
        return firstChar == '[' || firstChar == '(' || firstChar == '{' ||
            lastChar == ']' || lastChar == ')' || lastChar == '}' ||
            versionSpec.contains(",");
    }
}
