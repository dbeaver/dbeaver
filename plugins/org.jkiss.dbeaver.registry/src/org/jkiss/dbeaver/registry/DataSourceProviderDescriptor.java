/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.navigator.meta.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataSourceProviderDescriptor
 */
public class DataSourceProviderDescriptor extends AbstractDescriptor implements DBPDataSourceProviderDescriptor
{
    private static final Log log = Log.getLog(DataSourceProviderDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceProvider"; //$NON-NLS-1$

    public static final DataSourceProviderDescriptor NULL_PROVIDER = new DataSourceProviderDescriptor(null, "NULL");

    private DataSourceProviderRegistry registry;
    private DataSourceProviderDescriptor parentProvider;
    private final String id;
    private final ObjectType implType;
    private final String name;
    private final String description;
    private final boolean temporary;
    private DBPImage icon;
    private DBPDataSourceProvider instance;
    private DBXTreeItem treeDescriptor;
    private final Map<String, DBXTreeNode> treeNodeMap = new HashMap<>();
    private boolean driversManagable;
    private final List<DBPPropertyDescriptor> driverProperties = new ArrayList<>();
    private final List<DriverDescriptor> drivers = new ArrayList<>();
    private final List<NativeClientDescriptor> nativeClients = new ArrayList<>();
    @NotNull
    private SQLDialectMetadata scriptDialect;

    public DataSourceProviderDescriptor(DataSourceProviderRegistry registry, IConfigurationElement config)
    {
        super(config);
        this.registry = registry;
        this.temporary = false;

        String parentId = config.getAttribute(RegistryConstants.ATTR_PARENT);
        if (!CommonUtils.isEmpty(parentId)) {
            this.parentProvider = registry.getDataSourceProvider(parentId);
            if (this.parentProvider == null) {
                log.error("Provider '" + parentId + "' not found");
            }
        }

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        if (this.icon == null) {
            this.icon = DBIcon.DATABASE_DEFAULT;
        }
        String dialectId = config.getAttribute(RegistryConstants.ATTR_DIALECT);
        if (CommonUtils.isEmpty(dialectId)) {
            log.debug("No SQL dialect specified for data source provider '" + this.id + "'. Use default.");
            dialectId = BasicSQLDialect.ID;
        }
        this.scriptDialect = SQLDialectRegistry.getInstance().getDialect(dialectId);
        if (this.scriptDialect == null) {
            log.debug("Script dialect '" + dialectId + "' not found in registry (for data source provider " + id + "). Use default.");
            this.scriptDialect = SQLDialectRegistry.getInstance().getDialect(BasicSQLDialect.ID);
        }

        {
            // Load tree structure
            IConfigurationElement[] trees = config.getChildren(RegistryConstants.TAG_TREE);
            if (!ArrayUtils.isEmpty(trees)) {
                this.treeDescriptor = this.loadTreeInfo(trees[0]);
            } else if (parentProvider != null) {
                // Use parent's tree
                this.treeDescriptor = new DBXTreeItem(this, null, parentProvider.treeDescriptor);
            }

            // Load tree injections
            IConfigurationElement[] injections = config.getChildren(RegistryConstants.TAG_TREE_INJECTION);
            if (!ArrayUtils.isEmpty(injections)) {
                for (IConfigurationElement treeInject : injections) {
                    this.injectTreeNodes(treeInject);
                }
            }
        }

        // Load driver properties
        {
            if (parentProvider != null) {
                driverProperties.addAll(parentProvider.driverProperties);
            }
            for (IConfigurationElement propsElement : config.getChildren(RegistryConstants.TAG_DRIVER_PROPERTIES)) {
                for (IConfigurationElement prop : propsElement.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
                    driverProperties.addAll(PropertyDescriptor.extractProperties(prop));
                }
            }
        }

        // Load supplied drivers
        {
            for (IConfigurationElement driversElement : config.getChildren(RegistryConstants.TAG_DRIVERS)) {
                this.driversManagable = driversElement.getAttribute(RegistryConstants.ATTR_MANAGABLE) == null ||
                    CommonUtils.getBoolean(driversElement.getAttribute(RegistryConstants.ATTR_MANAGABLE));
                for (IConfigurationElement driverElement : driversElement.getChildren(RegistryConstants.TAG_DRIVER)) {
                    try {
                        this.drivers.add(loadDriver(driverElement));
                    } catch (Exception e) {
                        log.error("Error loading driver", e);
                    }
                }
            }
        }

        // Load native clients
        {
            for (IConfigurationElement nativeClientsElement : config.getChildren("nativeClients")) {
                for (IConfigurationElement clientElement : nativeClientsElement.getChildren("client")) {
                    this.nativeClients.add(new NativeClientDescriptor(clientElement));
                }
            }
        }
    }

    DataSourceProviderDescriptor(DataSourceProviderRegistry registry, String id) {
        super("org.jkiss.dbeaver.registry");
        this.registry = registry;
        this.id = id;
        this.name = id;
        this.description = "Missing datasource provider " + id;
        this.implType = new ObjectType(DBPDataSourceProvider.class.getName());
        this.temporary = true;
        this.treeDescriptor = new DBXTreeItem(this, null, null, id, id, false, true, false, false, true, null, null);
        this.scriptDialect = SQLDialectRegistry.getInstance().getDialect(BasicSQLDialect.ID);
    }

    void patchConfigurationFrom(IConfigurationElement config) {
        // Load tree injections
        IConfigurationElement[] injections = config.getChildren(RegistryConstants.TAG_TREE_INJECTION);
        if (!ArrayUtils.isEmpty(injections)) {
            for (IConfigurationElement treeInject : injections) {
                this.injectTreeNodes(treeInject);
            }
        }
    }

    public void dispose()
    {
        drivers.clear();
        instance = null;
    }

    public DataSourceProviderRegistry getRegistry()
    {
        return registry;
    }

    @Override
    public DataSourceProviderDescriptor getParentProvider() {
        return parentProvider;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBPImage getIcon()
    {
        return icon;
    }

    @NotNull
    public DBPDataSourceProvider getInstance(DriverDescriptor driver)
    {
        if (instance == null) {
            initProviderBundle(driver);
            try {
                // locate class
                this.instance = implType.createInstance(DBPDataSourceProvider.class);
                // Initialize it
                this.instance.init(DBWorkbench.getPlatform());
            }
            catch (Throwable ex) {
                this.instance = null;
                throw new IllegalStateException("Can't initialize data source provider '" + implType.getImplName() + "'", ex);
            }
        }
        return instance;
    }

    @Override
    public DBXTreeNode getTreeDescriptor()
    {
        return treeDescriptor;
    }

    @NotNull
    @Override
    public SQLDialectMetadata getScriptDialect() {
        return scriptDialect;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    //////////////////////////////////////
    // Drivers

    public boolean isDriversManagable()
    {
        return driversManagable;
    }

    public List<DBPPropertyDescriptor> getDriverProperties()
    {
        return driverProperties;
    }

    public DBPPropertyDescriptor getDriverProperty(String name)
    {
        for (DBPPropertyDescriptor prop : driverProperties) {
            if (prop.getId().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    public List<DriverDescriptor> getDrivers()
    {
        return drivers;
    }

    public List<DriverDescriptor> getEnabledDrivers()
    {
        List<DriverDescriptor> eDrivers = new ArrayList<>();
        for (DriverDescriptor driver : drivers) {
            if (!driver.isDisabled() && driver.getReplacedBy() == null && driver.isSupportedByLocalSystem()) {
                eDrivers.add(driver);
            }
        }
        return eDrivers;
    }

    public DriverDescriptor getDriver(String id)
    {
        for (DriverDescriptor driver : drivers) {
            if (driver.getId().equals(id)) {
                while (driver.getReplacedBy() != null) {
                    driver = driver.getReplacedBy();
                }
                return driver;
            }
        }
        return null;
    }

    public DriverDescriptor createDriver()
    {
        return createDriver(SecurityUtils.generateGUID(false));
    }

    public DriverDescriptor createDriver(String id)
    {
        return new DriverDescriptor(this, id);
    }

    public DriverDescriptor createDriver(DriverDescriptor copyFrom)
    {
        return new DriverDescriptor(this, SecurityUtils.generateGUID(false), copyFrom);
    }

    public void addDriver(DriverDescriptor driver)
    {
        this.drivers.add(driver);
    }

    public boolean removeDriver(DriverDescriptor driver)
    {
        if (!driver.isCustom()) {
            driver.setDisabled(true);
            driver.setModified(true);
            return true;
        } else {
            return this.drivers.remove(driver);
        }
    }

    //////////////////////////////////////
    // Native clients

    public List<NativeClientDescriptor> getNativeClients() {
        return nativeClients;
    }

    //////////////////////////////////////
    // Internal


    private void initProviderBundle(DriverDescriptor driver)
    {
    }

    private DBXTreeItem loadTreeInfo(IConfigurationElement config)
    {
        DBXTreeItem treeRoot = new DBXTreeItem(
            this,
            null,
            config,
            config.getAttribute(RegistryConstants.ATTR_PATH),
            null,
            false,
            true, false, false, false,
            config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF),
            null);
        loadTreeChildren(config, treeRoot, null);
        loadTreeIcon(treeRoot, config);
        return treeRoot;
    }

    private void injectTreeNodes(IConfigurationElement config) {
        String injectPath = config.getAttribute(RegistryConstants.ATTR_PATH);
        if (CommonUtils.isEmpty(injectPath)) {
            return;
        }
        String[] path = injectPath.split("/");
        if (path.length <= 0) {
            return;
        }
        if (!path[0].equals(treeDescriptor.getPath())) {
            return;
        }
        DBXTreeItem baseItem = treeDescriptor;
        for (int i = 1; i < path.length; i++) {
            baseItem = baseItem.findChildItemByPath(path[i]);
            if (baseItem == null) {
                return;
            }
        }

        String changeFolderType = config.getAttribute("changeFolderType");
        if (changeFolderType != null) {
            DBXTreeNode parentNode = baseItem.getParent();
            if (parentNode instanceof DBXTreeFolder) {
                ((DBXTreeFolder)parentNode).setType(changeFolderType);
            } else {
                log.error("Can't update folder type to " + changeFolderType);
            }
        } else {
            String afterPath = config.getAttribute(RegistryConstants.ATTR_AFTER);
            DBXTreeItem afterItem = null;
            if (afterPath != null) {
                afterItem = baseItem.findChildItemByPath(afterPath);
            }

            // Inject nodes into tree item
            loadTreeChildren(config, baseItem, afterItem);
        }
    }

    private void loadTreeChildren(IConfigurationElement config, DBXTreeNode parent, DBXTreeItem afterItem)
    {
        IConfigurationElement[] children = config.getChildren();
        if (!ArrayUtils.isEmpty(children)) {
            for (IConfigurationElement child : children) {
                loadTreeNode(parent, child, afterItem);
            }
        }
    }

    private void loadTreeNode(DBXTreeNode parent, IConfigurationElement config, DBXTreeItem afterItem)
    {
        DBXTreeNode child = null;
        final String refId = config.getAttribute(RegistryConstants.ATTR_REF);
        if (!CommonUtils.isEmpty(refId)) {
            child = treeNodeMap.get(refId);
            if (child != null) {
                parent.addChild(child);
            } else {
                log.warn("Bad node reference: " + refId);
            }
        } else {
            String nodeType = config.getName();
            switch (nodeType) {
                case RegistryConstants.TAG_FOLDER: {
                    child = new DBXTreeFolder(
                        this,
                        parent,
                        config,
                        config.getAttribute(RegistryConstants.ATTR_TYPE),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_NAVIGABLE), true),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_VIRTUAL)),
                        config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF));
                    break;
                }
                case RegistryConstants.TAG_ITEMS: {
                    String recursive = config.getAttribute(RegistryConstants.ATTR_RECURSIVE);
                    child = new DBXTreeItem(
                        this,
                        parent,
                        config,
                        config.getAttribute(RegistryConstants.ATTR_PATH),
                        config.getAttribute(RegistryConstants.ATTR_PROPERTY),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_OPTIONAL)),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_NAVIGABLE), true),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_INLINE)),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_VIRTUAL)),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_STANDALONE)),
                        config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF),
                        recursive);
                    break;
                }
                case RegistryConstants.TAG_TREE_CONTRIBUTION: {
                    String contrCategory = config.getAttribute(RegistryConstants.ATTR_CATEGORY);
                    if (parent instanceof DBXTreeFolder) {
                        ((DBXTreeFolder)parent).addContribution(contrCategory);
                    } else {
                        log.warn(RegistryConstants.TAG_TREE_CONTRIBUTION + " allowed only inside folders");
                    }
                    break;
                }
                case RegistryConstants.TAG_OBJECT: {
                    child = new DBXTreeObject(
                        this,
                        parent,
                        config,
                        config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF),
                        config.getAttribute(RegistryConstants.ATTR_LABEL),
                        config.getAttribute(RegistryConstants.ATTR_DESCRIPTION),
                        config.getAttribute(RegistryConstants.ATTR_EDITOR));
                    break;
                }
                default:
                    // Unknown node type
                    //log.warn("Unknown node type: " + nodeType);
                    break;
            }

            if (child != null) {
                if (!CommonUtils.isEmpty(child.getId())) {
                    treeNodeMap.put(child.getId(), child);
                }
                loadTreeHandlers(child, config);
                loadTreeIcon(child, config);
                loadTreeChildren(config, child, null);
            }
        }
        if (child != null && afterItem != null) {
            parent.moveChildAfter(child, afterItem);
        }
    }

    private void loadTreeHandlers(DBXTreeNode node, IConfigurationElement config)
    {
        IConfigurationElement[] handlerElements = config.getChildren("handler");
        if (!ArrayUtils.isEmpty(handlerElements)) {
            for (IConfigurationElement iconElement : handlerElements) {
                try {
                    DBXTreeNodeHandler.Action action = DBXTreeNodeHandler.Action.valueOf(iconElement.getAttribute("action"));
                    String performName = iconElement.getAttribute("perform");
                    String command = iconElement.getAttribute("command");
                    DBXTreeNodeHandler.Perform perform;
                    if (!CommonUtils.isEmpty(performName)) {
                        perform = DBXTreeNodeHandler.Perform.valueOf(performName);
                    } else if (!CommonUtils.isEmpty(command)) {
                        perform = DBXTreeNodeHandler.Perform.command;
                    } else {
                        perform = DBXTreeNodeHandler.Perform.none;
                    }
                    node.addActionHandler(action, perform, command);
                } catch (Exception e) {
                    log.error("Error adding node handler", e);
                }
            }
        }
    }

    private void loadTreeIcon(DBXTreeNode node, IConfigurationElement config)
    {
        String defaultIcon = config.getAttribute(RegistryConstants.ATTR_ICON);
        IConfigurationElement[] iconElements = config.getChildren(RegistryConstants.ATTR_ICON);
        if (!ArrayUtils.isEmpty(iconElements)) {
            for (IConfigurationElement iconElement : iconElements) {
                String icon = iconElement.getAttribute(RegistryConstants.ATTR_ICON);
                String expr = iconElement.getAttribute(RegistryConstants.ATTR_IF);
                boolean isDefault = CommonUtils.getBoolean(iconElement.getAttribute(RegistryConstants.ATTR_DEFAULT));
                if (isDefault && CommonUtils.isEmpty(expr)) {
                    defaultIcon = icon;
                } else {
                    DBPImage iconImage = iconToImage(icon);
                    if (iconImage != null) {
                        node.addIcon(new DBXTreeIcon(expr, iconImage));
                    }
                }
            }
        }
        if (defaultIcon != null) {
            DBPImage defaultImage = iconToImage(defaultIcon);
            if (defaultImage != null) {
                node.setDefaultIcon(defaultImage);
            }
        }
    }

    private DriverDescriptor loadDriver(IConfigurationElement config)
    {
        return new DriverDescriptor(this, config);
    }

    @Override
    public String toString() {
        return id;
    }

    public String getFullIdentifier() {
        return getPluginId() + '/' + id;
    }

    public DriverDescriptor getDriverByName(String category, String name) {
        if (category != null && category.isEmpty()) {
            category = null;
        }
        for (DriverDescriptor driver : drivers) {
            if (CommonUtils.equalObjects(category, driver.getCategory()) && CommonUtils.equalObjects(name, driver.getName())) {
                return driver;
            }
        }
        return null;
    }

}
