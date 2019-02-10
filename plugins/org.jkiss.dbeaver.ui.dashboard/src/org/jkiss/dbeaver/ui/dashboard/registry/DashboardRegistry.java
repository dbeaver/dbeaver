/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardDataType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

public class DashboardRegistry {
    private static final Log log = Log.getLog(DashboardRegistry.class);

    private static DashboardRegistry instance = null;

    public synchronized static DashboardRegistry getInstance() {
        if (instance == null) {
            instance = new DashboardRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, DashboardDescriptor> dashboardList = new LinkedHashMap<>();
    private final List<DashboardViewTypeDescriptor> viewTypeList = new ArrayList<>();

    private DashboardRegistry(IExtensionRegistry registry) {
        // Load data dashboardList from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardDescriptor.EXTENSION_ID);
        // Load types
        for (IConfigurationElement ext : extElements) {
            if ("dashboardView".equals(ext.getName())) {
                viewTypeList.add(
                    new DashboardViewTypeDescriptor(ext));
            }
        }
        // Load dashboards from extensions
        for (IConfigurationElement ext : extElements) {
            if ("dashboard".equals(ext.getName())) {
                DashboardDescriptor dashboard = new DashboardDescriptor(this, ext);
                dashboardList.put(dashboard.getId(), dashboard);
            }
        }

        // Load dashboards from config
        File configFile = getDashboardsConfigFile();
        if (configFile.exists()) {
            try {
                loadConfigFromFile(configFile);
            } catch (Exception e) {
                log.error("Error loading dashboard configuration", e);
            }
        }
    }

    private void loadConfigFromFile(File configFile) throws XMLException {
        Document dbDocument = XMLUtils.parseDocument(configFile);
        for (Element dbElement : XMLUtils.getChildElementList(dbDocument.getDocumentElement(), "dashboard")) {
            DashboardDescriptor dashboard = new DashboardDescriptor(this, dbElement);
            dashboardList.put(dashboard.getId(), dashboard);
        }
    }

    private void saveConfigFile() {
        try (OutputStream out = new FileOutputStream(getDashboardsConfigFile())){
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            xml.startElement("dashboards");
            for (DashboardDescriptor dashboard : dashboardList.values()) {
                if (dashboard.isCustom()) {
                    xml.startElement("dashboard");
                    dashboard.serialize(xml);
                    xml.endElement();
                }
            }
            xml.endElement();
            xml.flush();
        } catch (Exception e) {
            log.error("Error saving dashboard configuration", e);
        }
    }

    private File getDashboardsConfigFile() {
        return new File(UIDashboardActivator.getDefault().getStateLocation().toFile(), "dashboards.xml");
    }

    public DashboardViewTypeDescriptor getViewType(String id) {
        for (DashboardViewTypeDescriptor descriptor : viewTypeList) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public List<DashboardDescriptor> getAllDashboards() {
        return new ArrayList<>(dashboardList.values());
    }

    public DashboardDescriptor getDashboard(String id) {
        return dashboardList.get(id);
    }

    /**
     * Find dashboard matchign source. Source can be {@link DBPDataSourceContainer}, {@link DBPDataSourceProviderDescriptor} or {@link DBPDriver}
     */
    public List<DashboardDescriptor> getDashboards(DBPNamedObject source, boolean defaultOnly) {
        if (source instanceof DBPDataSourceContainer) {
            source = ((DBPDataSourceContainer) source).getDriver();
        }
        String providerId, driverId, driverClass;
        if (source instanceof DBPDataSourceProviderDescriptor) {
            providerId = ((DBPDataSourceProviderDescriptor) source).getId();
            driverId = null;
            driverClass = null;
        } else {
            providerId = ((DBPDriver)source).getProviderId();
            driverId = ((DBPDriver)source).getId();
            driverClass = ((DBPDriver)source).getDriverClassName();
        }

        List<DashboardDescriptor> result = new ArrayList<>();
        for (DashboardDescriptor dd : dashboardList.values()) {
            if (dd.matches(providerId, driverId, driverClass)) {
                if (!defaultOnly || dd.isShowByDefault()) {
                    result.add(dd);
                }
            }
        }
        return result;
    }

    public void createDashboard(DashboardDescriptor dashboard) throws IllegalArgumentException {
        if (dashboardList.containsKey(dashboard.getId())) {
            throw new IllegalArgumentException("Dashboard " + dashboard.getId() + "' already exists");
        }
        if (!dashboard.isCustom()) {
            throw new IllegalArgumentException("Only custom dashboards can be added");
        }
        dashboardList.put(dashboard.getId(), dashboard);

        saveConfigFile();
    }

    public void removeDashboard(DashboardDescriptor dashboard) throws IllegalArgumentException {
        if (!dashboardList.containsKey(dashboard.getId())) {
            throw new IllegalArgumentException("Dashboard " + dashboard.getId() + "' doesn't exist");
        }
        if (!dashboard.isCustom()) {
            throw new IllegalArgumentException("Only custom dashboards can be removed");
        }
        dashboardList.remove(dashboard.getId());

        saveConfigFile();
    }

    public List<DashboardViewType> getSupportedViewTypes(DashboardDataType dataType) {
        List<DashboardViewType> result = new ArrayList<>();
        for (DashboardViewType vt : viewTypeList) {
            if (ArrayUtils.contains(vt.getSupportedTypes(), dataType)) {
                result.add(vt);
            }
        }
        return result;
    }

    public List<DBPNamedObject> getAllSupportedSources() {
        Set<DBPNamedObject> result = new LinkedHashSet<>();
        for (DashboardDescriptor dd : dashboardList.values()) {
            result.addAll(dd.getSupportedSources());
        }
        ArrayList<DBPNamedObject> sortedDrivers = new ArrayList<>(result);
        sortedDrivers.sort(Comparator.comparing(DBPNamedObject::getName));
        return sortedDrivers;
    }

    public void saveSettings() {
        saveConfigFile();
    }
}
