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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.impl.ProviderPropertyDescriptor;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.navigator.meta.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadataRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.MissingDataSourceProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * DataSourceProviderDescriptor
 */
public class DataSourceProviderDescriptor extends AbstractDescriptor implements DBPDataSourceProviderDescriptor {
    private static final String ATTRIBUTE_CHANGE_FOLDER_LABEL = "changeFolderLabel"; //$NON-NLS-1$
    private static final String ATTRIBUTE_CHANGE_FOLDER_TYPE = "changeFolderType"; //$NON-NLS-1$
    private static final String ATTRIBUTE_REMOVE = "remove"; //$NON-NLS-1$
    private static final String ATTRIBUTE_REPLACE_CHILDREN = "replaceChildren"; //$NON-NLS-1$

    private static final Log log = Log.getLog(DataSourceProviderDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceProvider"; //$NON-NLS-1$

    public static final DataSourceProviderDescriptor NULL_PROVIDER = new DataSourceProviderDescriptor(null, "NULL");

    private DataSourceProviderRegistry registry;
    private DataSourceProviderDescriptor parentProvider;
    private final String id;
    private ObjectType implType;
    private final String name;
    private final String description;
    private final boolean temporary;
    private DBPImage icon;
    private DBPDataSourceProvider instance;
    private DBXTreeDescriptor treeDescriptor;
    private final Map<String, DBXTreeNode> treeNodeMap = new HashMap<>();
    private boolean driversManagable;
    private boolean supportsDriverMigration;
    private final List<DBPPropertyDescriptor> driverProperties = new ArrayList<>();
    private final List<DriverDescriptor> drivers = new ArrayList<>();
    private final List<NativeClientDescriptor> nativeClients = new ArrayList<>();
    private final List<DBPDataSourceProviderDescriptor> childrenProviders = new ArrayList<>();
    private final List<ProviderPropertiesInto> providerProperties = new ArrayList<>();

    @NotNull
    private SQLDialectMetadata scriptDialect;
    private boolean inheritClients;
    private boolean inheritAuthModels = true;

    public DataSourceProviderDescriptor(DataSourceProviderRegistry registry, IConfigurationElement config) {
        super(config);
        this.registry = registry;
        this.temporary = false;

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
        SQLDialectMetadataRegistry dialectRegistry = DBWorkbench.getPlatform().getSQLDialectRegistry();
        this.scriptDialect = dialectRegistry.getDialect(dialectId);
        if (this.scriptDialect == null) {
            log.debug("Script dialect '" + dialectId + "' not found in registry (for data source provider " + id + "). Use default.");
            this.scriptDialect = dialectRegistry.getDialect(BasicSQLDialect.ID);
        }

        // Load tree structure
        IConfigurationElement[] trees = config.getChildren(RegistryConstants.TAG_TREE);
        if (!ArrayUtils.isEmpty(trees)) {
            this.treeDescriptor = this.loadTreeInfo(trees[0]);
        }
        this.supportsDriverMigration = CommonUtils.toBoolean(config.getAttribute("supports-migration"));
        this.inheritAuthModels = CommonUtils.getBoolean(config.getAttribute("inheritAuthModels"), true);
    }

    void linkParentProvider(IConfigurationElement config) {
        String parentId = config.getAttribute(RegistryConstants.ATTR_PARENT);
        if (!CommonUtils.isEmpty(parentId)) {
            this.parentProvider = registry.getDataSourceProvider(parentId);
            if (this.parentProvider == null) {
                log.error("Provider '" + parentId + "' not found");
            } else {
                this.parentProvider.addChildrenProvider(this);
            }
        }
    }

    void loadExtraConfig(IConfigurationElement config) {
        {
            // Load tree structure
            if (treeDescriptor == null && parentProvider != null) {
                // Use parent's tree
                this.treeDescriptor = new DBXTreeDescriptor(this, parentProvider.getTreeDescriptor());
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

                // Load main properties
                {
                    for (IConfigurationElement propsElement : driversElement.getChildren(RegistryConstants.TAG_MAIN_PROPERTIES)) {
                        String driversSpec = propsElement.getAttribute("drivers");
                        List<ProviderPropertyDescriptor> mainProperties = new ArrayList<>();
                        for (IConfigurationElement prop : propsElement.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
                            mainProperties.addAll(ProviderPropertyDescriptor.extractProviderProperties(prop));
                        }
                        List<DriverDescriptor> appDrivers;
                        if (CommonUtils.isEmpty(driversSpec) || driversSpec.equals("*")) {
                            appDrivers = drivers;
                        } else {
                            String[] driverIds = driversSpec.split(",");
                            appDrivers = drivers.stream()
                                .filter(d -> ArrayUtils.contains(driverIds, d.getId())).collect(Collectors.toList());
                        }
                        appDrivers.forEach(d -> d.addMainPropertyDescriptors(mainProperties));
                    }
                }

                // Load provider properties
                {
                    for (IConfigurationElement propsElement : driversElement.getChildren(RegistryConstants.TAG_PROVIDER_PROPERTIES)) {
                        String driversSpec = propsElement.getAttribute("drivers");
                        List<ProviderPropertyDescriptor> providerProperties = new ArrayList<>();
                        for (IConfigurationElement prop : propsElement.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
                            providerProperties.addAll(ProviderPropertyDescriptor.extractProviderProperties(prop));
                        }
                        this.providerProperties.add(new ProviderPropertiesInto(driversSpec, providerProperties));
                    }
                }
            }
        }

        // Load native clients
        {
            inheritClients = CommonUtils.getBoolean(config.getAttribute("inheritClients"),
                false); // Will be "true" if we can use native clients list from the parent

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
        this.implType = new ObjectType(MissingDataSourceProvider.class.getName());
        this.temporary = true;
        this.treeDescriptor = new DBXTreeDescriptor(this, null, null, id, id, false, true, false, false, true, null, null);
        this.scriptDialect = DBWorkbench.getPlatform().getSQLDialectRegistry().getDialect(BasicSQLDialect.ID);
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

    public void dispose() {
        drivers.clear();
        instance = null;
    }

    public DataSourceProviderRegistry getRegistry() {
        return registry;
    }

    @Override
    public DataSourceProviderDescriptor getParentProvider() {
        return parentProvider;
    }

    @Override
    public boolean matchesId(String id) {
        if (id.equals(this.id)) return true;
        if (!inheritAuthModels) {
            return false;
        }
        return parentProvider != null && parentProvider.matchesId(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return CommonUtils.toString(name, id);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    public DBPDataSourceProvider getInstance(DriverDescriptor driver) {
        if (instance == null) {
            initProviderBundle(driver);
            try {
                // locate class
                this.instance = implType.createInstance(DBPDataSourceProvider.class);
                // Initialize it
                this.instance.init(DBWorkbench.getPlatform());
            } catch (Throwable ex) {
                this.instance = null;
                throw new IllegalStateException("Can't initialize data source provider '" + implType.getImplName() + "'",
                    ex);
            }
        }
        return instance;
    }

    void replaceImplClass(IContributor contributor, String providerClass) {
        this.replaceContributor(contributor);
        this.implType = new ObjectType(providerClass);
    }

    @Override
    public DBXTreeDescriptor getTreeDescriptor() {
        return treeDescriptor == null ? (parentProvider == null ? null : parentProvider.getTreeDescriptor())
            : treeDescriptor;
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

    public boolean isDriversManagable() {
        return driversManagable;
    }

    public boolean supportsDriverMigration() {
        return supportsDriverMigration;
    }

    public List<DBPPropertyDescriptor> getDriverProperties() {
        return driverProperties;
    }

    public DBPPropertyDescriptor getDriverProperty(String name) {
        for (DBPPropertyDescriptor prop : driverProperties) {
            if (prop.getId().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    public List<DriverDescriptor> getDrivers() {
        return drivers;
    }

    public void removeCustomAndDisabledDrivers() {
        drivers.removeIf(driver -> driver.isCustom() || driver.isDisabled());
    }

    public List<DriverDescriptor> getEnabledDrivers() {
        List<DriverDescriptor> eDrivers = new ArrayList<>();
        for (DriverDescriptor driver : drivers) {
            if (!driver.isDisabled() && driver.getReplacedBy() == null && driver.isSupportedByLocalSystem()) {
                eDrivers.add(driver);
            }
        }
        return eDrivers;
    }

    /**
     * Retrieves an original or, if another one replaced it, substituted driver by the given {@code id}.
     *
     * @param id identifier of the driver to retrieve
     * @return driver or {@code null} if no driver was found
     */
    @Nullable
    @Override
    public DriverDescriptor getDriver(@NotNull String id) {
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

    /**
     * Retrieves a driver by the given {@code id}.
     *
     * @param id identifier of the driver to retrieve
     * @return driver or {@code null} if no driver was found
     */
    @Nullable
    public DriverDescriptor getOriginalDriver(@NotNull String id) {
        for (DriverDescriptor driver : drivers) {
            if (driver.getId().equals(id)) {
                return driver;
            }
        }

        return null;
    }

    public DriverDescriptor createDriver() {
        return createDriver(SecurityUtils.generateGUID(false));
    }

    public DriverDescriptor createDriver(String id) {
        return new DriverDescriptor(this, id);
    }

    public DriverDescriptor createDriver(DriverDescriptor copyFrom) {
        return new DriverDescriptor(this, SecurityUtils.generateGUID(false), copyFrom);
    }

    public void addDriver(DriverDescriptor driver) {
        this.drivers.add(driver);
    }

    public boolean removeDriver(DriverDescriptor driver) {
        if (!driver.isCustom()) {
            driver.setDisabled(true);
            driver.setModified(true);
            return true;
        } else {
            return this.drivers.remove(driver);
        }
    }

    @NotNull
    @Override
    public List<DBPDataSourceProviderDescriptor> getChildrenProviders() {
        return childrenProviders;
    }

    private void addChildrenProvider(@NotNull DataSourceProviderDescriptor descriptor) {
        childrenProviders.add(descriptor);
    }

    //////////////////////////////////////
    // Native clients

    public List<NativeClientDescriptor> getNativeClients() {
        if (inheritClients && parentProvider != null) {
            List<NativeClientDescriptor> clients = new ArrayList<>(nativeClients);
            clients.addAll(parentProvider.getNativeClients());
            return clients;
        }
        return nativeClients;
    }

    //////////////////////////////////////
    // Internal


    private void initProviderBundle(DriverDescriptor driver) {
    }

    private DBXTreeDescriptor loadTreeInfo(IConfigurationElement config) {
        DBXTreeDescriptor treeRoot = new DBXTreeDescriptor(
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
        DBXTreeNode parentNode = baseItem;

        if (CommonUtils.getBoolean(config.getAttribute(ATTRIBUTE_REPLACE_CHILDREN))) {
            baseItem.clearChildren();
        }

        if (CommonUtils.getBoolean(config.getAttribute(ATTRIBUTE_REMOVE))) {
            baseItem.clearChildren();
            DBXTreeNode folderNode = baseItem.getParent();
            if (folderNode != null) {
                folderNode.removeChild(baseItem);
            }
        }

        String changeFolderType = config.getAttribute(ATTRIBUTE_CHANGE_FOLDER_TYPE);
        if (changeFolderType != null) {
            DBXTreeNode folderNode = baseItem.getParent();
            if (folderNode instanceof DBXTreeFolder folder) {
                folder.setType(changeFolderType);
                String changeFolderLabel = config.getAttribute(ATTRIBUTE_CHANGE_FOLDER_LABEL);
                if (CommonUtils.isNotEmpty(changeFolderLabel)) {
                    folder.setInjectedConfig(config);
                    folder.setLabel(changeFolderLabel);
                    folder.setDescription(changeFolderLabel);
                }
            } else {
                log.error("Can't update folder type to " + changeFolderType);
            }
        } else {
            String afterPath = config.getAttribute(RegistryConstants.ATTR_AFTER);
            DBXTreeNode afterItem = null;
            if (afterPath != null) {
                afterItem = baseItem.findChildItemByPath(afterPath);
            } else {
                String sibling = config.getAttribute("sibling");
                if (sibling != null) {
                    DBXTreeItem siblingItem = baseItem.findChildItemByPath(sibling);
                    if (siblingItem == null) {
                        log.error("Sibling item '" + sibling + "' not found");
                    } else {
                        parentNode = siblingItem.getParent();
                    }
                }
            }

            // Inject nodes into tree item
            loadTreeChildren(config, parentNode, afterItem);
        }
    }

    private void loadTreeChildren(IConfigurationElement config, DBXTreeNode parent, DBXTreeNode afterItem) {
        IConfigurationElement[] children = config.getChildren();
        if (!ArrayUtils.isEmpty(children)) {
            for (IConfigurationElement child : children) {
                loadTreeNode(parent, child, afterItem);
            }
        }
    }

    private void loadTreeNode(DBXTreeNode parent, IConfigurationElement config, DBXTreeNode afterItem) {
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
                        config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF),
                        CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_OPTIONAL)));
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
                        ((DBXTreeFolder) parent).addContribution(contrCategory);
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
                if (child instanceof DBXTreeFolder treeFolder) {
                    var firstItem = treeFolder.getChildren(null)
                        .stream()
                        .filter(folderChild -> folderChild instanceof DBXTreeItem)
                        .findFirst()
                        .orElse(null);
                    if (firstItem == null && CommonUtils.isEmpty(treeFolder.getId())) {
                        log.warn(config + " folder has no child items and unique id is not specified " + treeFolder.getIdOrType() + " " + config.getAttribute(
                            "icon"));
                    }
                }
            }
        }
        if (child != null && afterItem != null) {
            parent.moveChildAfter(child, afterItem);
        }
    }

    private void loadTreeHandlers(DBXTreeNode node, IConfigurationElement config) {
        IConfigurationElement[] handlerElements = config.getChildren("handler");
        if (!ArrayUtils.isEmpty(handlerElements)) {
            for (IConfigurationElement iconElement : handlerElements) {
                try {
                    DBXTreeNodeHandler.Action action = DBXTreeNodeHandler.Action.valueOf(iconElement.getAttribute(
                        "action"));
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

    private void loadTreeIcon(DBXTreeNode node, IConfigurationElement config) {
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

    private DriverDescriptor loadDriver(IConfigurationElement config) {
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
            if (CommonUtils.equalObjects(category, driver.getCategory()) && CommonUtils.equalObjects(name,
                driver.getName())) {
                return driver;
            }
        }
        return null;
    }

    public static boolean matchesId(DBPDataSourceProviderDescriptor providerDescriptor, String id) {
        for (DBPDataSourceProviderDescriptor dspd = providerDescriptor; dspd != null; dspd = dspd.getParentProvider()) {
            if (id.equals(dspd.getId())) {
                return true;
            }
        }
        return false;
    }

    public void setDriverProviderProperties() {
        providerProperties.forEach(propInfo -> {
            String driversSpec = propInfo.driverIds();
            Predicate<DriverDescriptor> predicate =
                (CommonUtils.isEmpty(driversSpec) || driversSpec.equals("*"))
                    ? d -> true
                    : d -> ArrayUtils.contains(driversSpec.split(","), d.getId());
            this.drivers.stream()
                .filter(predicate)
                .forEach(d -> d.addProviderPropertyDescriptors(propInfo.providerProperties()));
        });
    }

    public record ProviderPropertiesInto(@Nullable String driverIds, @NotNull List<ProviderPropertyDescriptor> providerProperties) {
    }

}
