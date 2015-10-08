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
import org.jkiss.dbeaver.registry.driver.DriverLibraryMavenDependency;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Maven artifact version descriptor (POM).
 */
public class MavenArtifactVersion {
    static final Log log = Log.getLog(MavenArtifactVersion.class);

    public static final String PROP_PROJECT_VERSION = "project.version";
    public static final String PROP_PROJECT_GROUP_ID = "project.groupId";
    public static final String PROP_PROJECT_ARTIFACT_ID = "project.artifactId";

    private MavenLocalVersion localVersion;
    private String name;
    private String version;
    private String description;
    private String url;
    private MavenLocalVersion parent;
    private Map<String, String> properties = new LinkedHashMap<>();
    private List<MavenArtifactLicense> licenses = new ArrayList<>();
    private List<MavenArtifactDependency> dependencies;
    private List<MavenArtifactDependency> dependencyManagement;

    private final GeneralUtils.IVariableResolver variableResolver = new GeneralUtils.IVariableResolver() {
        @Override
        public String get(String name) {
            String value = properties.get(name);
            if (value == null) {
                if (name.equals(PROP_PROJECT_VERSION)) {
                    value = version;
                } else if (name.equals(PROP_PROJECT_GROUP_ID)) {
                    value = localVersion.getArtifact().getGroupId();
                } else if (name.equals(PROP_PROJECT_ARTIFACT_ID)) {
                    value = localVersion.getArtifact().getArtifactId();
                } else if (parent != null) {
                    return parent.getMetaData().variableResolver.get(name);
                }
            }
            return value;
        }
    };

    MavenArtifactVersion(DBRProgressMonitor monitor, MavenLocalVersion localVersion) throws IOException {
        this.localVersion = localVersion;
        loadPOM(monitor);
    }

    MavenArtifactVersion(MavenLocalVersion localVersion, String name, String version) {
        this.localVersion = localVersion;
        this.name = name;
        this.version = version;
    }

    public MavenLocalVersion getLocalVersion() {
        return localVersion;
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

    public MavenLocalVersion getParent() {
        return parent;
    }

    void setParent(MavenLocalVersion parent) {
        this.parent = parent;
    }

    void addDependency(MavenArtifactDependency dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        dependencies.add(dependency);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<MavenArtifactLicense> getLicenses() {
        return licenses;
    }

    public List<MavenArtifactDependency> getDependencies(DBRProgressMonitor monitor) {
        if (parent != null) {
            List<MavenArtifactDependency> parentDependencies = parent.getMetaData(monitor).getDependencies(monitor);
            if (!CommonUtils.isEmpty(parentDependencies)) {
                if (CommonUtils.isEmpty(dependencies)) {
                    return parentDependencies;
                }
                parentDependencies = new ArrayList<>(parentDependencies);
                parentDependencies.addAll(dependencies);
                return parentDependencies;
            }
        }
        return this.dependencies;
    }

    List<MavenArtifactDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return localVersion.toString();
    }

    private void loadPOM(DBRProgressMonitor monitor) throws IOException {
        String pomURL = localVersion.getArtifact().getFileURL(localVersion.getVersion(), MavenArtifact.FILE_POM);
        monitor.subTask("Load POM " + localVersion);

        Document pomDocument;
        try (InputStream mdStream = RuntimeUtils.openConnectionStream(pomURL)) {
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
//                    parent = MavenRegistry.getInstance().findArtifactVersion(parentReference);
                    MavenArtifact parentArtifact = MavenRegistry.getInstance().findArtifact(parentReference);
                    if (parentArtifact == null) {
                        log.error("Artifact [" + this + "] parent [" + parentReference + "] not found");
                    } else {
                        parent = parentArtifact.resolveVersion(monitor, parentReference.getVersion(), false);
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

    private List<MavenArtifactDependency> parseDependencies(DBRProgressMonitor monitor, Element element, boolean all) {
        List<MavenArtifactDependency> result = new ArrayList<>();
        Element dependenciesElement = XMLUtils.getChildElement(element, "dependencies");
        if (dependenciesElement != null) {
            for (Element dep : XMLUtils.getChildElementList(dependenciesElement, "dependency")) {
                String groupId = XMLUtils.getChildElementBody(dep, "groupId");
                String artifactId = XMLUtils.getChildElementBody(dep, "artifactId");
                if (groupId == null || artifactId == null) {
                    log.warn("Broken dependency reference: " + groupId + ":" + artifactId);
                    continue;
                }
                String version = XMLUtils.getChildElementBody(dep, "version");
                if (version == null) {
                    version = findDependencyVersion(monitor, groupId, artifactId);
                }
                if (version == null) {
                    log.error("Can't resolve artifact [" + groupId + ":" + artifactId + "] version. Skip.");
                    continue;
                }
                MavenArtifactDependency.Scope scope = MavenArtifactDependency.Scope.COMPILE;
                String scopeName = XMLUtils.getChildElementBody(dep, "scope");
                if (!CommonUtils.isEmpty(scopeName)) {
                    scope = MavenArtifactDependency.Scope.valueOf(scopeName.toUpperCase(Locale.ENGLISH));
                }
                boolean optional = CommonUtils.getBoolean(XMLUtils.getChildElementBody(dep, "optional"), false);

                // TODO: maybe we should include some of them
                if (!all && !optional && scope != MavenArtifactDependency.Scope.COMPILE && scope == MavenArtifactDependency.Scope.RUNTIME) {
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
        return result;
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
        return parent == null ? null : parent.getMetaData(monitor).findDependencyVersion(monitor, groupId, artifactId);
    }

    private String evaluateString(String value) {
        return GeneralUtils.replaceVariables(value, variableResolver);
    }

}