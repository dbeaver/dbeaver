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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Maven artifact version descriptor (POM).
 */
public class MavenArtifactVersion {
    static final Log log = Log.getLog(MavenArtifactVersion.class);

    public static final String PROP_PROJECT_VERSION = "project.version";
    public static final String PROP_PROJECT_GROUP_ID = "project.groupId";
    public static final String PROP_PROJECT_ARTIFACT_ID = "project.artifactId";

    private MavenArtifact artifact;
    private String name;
    private String version;
    private String description;
    private String url;
    private MavenArtifactVersion parent;
    private Map<String, String> properties = new LinkedHashMap<>();
    private List<MavenArtifactLicense> licenses = new ArrayList<>();
    private List<MavenArtifactDependency> dependencies;
    private List<MavenArtifactDependency> dependencyManagement;

    private GeneralUtils.IVariableResolver propertyResolver = new GeneralUtils.IVariableResolver() {
        @Override
        public String get(String name) {
            for (MavenArtifactVersion v = MavenArtifactVersion.this; v != null; v = v.parent) {
                String value = v.properties.get(name);
                if (value != null) {
                    return value;
                } else if (name.equals(PROP_PROJECT_VERSION)) {
                    return v.version;
                } else if (name.equals(PROP_PROJECT_GROUP_ID)) {
                    return v.artifact.getGroupId();
                } else if (name.equals(PROP_PROJECT_ARTIFACT_ID)) {
                    return v.artifact.getArtifactId();
                }
            }
            return null;
        }
    };

    MavenArtifactVersion(DBRProgressMonitor monitor, MavenArtifact artifact, String version) throws IOException {
        this.artifact = artifact;
        this.version = version;
        loadPOM(monitor);
    }

    public MavenArtifact getArtifact() {
        return artifact;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<MavenArtifactLicense> getLicenses() {
        return licenses;
    }

    public List<MavenArtifactDependency> getDependencies(DBRProgressMonitor monitor) {
        if (parent != null) {
            List<MavenArtifactDependency> parentDependencies = parent.getDependencies(monitor);
            if (!CommonUtils.isEmpty(parentDependencies)) {
                if (CommonUtils.isEmpty(dependencies)) {
                    return parentDependencies;
                }
                List<MavenArtifactDependency> result = new ArrayList<>(dependencies.size() + parentDependencies.size());
                result.addAll(dependencies);
                result.addAll(parentDependencies);
                return result;
            }
        }
        return this.dependencies;
    }

    List<MavenArtifactDependency> getDependencies() {
        return dependencies;
    }


    public File getCacheFile() {
        if (artifact.getRepository().isLocal()) {
            String externalURL = getExternalURL(MavenArtifact.FILE_JAR);
            try {
                return new File(new URL(externalURL).toURI());
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
        if (artifact.getRepository().isLocal()) {
            try {
                return new File(new URI(getRemotePOMLocation()));
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

    private void loadPOM(DBRProgressMonitor monitor) throws IOException {
        File localPOM = getLocalPOM();
        if (!localPOM.exists()) {
            cachePOM(localPOM);
        }

        monitor.subTask("Load POM " + this);

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
        description = XMLUtils.getChildElementBody(root, "description");
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
                        parentVersion
                    );
                    if (this.version == null) {
                        this.version = parentReference.getVersion();
                    }
                    parent = MavenRegistry.getInstance().findArtifact(monitor, parentReference);
                    if (parent == null) {
                        log.error("Artifact [" + this + "] parent [" + parentReference + "] not found");
                    }
                }
            }
        }

        {
            // Properties
            Element propsElement = XMLUtils.getChildElement(root, "properties");
            if (propsElement != null) {
                for (Element prop : XMLUtils.getChildElementList(propsElement)) {
                    properties.put(prop.getTagName(), XMLUtils.getElementBody(prop));
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
        {
            // Dependencies
            Element dmElement = XMLUtils.getChildElement(root, "dependencyManagement");
            if (dmElement != null) {
                dependencyManagement = parseDependencies(monitor, dmElement, true);
            }
            dependencies = parseDependencies(monitor, root, false);
        }
        monitor.worked(1);
    }

    private void cachePOM(File localPOM) throws IOException {
        if (artifact.getRepository().isLocal()) {
            return;
        }
        String pomURL = getRemotePOMLocation();
        try (InputStream is = RuntimeUtils.openConnectionStream(pomURL)) {
            File folder = localPOM.getParentFile();
            if (!folder.exists() && !folder.mkdirs()) {
                throw new IOException("Can't create cache folder '" + folder.getAbsolutePath() + "'");
            }

            try (OutputStream os = new FileOutputStream(localPOM)) {
                IOUtils.fastCopy(is, os);
            }
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
                MavenArtifactDependency.Scope scope = MavenArtifactDependency.Scope.COMPILE;
                String scopeName = XMLUtils.getChildElementBody(dep, "scope");
                if (!CommonUtils.isEmpty(scopeName)) {
                    scope = MavenArtifactDependency.Scope.valueOf(scopeName.toUpperCase(Locale.ENGLISH));
                }
                boolean optional = CommonUtils.getBoolean(XMLUtils.getChildElementBody(dep, "optional"), false);

                // TODO: maybe we should include some of them
                if (depManagement || (!optional && includesScope(scope))) {
                    String version = evaluateString(XMLUtils.getChildElementBody(dep, "version"));
                    if (version == null) {
                        version = findDependencyVersion(monitor, groupId, artifactId);
                    }
                    if (version == null) {
                        log.error("Can't resolve artifact [" + groupId + ":" + artifactId + "] version. Skip.");
                        continue;
                    }

                    MavenArtifactDependency dependency = new MavenArtifactDependency(
                        evaluateString(groupId),
                        evaluateString(artifactId),
                        evaluateString(version),
                        scope,
                        optional
                    );
                    result.add(dependency);

                    if (!depManagement) {
                        // Exclusions
                        Element exclusionsElement = XMLUtils.getChildElement(dep, "exclusions");
                        if (exclusionsElement != null) {
                            for (Element exclusion : XMLUtils.getChildElementList(exclusionsElement, "exclusion")) {
                                dependency.addExclusion(
                                    new MavenArtifactReference(
                                        CommonUtils.notEmpty(XMLUtils.getChildElementBody(exclusion, "groupId")),
                                        CommonUtils.notEmpty(XMLUtils.getChildElementBody(exclusion, "artifactId")),
                                        ""));
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

    private String findDependencyVersion(DBRProgressMonitor monitor, String groupId, String artifactId) {
        if (dependencyManagement != null) {
            for (MavenArtifactDependency dmArtifact : dependencyManagement) {
                if (dmArtifact.getGroupId().equals(groupId) &&
                    dmArtifact.getArtifactId().equals(artifactId))
                {
                    return dmArtifact.getVersion();
                }
            }
        }
        return parent == null ? null : parent.findDependencyVersion(monitor, groupId, artifactId);
    }

    private String evaluateString(String value) {
        if (value == null) {
            return null;
        }
        return GeneralUtils.replaceVariables(value, propertyResolver);
    }

}