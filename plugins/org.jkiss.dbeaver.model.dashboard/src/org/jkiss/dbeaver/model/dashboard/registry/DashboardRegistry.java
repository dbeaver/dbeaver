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
package org.jkiss.dbeaver.model.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.dashboard.DBDashboardContext;
import org.jkiss.dbeaver.model.dashboard.DBDashboardFolder;
import org.jkiss.dbeaver.model.dashboard.DBDashboardProvider;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
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

    private final Map<String, DashboardProviderDescriptor> dashboardProviders = new LinkedHashMap<>();
    private final Map<String, DashboardDescriptor> dashboards = new LinkedHashMap<>();

    private DashboardRegistry(IExtensionRegistry registry) {
        // Load data dashboardList from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DashboardDescriptor.EXTENSION_ID);
        // Load dashboards providers
        for (IConfigurationElement ext : extElements) {
            if ("provider".equals(ext.getName())) {
                DashboardProviderDescriptor provider = new DashboardProviderDescriptor(ext);
                dashboardProviders.put(provider.getId(), provider);
            }
        }

        // Load all static dashboards
        DBDashboardContext staticContext = new DBDashboardContext();
        for (DashboardProviderDescriptor dp : dashboardProviders.values()) {
            for (DashboardDescriptor dashboard : dp.getInstance().loadStaticDashboards(dp)) {
                dashboards.put(dashboard.getId(), dashboard);
            }
        }

        // Load dashboards from config
        loadConfigFromFile();
        WorkspaceConfigEventManager.addConfigChangedListener(CONFIG_FILE_NAME, o -> loadConfigFromFile());
    }

    private void loadConfigFromFile() {
        try {
            String configContent = DBWorkbench.getPlatform()
                .getPluginConfigurationController(DashboardConstants.DASHBOARDS_PLUGIN_ID)
                .loadConfigurationFile(CONFIG_FILE_NAME);
            if (CommonUtils.isEmpty(configContent)) {
                configContent = DBWorkbench.getPlatform()
                    .getPluginConfigurationController(DashboardConstants.DASHBOARDS_LEGACY_PLUGIN_ID)
                    .loadConfigurationFile(CONFIG_FILE_NAME);
            }
            
            synchronized (syncRoot) {
                // Clear all custom dashboards
                dashboards.values().removeIf(DashboardDescriptor::isCustom);
                if (CommonUtils.isNotEmpty(configContent)) {
                    Document dbDocument = XMLUtils.parseDocument(new StringReader(configContent));
                    for (Element dbElement : XMLUtils.getChildElementList(dbDocument.getDocumentElement(), "dashboard")) {
                        DashboardDescriptor dashboard = new DashboardDescriptor(this, dbElement);
                        dashboards.put(dashboard.getId(), dashboard);
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
            try (var e = xml.startElement("dashboards")) {
                synchronized (syncRoot) {
                    for (DashboardDescriptor dashboard : dashboards.values()) {
                        if (dashboard.isCustom()) {
                            try (var e1 = xml.startElement("dashboard")) {
                                dashboard.serialize(xml);
                            }
                        }
                    }
                }
            }
            xml.flush();
            out.flush();
            
            DBWorkbench.getPlatform()
                .getPluginConfigurationController(DashboardConstants.DASHBOARDS_PLUGIN_ID)
                .saveConfigurationFile(CONFIG_FILE_NAME, out.getBuffer().toString());
        } catch (Exception e) {
            log.error("Error saving dashboard configuration", e);
        }
    }

    public List<DashboardDescriptor> getAllDashboards() {
        synchronized (syncRoot) {
            return new ArrayList<>(dashboards.values());
        }
    }

    public DashboardDescriptor findDashboard(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDashboardContext context,
        @NotNull String id
    ) throws DBException {
        int divPos = id.indexOf(':');
        String providerId = DashboardConstants.DEF_DASHBOARD_PROVIDER;
        if (divPos != -1) {
            providerId = id.substring(0, divPos);
            id = id.substring(divPos + 1);
        }
        divPos = id.lastIndexOf('/');
        String path = null;
        if (divPos != -1) {
            path = id.substring(0, divPos);
            while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            while (path.startsWith("/")) path = path.substring(1);
            id = id.substring(divPos + 1);
        }
        if (!CommonUtils.isEmpty(path)) {
            DashboardProviderDescriptor provider = getDashboardProvider(providerId);
            if (provider == null) {
                log.debug("Dashboard provider '" + providerId + "' not found");
                return null;
            }
            DBDashboardFolder curFolder = null;
            for (String pathItem : path.split("/")) {
                if (curFolder == null) {
                    curFolder = DBUtils.findObject(provider.getInstance().loadRootFolders(monitor, provider, context), pathItem);
                } else {
                    curFolder = DBUtils.findObject(curFolder.loadSubFolders(monitor, context), pathItem);
                }
                if (curFolder == null) {
                    break;
                }
            }
            if (curFolder == null) {
                log.debug("Folder path '" + path + "' cannot be resolved");
                return null;
            }
            for (DashboardDescriptor dashboard : curFolder.loadDashboards(monitor, context)) {
                if (dashboard.getId().equals(id)) {
                    return dashboard;
                }
            }
            log.debug("Dashboard '" + id + "' not found in path '" + path + "'");
            return null;
        }
        synchronized (syncRoot) {
            return dashboards.get(id);
        }
    }

    public DashboardDescriptor getDashboard(String id) {
        synchronized (syncRoot) {
            return dashboards.get(id);
        }
    }

    public List<DashboardProviderDescriptor> getDashboardProviders() {
        return new ArrayList<>(dashboardProviders.values());
    }

    public List<DashboardProviderDescriptor> getDashboardProviders(DBPDataSourceContainer dataSource) {
        if (dataSource != null) {
            return dashboardProviders.values().stream().filter(dp -> dp.appliesTo(dataSource)).toList();
        }
        return dashboardProviders.values().stream().filter(DashboardProviderDescriptor::isEnabled).toList();
    }

    public DashboardProviderDescriptor getDashboardProvider(String id) {
        return dashboardProviders.get(id);
    }

    public DBDashboardProvider getDashboardProviderInstance(String id) {
        DashboardProviderDescriptor dpd = dashboardProviders.get(id);
        if (dpd != null) {
            return dpd.getInstance();
        }
        return null;
    }

    /**
     * Find dashboard matching source.
     * Source can be {@link DBPDataSourceContainer}, {@link DBPDataSourceProviderDescriptor} or {@link DBPDriver}
     */
    public List<DashboardDescriptor> getDashboards(
        @Nullable DashboardProviderDescriptor provider,
        @NotNull DBPNamedObject source,
        boolean defaultOnly
    ) {
        if (source instanceof DBPDataSourceContainer dsc) {
            source = dsc.getDriver();
        }
        String providerId, driverId, driverClass;
        if (source instanceof DBPDataSourceProviderDescriptor dspd) {
            providerId = dspd.getId();
            driverId = null;
            driverClass = null;
        } else if (source instanceof DBPDriver driver) {
            providerId = driver.getProviderId();
            driverId = driver.getId();
            driverClass = driver.getDriverClassName();
        } else {
            return new ArrayList<>();
        }

        List<DashboardDescriptor> result = new ArrayList<>();
        synchronized (syncRoot) {
            for (DashboardDescriptor dd : dashboards.values()) {
                if (provider != null && provider != dd.getDashboardProvider()) {
                    continue;
                }
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
            if (dashboards.containsKey(dashboard.getId())) {
                throw new IllegalArgumentException("Dashboard " + dashboard.getId() + "' already exists");
            }
            if (!dashboard.isCustom()) {
                throw new IllegalArgumentException("Only custom dashboards can be added");
            }
            dashboards.put(dashboard.getId(), dashboard);
    
            saveConfigFile();
        }
    }

    public void removeDashboard(DashboardDescriptor dashboard) throws IllegalArgumentException {
        synchronized (syncRoot) {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                throw new IllegalArgumentException("The user has no permission to remove dashboard configuration");
            }
            if (!dashboards.containsKey(dashboard.getId())) {
                throw new IllegalArgumentException("Dashboard " + dashboard.getId() + "' doesn't exist");
            }
            if (!dashboard.isCustom()) {
                throw new IllegalArgumentException("Only custom dashboards can be removed");
            }
            dashboards.remove(dashboard.getId());
    
            saveConfigFile();
        }
    }

    public List<DBPNamedObject> getAllSupportedSources(@NotNull DashboardProviderDescriptor dpd) {
        Set<DBPNamedObject> result = new LinkedHashSet<>();
        synchronized (syncRoot) {
            for (DashboardDescriptor dd : dashboards.values()) {
                if (dd.getDashboardProvider() == dpd) {
                    result.addAll(dd.getSupportedSources());
                }
            }
        }
        ArrayList<DBPNamedObject> sortedDrivers = new ArrayList<>(result);
        sortedDrivers.sort(Comparator.comparing(DBPNamedObject::getName));
        return sortedDrivers;
    }

    public void saveSettings() {
        saveConfigFile();
    }

}
