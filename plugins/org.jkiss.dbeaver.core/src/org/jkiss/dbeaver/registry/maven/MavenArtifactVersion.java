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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maven artifact version descriptor (POM).
 */
public class MavenArtifactVersion {
    static final Log log = Log.getLog(MavenArtifactVersion.class);

    public static final String PROP_PROJECT_VERSION = "project.version";

    private MavenLocalVersion localVersion;
    private String name;
    private String version;
    private String description;
    private String url;
    private MavenLocalVersion parent;
    private MavenArtifactReference parentReference;
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
                } else if (parent != null) {
                    return parent.getMetaData(VoidProgressMonitor.INSTANCE).variableResolver.get(name);
                }
            }
            return value;
        }
    };

    MavenArtifactVersion(DBRProgressMonitor monitor, MavenLocalVersion localVersion) throws IOException {
        this.localVersion = localVersion;
        loadPOM(monitor);
    }

    MavenArtifactVersion(String name, String version) {
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

    public MavenArtifactReference getParentReference() {
        return parentReference;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<MavenArtifactLicense> getLicenses() {
        return licenses;
    }

    public List<MavenArtifactDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return localVersion.toString();
    }

    private enum ParserState {
        ROOT,
        PARENT,
        PROPERTIES,
        LICENSE,
        DEPENDENCIES,
        DEPENDENCY
    }

    private void loadPOM(DBRProgressMonitor monitor) throws IOException {
        String pomURL = localVersion.getArtifact().getFileURL(localVersion.getVersion(), MavenArtifact.FILE_POM);
        monitor.subTask("Load POM [" + pomURL + "]");
System.out.println("Load POM " + localVersion.getArtifact().toString() + ":" + version);
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
                parentReference = new MavenArtifactReference(
                    XMLUtils.getChildElementBody(parentElement, "groupId"),
                    XMLUtils.getChildElementBody(parentElement, "artifactId"),
                    XMLUtils.getChildElementBody(parentElement, "version")
                );
                if (version == null) {
                    version = parentReference.getVersion();
                }
                MavenArtifact parentArtifact = MavenRegistry.getInstance().findArtifact(parentReference);
                if (parentArtifact == null) {
                    log.error("Artifact [" + this + "] parent [" + parentReference + "] not found");
                } else {
                    parent = parentArtifact.resolveVersion(monitor, parentReference.getVersion());
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
                dependencyManagement = parseDependencies(monitor, dmElement);
            }
            dependencies = parseDependencies(monitor, root);
        }
    }

    private List<MavenArtifactDependency> parseDependencies(DBRProgressMonitor monitor, Element element) {
        List<MavenArtifactDependency> result = new ArrayList<>();
        Element dependenciesElement = XMLUtils.getChildElement(element, "dependencies");
        if (dependenciesElement != null) {
            for (Element dep : XMLUtils.getChildElementList(dependenciesElement, "dependency")) {
                String groupId = XMLUtils.getChildElementBody(dep, "groupId");
                String artifactId = XMLUtils.getChildElementBody(dep, "artifactId");
                String version = XMLUtils.getChildElementBody(dep, "version");
                if (CommonUtils.isEmpty(version)) {
                    version = findDependencyVersion(monitor, groupId, artifactId);
                }
                if (version == null) {
                    log.error("Can't resolve artifact [" + groupId + ":" + artifactId + "] version. Skip.");
                    continue;
                }
                MavenArtifactReference depRef = new MavenArtifactReference(
                    groupId,
                    artifactId,
                    evaluateString(version)
                );
                result.add(new MavenArtifactDependency(
                    depRef,
                    XMLUtils.getChildElementBody(dep, "type"),
                    CommonUtils.getBoolean(XMLUtils.getChildElementBody(dep, "optional"), false)
                ));
            }
        }
        return result;
    }

    private String findDependencyVersion(DBRProgressMonitor monitor, String groupId, String artifactId) {
        if (dependencyManagement != null) {
            for (MavenArtifactDependency dmArtifact : dependencyManagement) {
                if (dmArtifact.getArtifactReference().getGroupId().equals(groupId) &&
                    dmArtifact.getArtifactReference().getArtifactId().equals(artifactId))
                {
                    return dmArtifact.getArtifactReference().getVersion();
                }
            }
        }
        return parent == null ? null : parent.getMetaData(monitor).findDependencyVersion(monitor, groupId, artifactId);
    }

    private String evaluateString(String value) {
        return GeneralUtils.replaceVariables(value, variableResolver);
    }
}