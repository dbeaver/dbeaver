/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.SecurityUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.registry.tree.*;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * DataSourceProviderDescriptor
 */
public class DataSourceProviderDescriptor extends AbstractDescriptor
{
    static final Log log = LogFactory.getLog(DataSourceProviderDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceProvider";

    private DataSourceProviderRegistry registry;
    private String id;
    private String implClassName;
    private String name;
    private String description;
    private Image icon;
    private DBPDataSourceProvider instance;
    private DBXTreeNode treeDescriptor;
    private boolean driversManagable;
    private List<PropertyGroupDescriptor> driverPropertyGroups = new ArrayList<PropertyGroupDescriptor>();
    private List<DriverDescriptor> drivers = new ArrayList<DriverDescriptor>();
    private List<DataSourceViewDescriptor> views = new ArrayList<DataSourceViewDescriptor>();

    public DataSourceProviderDescriptor(DataSourceProviderRegistry registry, IConfigurationElement config)
    {
        super(config.getContributor());
        this.registry = registry;

        this.id = config.getAttribute("id");
        this.implClassName = config.getAttribute("class");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        String iconName = config.getAttribute("icon");
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
                IConfigurationElement[] propElements = propsElement.getChildren(PropertyGroupDescriptor.PROPERTY_GROUP_TAG);
                for (IConfigurationElement prop : propElements) {
                    driverPropertyGroups.add(new PropertyGroupDescriptor(prop));
                }
            }
        }

        // Load supplied drivers
        IConfigurationElement[] driversGroup = config.getChildren("drivers");
        if (!CommonUtils.isEmpty(driversGroup)) {
            for (IConfigurationElement driversElement : driversGroup) {
                this.driversManagable = driversElement.getAttribute("managable") == null || "true".equals(driversElement.getAttribute(
                    "managable"));
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

    public List<PropertyGroupDescriptor> getDriverPropertyGroups()
    {
        return driverPropertyGroups;
    }

    public List<DriverDescriptor> getDrivers()
    {
        return drivers;
    }

    public List<DriverDescriptor> getEnabledDrivers()
    {
        List<DriverDescriptor> eDrivers = new ArrayList<DriverDescriptor>();
        for (DriverDescriptor driver : drivers) {
            if (!driver.isDisabled()) {
                eDrivers.add(driver);
            }
        }
        return eDrivers;
    }

    public DriverDescriptor getDriver(String id)
    {
        for (DriverDescriptor driver : drivers) {
            if (driver.getId().equals(id)) {
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
            "Connection",
            "Connection",
            config.getAttribute("path"),
            null,
            false,
            false,
            true,
            false);
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
        String nodeType = config.getName();
        if (nodeType.equals("folder")) {
            DBXTreeFolder folder = new DBXTreeFolder(
                this,
                parent,
                config.getAttribute("type"),
                config.getAttribute("label"),
                !"false".equals(config.getAttribute("navigable")));
            folder.setDescription(config.getAttribute("description"));
            child = folder;
        } else if (nodeType.equals("items")) {
            child = new DBXTreeItem(
                this,
                parent,
                config.getAttribute("label"),
                config.getAttribute("itemLabel"),
                config.getAttribute("path"),
                config.getAttribute("property"),
                "true".equals(config.getAttribute("optional")),
                "true".equals(config.getAttribute("virtual")),
                !"false".equals(config.getAttribute("navigable")),
                "true".equals(config.getAttribute("inline")));
        } else if (nodeType.equals("object")) {
            child = new DBXTreeObject(
                this,
                parent,
                config.getAttribute("label"),
                config.getAttribute("description"),
                config.getAttribute("editor"));
        } else {
            // Unknown node type
        }
        if (child != null) {
            loadTreeIcon(child, config);
            loadTreeChildren(config, child);
        }
    }

    private void loadTreeIcon(DBXTreeNode node, IConfigurationElement config)
    {
        String defaultIcon = config.getAttribute("icon");
        if (defaultIcon == null) {
            defaultIcon = config.getAttribute("iconId");
        }
        IConfigurationElement[] iconElements = config.getChildren("icon");
        if (!CommonUtils.isEmpty(iconElements)) {
            for (IConfigurationElement iconElement : iconElements) {
                String icon = iconElement.getAttribute("icon");
                if (icon == null) {
                    icon = iconElement.getAttribute("iconId");
                }
                String expr = iconElement.getAttribute("if");
                boolean isDefault = "true".equals(iconElement.getAttribute("default"));
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
