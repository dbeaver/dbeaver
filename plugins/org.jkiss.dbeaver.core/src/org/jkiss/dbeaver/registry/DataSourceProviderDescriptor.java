/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.registry.tree.*;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataSourceProviderDescriptor
 */
public class DataSourceProviderDescriptor extends AbstractDescriptor
{
    static final Log log = LogFactory.getLog(DataSourceProviderDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceProvider"; //$NON-NLS-1$

    public static final String TREE_NODE_TYPE_FOLDER = "folder"; //$NON-NLS-1$
    public static final String TREE_NODE_TYPE_ITEMS = "items"; //$NON-NLS-1$
    public static final String TREE_NODE_TYPE_OBJECT = "object"; //$NON-NLS-1$

    public static final String TREE_ATTR_ID = "id"; //$NON-NLS-1$
    public static final String TREE_ATTR_REF = "ref"; //$NON-NLS-1$
    public static final String TREE_ATTR_PATH = "path"; //$NON-NLS-1$
    public static final String TREE_ATTR_VISIBLE_IF = "visibleIf"; //$NON-NLS-1$
    public static final String TREE_ATTR_TYPE = "type"; //$NON-NLS-1$
    public static final String TREE_ATTR_LABEL = "label"; //$NON-NLS-1$
    public static final String TREE_ATTR_NAVIGABLE = "navigable"; //$NON-NLS-1$
    public static final String TREE_ATTR_DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String TREE_ATTR_ITEM_LABEL = "itemLabel"; //$NON-NLS-1$
    public static final String TREE_ATTR_PROPERTY = "property"; //$NON-NLS-1$
    public static final String TREE_ATTR_OPTIONAL = "optional"; //$NON-NLS-1$
    public static final String TREE_ATTR_VIRTUAL = "virtual"; //$NON-NLS-1$
    public static final String TREE_ATTR_INLINE = "inline"; //$NON-NLS-1$
    public static final String TREE_ATTR_EDITOR = "editor"; //$NON-NLS-1$
    public static final String TREE_ATTR_ICON = "icon"; //$NON-NLS-1$
    public static final String TREE_ATTR_ICON_ID = "iconId"; //$NON-NLS-1$
    public static final String TREE_ATTR_IF = "if"; //$NON-NLS-1$
    public static final String TREE_ATTR_DEFAULT = "default"; //$NON-NLS-1$

    private DataSourceProviderRegistry registry;
    private String id;
    private String implClassName;
    private String name;
    private String description;
    private Image icon;
    private DBPDataSourceProvider instance;
    private DBXTreeNode treeDescriptor;
    private Map<String, DBXTreeNode> treeNodeMap = new HashMap<String, DBXTreeNode>();
    private boolean driversManagable;
    private List<IPropertyDescriptor> driverProperties = new ArrayList<IPropertyDescriptor>();
    private List<DriverDescriptor> drivers = new ArrayList<DriverDescriptor>();
    private List<DataSourceViewDescriptor> views = new ArrayList<DataSourceViewDescriptor>();

    public DataSourceProviderDescriptor(DataSourceProviderRegistry registry, IConfigurationElement config)
    {
        super(config.getContributor());
        this.registry = registry;

        this.id = config.getAttribute("id");
        this.implClassName = config.getAttribute("class");
        this.name = config.getAttribute(TREE_ATTR_LABEL);
        this.description = config.getAttribute(TREE_ATTR_DESCRIPTION);
        String iconName = config.getAttribute(TREE_ATTR_ICON);
        if (!CommonUtils.isEmpty(iconName)) {
            this.icon = iconToImage(iconName);
        }
        if (this.icon == null) {
            this.icon = DBIcon.GEN_DATABASE_TYPE.getImage();
        }

        // Load tree structure
        IConfigurationElement[] trees = config.getChildren("tree");
        if (!CommonUtils.isEmpty(trees)) {
            this.treeDescriptor = this.loadTreeInfo(trees[0]);
        }

        // Load driver properties
        IConfigurationElement[] driverPropsGroup = config.getChildren("driver-properties");
        if (!CommonUtils.isEmpty(driverPropsGroup)) {
            for (IConfigurationElement propsElement : driverPropsGroup) {
                IConfigurationElement[] propElements = propsElement.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP);
                for (IConfigurationElement prop : propElements) {
                    driverProperties.addAll(PropertyDescriptorEx.extractProperties(prop));
                }
            }
        }

        // Load supplied drivers
        IConfigurationElement[] driversGroup = config.getChildren("drivers");
        if (!CommonUtils.isEmpty(driversGroup)) {
            for (IConfigurationElement driversElement : driversGroup) {
                this.driversManagable = driversElement.getAttribute("managable") == null ||
                    CommonUtils.getBoolean(driversElement.getAttribute("managable"));
                IConfigurationElement[] driverList = driversElement.getChildren("driver");
                if (!CommonUtils.isEmpty(driverList)) {
                    for (IConfigurationElement driverElement : driverList) {
                        this.drivers.add(loadDriver(driverElement));
                    }
                }
            }
        }

        // Load views
        IConfigurationElement[] viewsGroup = config.getChildren("views");
        if (!CommonUtils.isEmpty(viewsGroup)) {
            for (IConfigurationElement viewsElement : viewsGroup) {
                IConfigurationElement[] viewList = viewsElement.getChildren("view");
                if (!CommonUtils.isEmpty(viewList)) {
                    for (IConfigurationElement viewElement : viewList) {
                        this.views.add(
                            new DataSourceViewDescriptor(this, viewElement));
                    }
                }
            }
        }
    }

