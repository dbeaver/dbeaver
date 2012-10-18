/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.registry.tree.*;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

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

    private DataSourceProviderRegistry registry;
    private final String id;
    private final ObjectType implType;
    private final String name;
    private final String description;
    private Image icon;
    private DBPDataSourceProvider instance;
    private DBXTreeNode treeDescriptor;
    private final Map<String, DBXTreeNode> treeNodeMap = new HashMap<String, DBXTreeNode>();
    private boolean driversManagable;
    private final List<IPropertyDescriptor> driverProperties = new ArrayList<IPropertyDescriptor>();
    private final List<DriverDescriptor> drivers = new ArrayList<DriverDescriptor>();
    private final List<DataSourceViewDescriptor> views = new ArrayList<DataSourceViewDescriptor>();
    private final List<DataSourceToolDescriptor> tools = new ArrayList<DataSourceToolDescriptor>();

    public DataSourceProviderDescriptor(DataSourceProviderRegistry registry, IConfigurationElement config)
    {
        super(config.getContributor());
        this.registry = registry;

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        String iconName = config.getAttribute(RegistryConstants.ATTR_ICON);
        if (!CommonUtils.isEmpty(iconName)) {
            this.icon = iconToImage(iconName);
        }
        if (this.icon == null) {
            this.icon = DBIcon.GEN_DATABASE_TYPE.getImage();
        }

        // Load tree structure
        IConfigurationElement[] trees = config.getChildren(RegistryConstants.TAG_TREE);
        if (!CommonUtils.isEmpty(trees)) {
            this.treeDescriptor = this.loadTreeInfo(trees[0]);
        }

        // Load driver properties
        {
            for (IConfigurationElement propsElement : config.getChildren(RegistryConstants.TAG_DRIVER_PROPERTIES)) {
                for (IConfigurationElement prop : propsElement.getChildren(PropertyDescriptorEx.TAG_PROPERTY_GROUP)) {
                    driverProperties.addAll(PropertyDescriptorEx.extractProperties(prop));
                }
            }
        }

        // Load supplied drivers
        {
            for (IConfigurationElement driversElement : config.getChildren(RegistryConstants.TAG_DRIVERS)) {
                this.driversManagable = driversElement.getAttribute(RegistryConstants.ATTR_MANAGABLE) == null ||
                    CommonUtils.getBoolean(driversElement.getAttribute(RegistryConstants.ATTR_MANAGABLE));
                for (IConfigurationElement driverElement : driversElement.getChildren(RegistryConstants.TAG_DRIVER)) {
                    this.drivers.add(loadDriver(driverElement));
                }
            }
        }

        // Load views
        {
            for (IConfigurationElement viewsElement : config.getChildren(RegistryConstants.TAG_VIEWS)) {
                for (IConfigurationElement viewElement : viewsElement.getChildren(RegistryConstants.TAG_VIEW)) {
                    this.views.add(
                        new DataSourceViewDescriptor(this, viewElement));
                }
            }
        }

        // Load views
        {
            for (IConfigurationElement toolsElement : config.getChildren(RegistryConstants.TAG_TOOLS)) {
                for (IConfigurationElement toolElement : toolsElement.getChildren(RegistryConstants.TAG_TOOL)) {
                    this.tools.add(
                        new DataSourceToolDescriptor(this, toolElement));
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
            Class<?> implClass = implType.getObjectClass();
            if (implClass == null) {
                throw new DBException("Can't find descriptor class '" + implType.implName + "'");
            }
            try {
                this.instance = (DBPDataSourceProvider) implClass.newInstance();
            }
            catch (Throwable ex) {
                throw new DBException("Can't instantiate data source provider '" + implClass.getName() + "'", ex);
            }
            // Initialize it
            try {
                this.instance.init(DBeaverCore.getInstance());
            }
            catch (Throwable ex) {
                this.instance = null;
                throw new DBException("Can't initialize data source provider '" + implClass.getName() + "'", ex);
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

    public DataSourceViewDescriptor getView(String targetID)
    {
        for (DataSourceViewDescriptor view : views) {
            if (view.getTargetID().equals(targetID)) {
                return view;
            }
        }
        return null;
    }

    public List<DataSourceToolDescriptor> getTools(DBPObject object)
    {
        List<DataSourceToolDescriptor> result = new ArrayList<DataSourceToolDescriptor>();
        for (DataSourceToolDescriptor descriptor : tools) {
            if (descriptor.appliesTo(object)) {
                result.add(descriptor);
            }
        }
        return result;
    }

    private DBXTreeNode loadTreeInfo(IConfigurationElement config)
    {
        DBXTreeItem treeRoot = new DBXTreeItem(
            this,
            null,
            config.getAttribute(RegistryConstants.ATTR_ID),
            CoreMessages.model_navigator_Connection,
            CoreMessages.model_navigator_Connection,
            config.getAttribute(RegistryConstants.ATTR_PATH),
            null,
            false,
            false,
            true,
            false,
            config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF));
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
            if (nodeType.equals(RegistryConstants.TAG_FOLDER)) {
                DBXTreeFolder folder = new DBXTreeFolder(
                    this,
                    parent,
                    config.getAttribute(RegistryConstants.ATTR_ID),
                    config.getAttribute(RegistryConstants.ATTR_TYPE),
                    config.getAttribute(RegistryConstants.ATTR_LABEL),
                    CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_NAVIGABLE), true),
                    config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF));
                folder.setDescription(config.getAttribute(RegistryConstants.ATTR_DESCRIPTION));
                child = folder;
            } else if (nodeType.equals(RegistryConstants.TAG_ITEMS)) {
                child = new DBXTreeItem(
                    this,
                    parent,
                    config.getAttribute(RegistryConstants.ATTR_ID),
                    config.getAttribute(RegistryConstants.ATTR_LABEL),
                    config.getAttribute(RegistryConstants.ATTR_ITEM_LABEL),
                    config.getAttribute(RegistryConstants.ATTR_PATH),
                    config.getAttribute(RegistryConstants.ATTR_PROPERTY),
                    CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_OPTIONAL)),
                    CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_VIRTUAL)),
                    CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_NAVIGABLE), true),
                    CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_INLINE)),
                    config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF));
            } else if (nodeType.equals(RegistryConstants.TAG_OBJECT)) {
                child = new DBXTreeObject(
                    this,
                    parent,
                    config.getAttribute(RegistryConstants.ATTR_ID),
                    config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF),
                    config.getAttribute(RegistryConstants.ATTR_LABEL),
                    config.getAttribute(RegistryConstants.ATTR_DESCRIPTION),
                    config.getAttribute(RegistryConstants.ATTR_EDITOR));
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
        String defaultIcon = config.getAttribute(RegistryConstants.ATTR_ICON);
        if (defaultIcon == null) {
            defaultIcon = config.getAttribute(RegistryConstants.ATTR_ICON_ID);
        }
        IConfigurationElement[] iconElements = config.getChildren(RegistryConstants.ATTR_ICON);
        if (!CommonUtils.isEmpty(iconElements)) {
            for (IConfigurationElement iconElement : iconElements) {
                String icon = iconElement.getAttribute(RegistryConstants.ATTR_ICON);
                if (icon == null) {
                    icon = iconElement.getAttribute(RegistryConstants.ATTR_ICON_ID);
                }
                String expr = iconElement.getAttribute(RegistryConstants.ATTR_IF);
                boolean isDefault = CommonUtils.getBoolean(iconElement.getAttribute(RegistryConstants.ATTR_DEFAULT));
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
