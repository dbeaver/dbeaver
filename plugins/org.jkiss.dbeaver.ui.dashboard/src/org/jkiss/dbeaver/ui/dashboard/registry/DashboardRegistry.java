/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardDataType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class DashboardRegistry {
    private static final Log log = Log.getLog(DashboardRegistry.class);
    
    public static final String CONFIG_FILE_NAME = "dashboards.xml";

    private static DashboardRegistry instance = null;

    private final Object syncRoot = new Object();

    public synchronized static DashboardRegistry getInstance() {
        if (instance == null) {
            instance = new DashboardRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DashboardViewTypeDescriptor> viewTypeList = new ArrayList<>();
    private final Map<String, DashboardMapQueryDescriptor> mapQueries = new LinkedHashMap<>();
    private final Map<String, DashboardDescriptor> dashboardList = new LinkedHashMap<>();

    private DashboardRegistry(IExtensionRegistry registry) {
        // Load data dashboardList from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardDescriptor.EXTENSION_ID);
        // Load view types
        for (IConfigurationElement ext : extElements) {
            if ("dashboardView".equals(ext.getName())) {
                viewTypeList.add(
                    new DashboardViewTypeDescriptor(ext));
            }
        }
        // Load map queries
        for (IConfigurationElement ext : extElements) {
            if ("mapQuery".equals(ext.getName())) {
                DashboardMapQueryDescriptor query = new DashboardMapQueryDescriptor(ext);
                if (!CommonUtils.isEmpty(query.getId()) && !CommonUtils.isEmpty(query.getQueryText())) {
                    mapQueries.put(query.getId(), query);
                }
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
        loadConfigFromFile();
        WorkspaceConfigEventManager.addConfigChangedListener(CONFIG_FILE_NAME, o -> {
            loadConfigFromFile();
        });
    }

    private void loadConfigFromFile() {
        try {
            String configContent = DBWorkbench.getPlatform()
                .getPluginConfigurationController(UIDashboardActivator.PLUGIN_ID)
                .loadConfigurationFile(CONFIG_FILE_NAME);
            
            synchronized (syncRoot) {    
                dashboardList.clear();
                if (CommonUtils.isNotEmpty(configContent)) {
                    Document dbDocument = XMLUtils.parseDocument(new StringReader(configContent));
                    for (Element dbElement : XMLUtils.getChildElementList(dbDocument.getDocumentElement(), "dashboard")) {
                        DashboardDescriptor dashboard = new DashboardDescriptor(this, dbElement);
                        dashboardList.put(dashboard.getId(), dashboard);
                    }
                }            
            }
        } catch (Exception e) {
            log.error("Error loading dashboard configuration", e);
        }
    }

    private void saveConfigFile() {
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            log.warn("The user has no permission to save dashboards configuration");
            return;
        }
        try {
            StringWriter out = new StringWriter();
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            xml.startElement("dashboards");
            synchronized (syncRoot) {
                for (DashboardDescriptor dashboard : dashboardList.values()) {
                    if (dashboard.isCustom()) {
                        xml.startElement("dashboard");
                        dashboard.serialize(xml);
                        xml.endElement();
                    }
                }
            }
            xml.endElement();
            xml.flush();
            out.flush();
            
            DBWorkbench.getPlatform()
                .getPluginConfigurationController(UIDashboardActivator.PLUGIN_ID)
                .saveConfigurationFile(CONFIG_FILE_NAME, out.getBuffer().toString());
        } catch (Exception e) {
            log.error("Error saving dashboard configuration", e);
        }
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
        synchronized (syncRoot) {
            return new ArrayList<>(dashboardList.values());
        }
    }

    public DashboardDescriptor getDashboard(String id) {
        synchronized (syncRoot) {
            return dashboardList.get(id);
        }
    }

    /**
     * Find dashboard matching source.
     * Source can be {@link DBPDataSourceContainer}, {@link DBPDataSourceProviderDescriptor} or {@link DBPDriver}
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
        synchronized (syncRoot) {
            for (DashboardDescriptor dd : dashboardList.values()) {
                if (dd.matches(providerId, driverId, driverClass)) {
                    if (!defaultOnly || dd.isShowByDefault()) {
                        result.add(dd);
                    }
                }
            }
        }
        return result;
    }

    public void createDashboard(DashboardDescriptor dashboard) throws IllegalArgumentException {
        synchronized (syncRoot) {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                throw new IllegalArgumentException("The user has no permission to create dashboard configuration");
            }
            if (dashboardList.containsKey(dashboard.getId())) {
                throw new IllegalArgumentException("Dashboard " + dashboard.getId() + "' already exists");
            }
            if (!dashboard.isCustom()) {
                throw new IllegalArgumentException("Only custom dashboards can be added");
            }
            dashboardList.put(dashboard.getId(), dashboard);
    
            saveConfigFile();
        }
    }

    public void removeDashboard(DashboardDescriptor dashboard) throws IllegalArgumentException {
        synchronized (syncRoot) {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                throw new IllegalArgumentException("The user has no permission to remove dashboard configuration");
            }
            if (!dashboardList.containsKey(dashboard.getId())) {
                throw new IllegalArgumentException("Dashboard " + dashboard.getId() + "' doesn't exist");
            }
            if (!dashboard.isCustom()) {
                throw new IllegalArgumentException("Only custom dashboards can be removed");
            }
            dashboardList.remove(dashboard.getId());
    
            saveConfigFile();
        }
    }

    public List<DashboardViewType> getAllViewTypes() {
        return new ArrayList<>(viewTypeList);
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
        synchronized (syncRoot) {
            for (DashboardDescriptor dd : dashboardList.values()) {
                result.addAll(dd.getSupportedSources());
            }
        }
        ArrayList<DBPNamedObject> sortedDrivers = new ArrayList<>(result);
        sortedDrivers.sort(Comparator.comparing(DBPNamedObject::getName));
        return sortedDrivers;
    }

    public void saveSettings() {
        saveConfigFile();
    }

    public DashboardMapQueryDescriptor getMapQuery(String id) {
        return mapQueries.get(id);
    }
}
