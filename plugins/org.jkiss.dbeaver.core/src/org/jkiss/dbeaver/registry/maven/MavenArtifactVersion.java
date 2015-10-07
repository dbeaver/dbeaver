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
    private MavenArtifactVersion parent;
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
                if (parent != null) {
                    return parent.variableResolver.get(name);
                }
                if (name.equals(PROP_PROJECT_VERSION)) {
                    value = version;
                }
            }
            return value;
        }
    };

    MavenArtifactVersion(MavenLocalVersion localVersion) throws IOException {
        this.localVersion = localVersion;
        loadPOM();
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

    private void loadPOM() throws IOException {
        String pomURL = localVersion.getArtifact().getFileURL(localVersion.getVersion(), MavenArtifact.FILE_POM);
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
                dependencyManagement = parseDependencies(dmElement);
            }
            dependencies = parseDependencies(root);
        }
    }

    private List<MavenArtifactDependency> parseDependencies(Element element) {
        List<MavenArtifactDependency> result = new ArrayList<>();
        Element dependenciesElement = XMLUtils.getChildElement(element, "dependencies");
        if (dependenciesElement != null) {
            for (Element dep : XMLUtils.getChildElementList(dependenciesElement, "dependency")) {
                MavenArtifactReference depRef = new MavenArtifactReference(
                    XMLUtils.getChildElementBody(dep, "groupId"),
                    XMLUtils.getChildElementBody(dep, "artifactId"),
                    evaluateString(XMLUtils.getChildElementBody(dep, "version"))
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

    private String evaluateString(String value) {
        return GeneralUtils.replaceVariables(value, variableResolver);
    }
}