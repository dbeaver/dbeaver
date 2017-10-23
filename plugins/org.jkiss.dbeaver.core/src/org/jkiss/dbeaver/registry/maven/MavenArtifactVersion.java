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
package org.jkiss.dbeaver.registry.maven;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Maven artifact version descriptor (POM).
 */
public class MavenArtifactVersion implements IMavenIdentifier {
    private static final Log log = Log.getLog(MavenArtifactVersion.class);

    public static final String PROP_PROJECT_VERSION = "project.version";
    public static final String PROP_PROJECT_GROUP_ID = "project.groupId";
    public static final String PROP_PROJECT_ARTIFACT_ID = "project.artifactId";
    private static final String DEFAULT_PROFILE_ID = "#root";

    private MavenArtifact artifact;
    private String name;
    private String version;
    private String packaging;
    private String description;
    private String url;
    private MavenArtifactVersion parent;
    private List<MavenArtifactVersion> imports;
    private final List<MavenArtifactLicense> licenses = new ArrayList<>();
    private final List<MavenProfile> profiles = new ArrayList<>();

    private GeneralUtils.IVariableResolver propertyResolver = new GeneralUtils.IVariableResolver() {
        @Override
        public String get(String name) {
            switch (name) {
                case PROP_PROJECT_VERSION:
                    return version;
                case PROP_PROJECT_GROUP_ID:
                    return artifact.getGroupId();
                case PROP_PROJECT_ARTIFACT_ID:
                    return artifact.getArtifactId();
            }
            for (MavenArtifactVersion v = MavenArtifactVersion.this; v != null; v = v.parent) {
                for (MavenProfile profile : v.profiles) {
                    if (!profile.isActive()) {
                        continue;
                    }
                    String value = profile.properties.get(name);
                    if (value != null) {
                        return value;
                    }
                }
            }
            return null;
        }
    };

    MavenArtifactVersion(@NotNull DBRProgressMonitor monitor, @NotNull MavenArtifact artifact, @NotNull String version) throws IOException {
        this.artifact = artifact;
        this.version = version;

        try {
            loadPOM(monitor);
        } catch (IOException e) {
            log.error("Error loading artifact version POM: " + e.getMessage());
        }
    }

    @NotNull
    public MavenArtifact getArtifact() {
        return artifact;
    }

    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getGroupId() {
        return artifact.getGroupId();
    }

    @NotNull
    @Override
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    @Nullable
    @Override
    public String getClassifier() {
        return artifact.getClassifier();
    }

    @NotNull
    @Override
    public String getVersion() {
        return version;
    }

    @NotNull
    @Override
    public String getId() {
        return MavenArtifactReference.makeId(this);
    }