    public void dispose()
    {
        for (DriverDescriptor driver : drivers) {
            driver.dispose();
        }
        drivers.clear();
        if (this.instance != null) {
            this.instance.close();
        }
    }

    public DataSourceProviderRegistry getRegistry()
    {
        return registry;
    }

    public String getId()
    {
        return id;
    }

    public String getImplClassName()
    {
        return implClassName;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Image getIcon()
    {
        return icon;
    }

    public DBPDataSourceProvider getInstance()
        throws DBException
    {
        if (instance == null) {
            // Create instance
            Class<?> implClass = getObjectClass(implClassName);
            if (implClass == null) {
                throw new DBException("Can't find descriptor class '" + implClassName + "'");
            }
            try {
                this.instance = (DBPDataSourceProvider) implClass.newInstance();
            }
            catch (Throwable ex) {
                throw new DBException("Can't instantiate data source provider '" + implClassName + "'", ex);
            }
            // Initialize it
            try {
                this.instance.init(DBeaverCore.getInstance());
            }
            catch (Throwable ex) {
                this.instance = null;
                throw new DBException("Can't initialize data source provider '" + implClassName + "'", ex);
            }
        }
        return instance;
    }

    public DBXTreeNode getTreeDescriptor()
    {
        return treeDescriptor;
    }

    public boolean isDriversManagable()
    {
        return driversManagable;
    }

    public List<IPropertyDescriptor> getDriverProperties()
    {
        return driverProperties;
    }

    public List<DriverDescriptor> getDrivers()
    {
        return drivers;
    }

    public List<DriverDescriptor> getEnabledDrivers()
    {
        List<DriverDescriptor> eDrivers = new ArrayList<DriverDescriptor>();
        for (DriverDescriptor driver : drivers) {
            if (!driver.isDisabled() && driver.getReplacedBy() == null) {
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
        return new DriverDescriptor(this, SecurityUtils.generateGUID(false));
    }

    public void addDriver(DriverDescriptor driver)
    {
        this.drivers.add(driver);
    }

    public boolean removeDriver(DriverDescriptor driver)
    {
        if (!driver.isCustom()) {
            driver.setDisabled(true);
            return true;
        } else {
            return this.drivers.remove(driver);
        }
    }

    public List<DataSourceViewDescriptor> getViews()
    {
        return views;
    }

    public DataSourceViewDescriptor getView(String targetID)
    {
        for (DataSourceViewDescriptor view : views) {
            if (view.getTargetID().equals(targetID)) {
                return view;
            }
        }
        return null;
    }

    private DBXTreeNode loadTreeInfo(IConfigurationElement config)
    {
        DBXTreeItem treeRoot = new DBXTreeItem(
            this,
            null,
            config.getAttribute(TREE_ATTR_ID),
            "Connection",
            "Connection",
            config.getAttribute(TREE_ATTR_PATH),
            null,
            false,
            false,
            true,
            false,
            config.getAttribute(TREE_ATTR_VISIBLE_IF));
        loadTreeChildren(config, treeRoot);
        loadTreeIcon(treeRoot, config);
        return treeRoot;
    }

    private void loadTreeChildren(IConfigurationElement config, DBXTreeNode parent)
    {
        IConfigurationElement[] children = config.getChildren();
        if (!CommonUtils.isEmpty(children)) {
            for (IConfigurationElement child : children) {
                loadTreeNode(parent, child);
            }
        }
    }

    private void loadTreeNode(DBXTreeNode parent, IConfigurationElement config)
    {
        DBXTreeNode child = null;
        final String refId = config.getAttribute(TREE_ATTR_REF);
        if (!CommonUtils.isEmpty(refId)) {
            child = treeNodeMap.get(refId);
            if (child != null) {
                parent.addChild(child);
            } else {
                log.warn("Bad node reference: " + refId);
            }
        } else {
            String nodeType = config.getName();
            if (nodeType.equals(TREE_NODE_TYPE_FOLDER)) {
                DBXTreeFolder folder = new DBXTreeFolder(
                    this,
                    parent,
                    config.getAttribute(TREE_ATTR_ID),
                    config.getAttribute(TREE_ATTR_TYPE),
                    config.getAttribute(TREE_ATTR_LABEL),
                    CommonUtils.getBoolean(config.getAttribute(TREE_ATTR_NAVIGABLE), true),
                    config.getAttribute(TREE_ATTR_VISIBLE_IF));
                folder.setDescription(config.getAttribute(TREE_ATTR_DESCRIPTION));
                child = folder;
            } else if (nodeType.equals(TREE_NODE_TYPE_ITEMS)) {
                child = new DBXTreeItem(
                    this,
                    parent,
                    config.getAttribute(TREE_ATTR_ID),
                    config.getAttribute(TREE_ATTR_LABEL),
                    config.getAttribute(TREE_ATTR_ITEM_LABEL),
                    config.getAttribute(TREE_ATTR_PATH),
                    config.getAttribute(TREE_ATTR_PROPERTY),
                    CommonUtils.getBoolean(config.getAttribute(TREE_ATTR_OPTIONAL)),
                    CommonUtils.getBoolean(config.getAttribute(TREE_ATTR_VIRTUAL)),
                    CommonUtils.getBoolean(config.getAttribute(TREE_ATTR_NAVIGABLE), true),
                    CommonUtils.getBoolean(config.getAttribute(TREE_ATTR_INLINE)),
                    config.getAttribute(TREE_ATTR_VISIBLE_IF));
            } else if (nodeType.equals(TREE_NODE_TYPE_OBJECT)) {
                child = new DBXTreeObject(
                    this,
                    parent,
                    config.getAttribute(TREE_ATTR_ID),
                    config.getAttribute(TREE_ATTR_VISIBLE_IF),
                    config.getAttribute(TREE_ATTR_LABEL),
                    config.getAttribute(TREE_ATTR_DESCRIPTION),
                    config.getAttribute(TREE_ATTR_EDITOR));
            } else {
                // Unknown node type
                //log.warn("Unknown node type: " + nodeType);
            }

            if (child != null) {
                if (!CommonUtils.isEmpty(child.getId())) {
                    treeNodeMap.put(child.getId(), child);
                }
                loadTreeIcon(child, config);
                loadTreeChildren(config, child);
            }
        }
    }

    private void loadTreeIcon(DBXTreeNode node, IConfigurationElement config)
    {
        String defaultIcon = config.getAttribute(TREE_ATTR_ICON);
        if (defaultIcon == null) {
            defaultIcon = config.getAttribute(TREE_ATTR_ICON_ID);
        }
        IConfigurationElement[] iconElements = config.getChildren(TREE_ATTR_ICON);
        if (!CommonUtils.isEmpty(iconElements)) {
            for (IConfigurationElement iconElement : iconElements) {
                String icon = iconElement.getAttribute(TREE_ATTR_ICON);
                if (icon == null) {
                    icon = iconElement.getAttribute(TREE_ATTR_ICON_ID);
                }
                String expr = iconElement.getAttribute(TREE_ATTR_IF);
                boolean isDefault = CommonUtils.getBoolean(iconElement.getAttribute(TREE_ATTR_DEFAULT));
                if (isDefault && CommonUtils.isEmpty(expr)) {
                    defaultIcon = icon;
                } else {
                    Image iconImage = iconToImage(icon);
                    if (iconImage != null) {
                        node.addIcon(new DBXTreeIcon(expr, iconImage));
                    }
                }
            }
        }
        if (defaultIcon != null) {
            Image defaultImage = iconToImage(defaultIcon);
            if (defaultImage != null) {
                node.setDefaultIcon(defaultImage);
            }
        }
    }

    private DriverDescriptor loadDriver(IConfigurationElement config)
    {
        return new DriverDescriptor(this, config);
    }
}
