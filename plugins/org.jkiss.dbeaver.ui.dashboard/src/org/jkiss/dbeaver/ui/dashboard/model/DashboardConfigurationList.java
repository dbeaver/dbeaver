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
package org.jkiss.dbeaver.ui.dashboard.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardConfigurationList
 */
public class DashboardConfigurationList {

    private static final Log log = Log.getLog(DashboardConfigurationList.class);

    public static final String DEFAULT_DASHBOARD_ID = "default";
    public static final String DEFAULT_DASHBOARD_NAME = "Default";

    @NotNull
    private final DBPProject project;
    @Nullable
    private IFile dashboardFile;
    @Nullable
    private final DBPDataSourceContainer dataSourceContainer;
    private final Map<String, DashboardConfiguration> dashboards = new LinkedHashMap<>();

    public DashboardConfigurationList(@NotNull DBPProject project, @NotNull IFile dashboardFile) {
        this.project = project;
        this.dashboardFile = dashboardFile;
        this.dataSourceContainer = null;
        loadFromFile(dashboardFile);
    }

    public DashboardConfigurationList(DBPDataSourceContainer dataSourceContainer) {
        this.project = dataSourceContainer.getProject();
        this.dataSourceContainer = dataSourceContainer;
        loadFromDataSource();
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    @Nullable
    public IFile getDashboardFile() {
        return dashboardFile;
    }

    @Nullable
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    private void loadFromDataSource() {
        Document dbDocument = null;

        try {
            String dbSerialized = dataSourceContainer.getExtension(DashboardConstants.DS_PROP_DASHBOARDS);
            if (!CommonUtils.isEmpty(dbSerialized)) {
                dbDocument = XMLUtils.parseDocument(new StringReader(dbSerialized));
            } else {
                // Backward compatibility - read from file
                Path configFile = getConfigFile(false);
                if (Files.exists(configFile)) {
                    dbDocument = XMLUtils.parseDocument(configFile.toFile());
                }
            }
        } catch (XMLException e) {
            log.error("Error parsing dashboards", e);
        }

        loadConfiguration(dbDocument);
    }

    private void loadFromFile(IFile file) {
        Document dbDocument = null;

        if (file.exists()) {
            try (InputStream is = file.getContents()) {
                dbDocument = XMLUtils.parseDocument(is);
            } catch (Exception e) {
                log.error("Error parsing dashboards", e);
            }

            loadConfiguration(dbDocument);
        }
    }

    private void loadConfiguration(Document dbDocument) {
        if (dbDocument != null) {
            try {
                Element rootElement = dbDocument.getDocumentElement();
                if (rootElement.getTagName().equals("dashboardList")) {
                    for (Element dashboardElement : XMLUtils.getChildElementList(rootElement, "dashboards")) {
                        DashboardConfiguration configuration = new DashboardConfiguration(
                            project,
                            dataSourceContainer,
                            null
                        );
                        configuration.loadConfiguration(dashboardElement);
                        dashboards.put(configuration.getDashboardId(), configuration);
                    }
                } else if (rootElement.getTagName().equals("dashboards")) {
                    DashboardConfiguration configuration = new DashboardConfiguration(
                        project,
                        dataSourceContainer,
                        null
                    );
                    configuration.loadConfiguration(rootElement);
                    dashboards.put(configuration.getDashboardId(), configuration);
                } else {
                    throw new DBException("Unsupported dashboards format: " + rootElement.getTagName());
                }
            } catch (Exception e) {
                log.error("Error loading dashboard view configuration", e);
            }
        }
    }

    public void saveConfiguration() throws IOException {
        if (dashboardFile != null) {
            try {
                ByteArrayInputStream contents = new ByteArrayInputStream(saveToString().getBytes(StandardCharsets.UTF_8));
                if (!dashboardFile.exists()) {
                    dashboardFile.create(contents, true, new NullProgressMonitor());
                } else {
                    dashboardFile.setContents(contents, true, false, new NullProgressMonitor());
                }
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
        } else if (dataSourceContainer != null) {
            saveToDataSource();
        } else {
            throw new IOException("Unsupported dashboard configuration format");
        }
    }

    private String saveToString() throws IOException {
        StringWriter buffer = new StringWriter();
        XMLBuilder xml = new XMLBuilder(buffer, GeneralUtils.UTF8_ENCODING, true);
        xml.setButify(true);
        serializeConfig(xml);
        xml.flush();

        return buffer.toString();
    }

    private void saveToDataSource() throws IOException {
        if (dataSourceContainer == null) {
            throw new IOException("Dashboard configuration is not connected with datasource");
        }
        if (dashboards.isEmpty()) {
            dataSourceContainer.setExtension(DashboardConstants.DS_PROP_DASHBOARDS, null);
        } else {
            StringWriter buffer = new StringWriter();
            XMLBuilder xml = new XMLBuilder(buffer, GeneralUtils.UTF8_ENCODING, false);
            xml.setButify(false);
            serializeConfig(xml);
            xml.flush();
            dataSourceContainer.setExtension(DashboardConstants.DS_PROP_DASHBOARDS, buffer.toString());
        }
        dataSourceContainer.persistConfiguration();
    }

    private void serializeConfig(XMLBuilder xml) throws IOException {
        try (var ignored = xml.startElement("dashboardList")) {
            for (DashboardConfiguration dashboard : dashboards.values()) {
                dashboard.serializeConfig(xml);
            }
        }
    }

    private Path getConfigFile(boolean forceCreate) {
        Path pluginFolder = RuntimeUtils.getPluginStateLocation(UIDashboardActivator.getDefault());
        Path viewConfigFolder = pluginFolder.resolve("views");
        if (!Files.exists(viewConfigFolder)) {
            if (forceCreate) {
                try {
                    Files.createDirectories(viewConfigFolder);
                } catch (IOException e) {
                    log.error("Can't create view config folder " + viewConfigFolder, e);
                }
            }
        }
        return viewConfigFolder.resolve("view-" + project.getName() +
            (dataSourceContainer == null ? "" : "_" + dataSourceContainer.getId().replace("/", "_")) + ".xml");
    }

    public List<DashboardConfiguration> getDashboards() {
        return new ArrayList<>(dashboards.values());
    }


    public DashboardConfiguration getDashboard(String id) {
        return dashboards.get(id);
    }

    public DashboardConfiguration createDashboard(String id, String name) {
        DashboardConfiguration configuration = new DashboardConfiguration(project, dataSourceContainer, id);
        configuration.setDashboardName(name);
        this.dashboards.put(id, configuration);
        return configuration;
    }

    public void deleteDashBoard(DashboardConfiguration dashboard) {
        if (this.dashboards.remove(dashboard.getDashboardId()) != null) {
            try {
                saveConfiguration();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void checkDefaultDashboardExistence() {
        if (dashboards.isEmpty()) {
            // Add fake default dashboard
            this.createDashboard(
                DashboardConfigurationList.DEFAULT_DASHBOARD_ID,
                DashboardConfigurationList.DEFAULT_DASHBOARD_NAME);
        }
    }
}