    @Nullable
    public String getPackaging() {
        return packaging;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public MavenArtifactVersion getParent() {
        return parent;
    }

    public List<MavenArtifactLicense> getLicenses() {
        return licenses;
    }

    public List<MavenProfile> getProfiles() {
        return profiles;
    }

    public List<MavenArtifactDependency> getDependencies() {
        List<MavenArtifactDependency> dependencies = new ArrayList<>();
        for (MavenProfile profile : profiles) {
            if (profile.isActive() && !CommonUtils.isEmpty(profile.dependencies)) {
                dependencies.addAll(profile.dependencies);
            }
        }
        if (parent != null) {
            List<MavenArtifactDependency> parentDependencies = parent.getDependencies();
            if (!CommonUtils.isEmpty(parentDependencies)) {
                dependencies.addAll(parentDependencies);
            }
        }
        return dependencies;
    }

    public File getCacheFile() {
        if (artifact.getRepository().getType() == MavenRepository.RepositoryType.LOCAL) {
            String externalURL = getExternalURL(MavenArtifact.FILE_JAR);
            try {
                return RuntimeUtils.getLocalFileFromURL(new URL(externalURL));
//                return new File(new URL(externalURL).toURI());
            } catch (Exception e) {
                log.warn("Bad repository URL", e);
                return new File(externalURL);
            }
        }
        return new File(artifact.getRepository().getLocalCacheDir(), artifact.getGroupId() + "/" + artifact.getVersionFileName(version, MavenArtifact.FILE_JAR));
    }

    public String getExternalURL(String fileType) {
        return artifact.getFileURL(version, fileType);
    }

    public String getPath() {
        return artifact.toString() + ":" + version;
    }

    @Override
    public String toString() {
        return getPath();
    }

    private File getLocalPOM() {
        if (artifact.getRepository().getType() == MavenRepository.RepositoryType.LOCAL) {
            try {
                return new File(GeneralUtils.makeURIFromFilePath(getRemotePOMLocation()));
            } catch (URISyntaxException e) {
                log.warn(e);
            }
        }
        return new File(
            artifact.getRepository().getLocalCacheDir(),
            artifact.getGroupId() + "/" + artifact.getVersionFileName(version, MavenArtifact.FILE_POM));
    }

    private String getRemotePOMLocation() {
        return artifact.getFileURL(version, MavenArtifact.FILE_POM);
    }

    private void cachePOM(File localPOM) throws IOException {
        if (artifact.getRepository().getType() == MavenRepository.RepositoryType.LOCAL) {
            return;
        }
        String pomURL = getRemotePOMLocation();
        try (InputStream is = WebUtils.openConnection(pomURL, artifact.getRepository().getAuthInfo()).getInputStream()) {
            File folder = localPOM.getParentFile();
            if (!folder.exists() && !folder.mkdirs()) {
                throw new IOException("Can't create cache folder '" + folder.getAbsolutePath() + "'");
            }

            try (OutputStream os = new FileOutputStream(localPOM)) {
                IOUtils.fastCopy(is, os);
            }
        }
    }

    private void loadPOM(DBRProgressMonitor monitor) throws IOException {
        monitor.subTask("Load POM " + this);

        File localPOM = getLocalPOM();
        if (!localPOM.exists()) {
            cachePOM(localPOM);
        }


        Document pomDocument;
        try (InputStream mdStream = new FileInputStream(localPOM)) {
            pomDocument = XMLUtils.parseDocument(mdStream);
        } catch (XMLException e) {
            throw new IOException("Error parsing POM", e);
        }
        Element root = pomDocument.getDocumentElement();
        name = XMLUtils.getChildElementBody(root, "name");
        url = XMLUtils.getChildElementBody(root, "url");
        version = XMLUtils.getChildElementBody(root, "version");
        packaging = XMLUtils.getChildElementBody(root, "packaging");
        description = XMLUtils.getChildElementBody(root, "description");
        if (description != null) {
            description = TextUtils.compactWhiteSpaces(description.trim());
        }
        {
            // Parent
            Element parentElement = XMLUtils.getChildElement(root, "parent");
            if (parentElement != null) {
                String parentGroupId = XMLUtils.getChildElementBody(parentElement, "groupId");
                String parentArtifactId = XMLUtils.getChildElementBody(parentElement, "artifactId");
                String parentVersion = XMLUtils.getChildElementBody(parentElement, "version");
                if (parentGroupId == null || parentArtifactId == null || parentVersion == null) {
                    log.error("Broken parent reference: " + parentGroupId + ":" + parentArtifactId + ":" + parentVersion);
                } else {
                    MavenArtifactReference parentReference = new MavenArtifactReference(
                        parentGroupId,
                        parentArtifactId,
                        null,
                        parentVersion);
                    if (this.version == null) {
                        this.version = parentReference.getVersion();
                    }
                    parent = MavenRegistry.getInstance().findArtifact(monitor, this, parentReference);
                    if (parent == null) {
                        log.error("Artifact [" + this + "] parent [" + parentReference + "] not found");
                    }
                }
            }
        }

        {
            // Licenses
            Element licensesElement = XMLUtils.getChildElement(root, "licenses");
            if (licensesElement != null) {
                for (Element prop : XMLUtils.getChildElementList(licensesElement, "license")) {
                    licenses.add(new MavenArtifactLicense(
                        XMLUtils.getChildElementBody(prop, "name"),
                        XMLUtils.getChildElementBody(prop, "url")
                    ));
                }
            }
        }

        // Default profile
        MavenProfile defaultProfile = new MavenProfile(DEFAULT_PROFILE_ID);
        defaultProfile.active = true;
        profiles.add(defaultProfile);
        parseProfile(monitor, defaultProfile, root);

        {
            // Profiles
            Element licensesElement = XMLUtils.getChildElement(root, "profiles");
            if (licensesElement != null) {
                for (Element profElement : XMLUtils.getChildElementList(licensesElement, "profile")) {
                    MavenProfile profile = new MavenProfile(XMLUtils.getChildElementBody(profElement, "id"));
                    profiles.add(profile);
                    parseProfile(monitor, profile, profElement);
                }
            }
        }

        monitor.worked(1);
    }

    private void parseProfile(DBRProgressMonitor monitor, MavenProfile profile, Element element) {
        {
            // Activation
            Element activationElement = XMLUtils.getChildElement(element, "activation");
            if (activationElement != null) {
                String activeByDefault = XMLUtils.getChildElementBody(activationElement, "activeByDefault");
                if (!CommonUtils.isEmpty(activeByDefault)) {
                    profile.active = CommonUtils.getBoolean(activeByDefault);
                }
                String jdk = XMLUtils.getChildElementBody(activationElement, "jdk");
                if (!CommonUtils.isEmpty(jdk)) {
                    profile.active = MavenArtifact.versionMatches(System.getProperty(StandardConstants.ENV_JAVA_VERSION), jdk);
                }
                Element osElement = XMLUtils.getChildElement(activationElement, "os");
                if (osElement != null) {

                }
                Element propElement = XMLUtils.getChildElement(activationElement, "property");
                if (propElement != null) {
                    String propName = XMLUtils.getChildElementBody(propElement, "name");
                    //String propValue = XMLUtils.getChildElementBody(propElement, "value");
                    // TODO: implement real properties checks. Now enable all profiles with !prop
                    if (propName != null && propName.startsWith("!")) {
                        profile.active = true;
                    }
                }
            }
        }
        if (!profile.active) {
            // Do not parse dependencies of non-active profiles (most likely they will fail).
            return;
        }
        {
            // Properties
            Element propsElement = XMLUtils.getChildElement(element, "properties");
            if (propsElement != null) {
                for (Element prop : XMLUtils.getChildElementList(propsElement)) {
                    profile.properties.put(prop.getTagName(), XMLUtils.getElementBody(prop));
                }
            }
        }
        {
            // Repositories
            Element repsElement = XMLUtils.getChildElement(element, "repositories");
            if (repsElement != null) {
                for (Element repElement : XMLUtils.getChildElementList(repsElement, "repository")) {
                    MavenRepository repository = new MavenRepository(
                        XMLUtils.getChildElementBody(repElement, "id"),
                        XMLUtils.getChildElementBody(repElement, "name"),
                        XMLUtils.getChildElementBody(repElement, "url"),
                        MavenRepository.RepositoryType.EXTERNAL);
                    String layout = XMLUtils.getChildElementBody(repElement, "layout");
                    if ("legacy".equals(layout)) {
                        log.debug("Skip legacy repository [" + repository + "]");
                        continue;
                    }
                    Element releasesElement = XMLUtils.getChildElement(repElement, "releases");
                    if (releasesElement == null) {
                        continue;
                    }
                    boolean enabled = CommonUtils.toBoolean(XMLUtils.getChildElementBody(releasesElement, "enabled"));
                    if (enabled) {
                        profile.addRepository(repository);
                    }
                }
            }
        }
        {
            // Dependencies
            Element dmElement = XMLUtils.getChildElement(element, "dependencyManagement");
            if (dmElement != null) {
                profile.dependencyManagement = parseDependencies(monitor, dmElement, true);
            }
            profile.dependencies = parseDependencies(monitor, element, false);
        }
    }

    private List<MavenArtifactDependency> parseDependencies(DBRProgressMonitor monitor, Element element, boolean depManagement) {
        List<MavenArtifactDependency> result = new ArrayList<>();
        Element dependenciesElement = XMLUtils.getChildElement(element, "dependencies");
        if (dependenciesElement != null) {
            for (Element dep : XMLUtils.getChildElementList(dependenciesElement, "dependency")) {
                String groupId = evaluateString(XMLUtils.getChildElementBody(dep, "groupId"));
                String artifactId = evaluateString(XMLUtils.getChildElementBody(dep, "artifactId"));
                if (groupId == null || artifactId == null) {
                    log.warn("Broken dependency reference: " + groupId + ":" + artifactId);
                    continue;
                }
                String classifier = evaluateString(XMLUtils.getChildElementBody(dep, "classifier"));

                MavenArtifactDependency dmInfo = depManagement ? null : findDependencyManagement(groupId, artifactId);

                // Resolve scope
                MavenArtifactDependency.Scope scope = null;
                String scopeName = XMLUtils.getChildElementBody(dep, "scope");
                if (!CommonUtils.isEmpty(scopeName)) {
                    scope = MavenArtifactDependency.Scope.valueOf(scopeName.toUpperCase(Locale.ENGLISH));
                }
                if (scope == null && dmInfo != null) {
                    scope = dmInfo.getScope();
                }
                if (scope == null) {
                    scope = MavenArtifactDependency.Scope.COMPILE;
                }

                String optionalString = XMLUtils.getChildElementBody(dep, "optional");
                boolean optional = optionalString == null ?
                    (dmInfo != null && dmInfo.isOptional()) :
                    CommonUtils.getBoolean(optionalString);

                // Resolve version
                String version = evaluateString(XMLUtils.getChildElementBody(dep, "version"));

                if (depManagement && scope == MavenArtifactDependency.Scope.IMPORT) {
                    // Import another pom
                    if (version == null) {
                        log.error("Missing imported artifact [" + groupId + ":" + artifactId + "] version. Skip.");
                        continue;
                    }
                    MavenArtifactReference importReference = new MavenArtifactReference(
                        groupId,
                        artifactId,
                        classifier,
                        version);
                    MavenArtifactVersion importedVersion = MavenRegistry.getInstance().findArtifact(monitor, this, importReference);
                    if (importedVersion == null) {
                        log.error("Imported artifact [" + importReference + "] not found. Skip.");
                    }
                    if (imports == null) {
                        imports = new ArrayList<>();
                    }
                    imports.add(importedVersion);
                } else if (depManagement || (!optional && includesScope(scope))) {
                    // TODO: maybe we should include optional or PROVIDED

                    if (version == null && dmInfo != null) {
                        version = dmInfo.getVersion();
                    }
                    if (version == null) {
                        log.error("Can't resolve artifact [" + groupId + ":" + artifactId + "] version. Skip.");
                        continue;
                    }

                    MavenArtifactDependency dependency = new MavenArtifactDependency(
                        groupId,
                        artifactId,
                        classifier,
                        version,
                        scope,
                        optional);
                    result.add(dependency);

                    // Exclusions
                    Element exclusionsElement = XMLUtils.getChildElement(dep, "exclusions");
                    if (exclusionsElement != null) {
                        for (Element exclusion : XMLUtils.getChildElementList(exclusionsElement, "exclusion")) {
                            dependency.addExclusion(
                                new MavenArtifactReference(
                                    CommonUtils.notEmpty(XMLUtils.getChildElementBody(exclusion, "groupId")),
                                    CommonUtils.notEmpty(XMLUtils.getChildElementBody(exclusion, "artifactId")),
                                    null,
                                    ""));
                        }
                    }
                    if (dmInfo != null) {
                        List<MavenArtifactReference> dmExclusions = dmInfo.getExclusions();
                        if (dmExclusions != null) {
                            for (MavenArtifactReference dmEx : dmExclusions) {
                                dependency.addExclusion(dmEx);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean includesScope(MavenArtifactDependency.Scope scope) {
        return
            scope == MavenArtifactDependency.Scope.COMPILE ||
            scope == MavenArtifactDependency.Scope.RUNTIME/* ||
            scope == MavenArtifactDependency.Scope.PROVIDED*/;
    }

    private MavenArtifactDependency findDependencyManagement(String groupId, String artifactId) {
        for (MavenProfile profile : profiles) {
            if (profile.isActive() && profile.dependencyManagement != null) {
                for (MavenArtifactDependency dmArtifact : profile.dependencyManagement) {
                    if (dmArtifact.getGroupId().equals(groupId) &&
                        dmArtifact.getArtifactId().equals(artifactId)) {
                        return dmArtifact;
                    }
                }
            }
        }
        // Check in imported BOMs
        if (imports != null) {
            for (MavenArtifactVersion i : imports) {
                MavenArtifactDependency dependencyManagement = i.findDependencyManagement(groupId, artifactId);
                if (dependencyManagement != null) {
                    return dependencyManagement;
                }
            }
        }
        return parent == null ? null : parent.findDependencyManagement(groupId, artifactId);
    }

    private String evaluateString(String value) {
        if (value == null) {
            return null;
        }
        return GeneralUtils.replaceVariables(value, propertyResolver);
    }

    @NotNull
    public Collection<MavenRepository> getActiveRepositories() {
        Map<String, MavenRepository> repositories = new LinkedHashMap<>();
        for (MavenArtifactVersion v = MavenArtifactVersion.this; v != null; v = v.parent) {
            for (MavenProfile profile : v.profiles) {
                if (profile.isActive()) {
                    List<MavenRepository> pr = profile.getRepositories();
                    if (pr != null) {
                        for (MavenRepository repository : pr) {
                            repositories.put(repository.getId(), repository);
                        }
                    }
                }
            }
        }
        return repositories.values();
    }
}