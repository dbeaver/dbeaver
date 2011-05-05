/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBPProperty;
import org.jkiss.dbeaver.model.DBPPropertyGroup;
import org.jkiss.dbeaver.registry.PropertyDescriptor;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.properties.EditablePropertiesControl;

import java.util.*;

/**
 * Connection properties control
 */
public class ConnectionPropertiesControl extends EditablePropertiesControl {

    static final Log log = LogFactory.getLog(ConnectionPropertiesControl.class);

    private DBPPropertyGroup driverProvidedProperties;
    private DBPPropertyGroup customProperties;

    public ConnectionPropertiesControl(Composite parent, int style)
    {
        super(parent, style);
        setExpandSingleRoot(false);
    }

    public void loadProperties(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        Map<String, String> connectionProps = new HashMap<String, String>();
        connectionProps.putAll(driver.getConnectionProperties());
        connectionProps.putAll(connectionInfo.getProperties());
        driverProvidedProperties = null;
        customProperties = null;

        loadDriverProperties(driver, connectionInfo);
        loadCustomProperties(driver, connectionProps);

        super.loadProperties(getAllPropertyGroups(driver, true), connectionProps);
    }

    public void loadProperties(DBPDriver driver, Map<String, String> properties)
    {
        driverProvidedProperties = null;
        customProperties = null;

        loadCustomProperties(driver, properties);

        super.loadProperties(getAllPropertyGroups(driver, true), properties);
    }

    protected boolean isCustomProperty(DBPProperty property)
    {
        return property.getGroup() instanceof CustomPropertyGroup;
    }

    protected void contributeContextMenu(IMenuManager manager, final Object selectedObject)
    {
        boolean isCustom = false;
        if (selectedObject instanceof CustomPropertyGroup) {
            isCustom = true;
        } else if (selectedObject instanceof DBPProperty && ((DBPProperty)selectedObject).getGroup() instanceof CustomPropertyGroup) {
            isCustom = true;
        }
        if (isCustom) {
            manager.add(new Action("Add new property") {
                @Override
                public void run() {
                    addNewProperty(selectedObject);
                }
            });
            if (selectedObject instanceof DBPProperty) {
                manager.add(new Action("Remove property") {
                    @Override
                    public void run() {
                        removeProperty((DBPProperty)selectedObject);
                    }
                });
            }
        }
    }

    private void addNewProperty(Object parent) {
        CustomPropertyGroup customGroup = null;
        if (parent instanceof DBPProperty && ((DBPProperty)parent).getGroup() instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)((DBPProperty)parent).getGroup();
        } else if (parent instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)parent;
        }
        if (customGroup != null) {
            // Ask user for new property name
            String propName = EnterNameDialog.chooseName(getShell(), "Property Name");
            if (propName != null) {
                // Check property name (must be unique
                PropertyDescriptor newProp = customGroup.addProperty(propName);
                handlePropertyCreate(newProp, "");
            }
        }
    }

    private void removeProperty(DBPProperty prop) {
        CustomPropertyGroup customGroup = null;
        if (prop.getGroup() instanceof CustomPropertyGroup) {
            customGroup = (CustomPropertyGroup)prop.getGroup();
        }
        if (customGroup != null) {
            customGroup.removeProperty(prop);
            handlePropertyRemove(prop);
        }

    }

    private List<DBPPropertyGroup> getAllPropertyGroups(DBPDriver driver, boolean includeCustom) {
        List<DBPPropertyGroup> groups = new ArrayList<DBPPropertyGroup>();
        groups.addAll(driver.getConnectionPropertyGroups());
        if (driverProvidedProperties != null) {
            groups.add(driverProvidedProperties);
        }
        if (includeCustom && customProperties != null) {
            groups.add(customProperties);
        }
        return groups;
    }

    private void loadDriverProperties(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        try {
            driverProvidedProperties = driver.getDataSourceProvider().getConnectionProperties(driver, connectionInfo);
        } catch (DBException e) {
            log.warn("Can't load driver properties", e);
        }
    }

    private void loadCustomProperties(DBPDriver driver, Map<String, String> properties)
    {
        // Custom properties are properties which are came not from driver and not from
        Set<String> customNames = new TreeSet<String>();

        // Collect all driver (and all other) properties
        Set<String> propNames = new TreeSet<String>();
        List<DBPPropertyGroup> allGroups = getAllPropertyGroups(driver, false);
        for (DBPPropertyGroup group : allGroups) {
            for (DBPProperty prop : group.getProperties()) {
                propNames.add(prop.getName());
            }
        }

        // Find prop values which are not from driver
        for (Object propName : properties.keySet()) {
            if (!propNames.contains(propName.toString())) {
                customNames.add(propName.toString());
            }
        }
        customProperties = new CustomPropertyGroup(customNames);
    }

    private class CustomPropertyGroup implements DBPPropertyGroup
    {
        private List<DBPProperty> propList;

        public CustomPropertyGroup(Set<String> propNames)
        {
            this.propList = new ArrayList<DBPProperty>();
            if (propNames != null) {
                for (String name : propNames) {
                    propList.add(new PropertyDescriptor(this, name, name, null, DBPProperty.PropertyType.STRING, false, null, null));
                }
            }
        }

        public String getName()
        {
            return "User Properties";
        }

        public String getDescription()
        {
            return "User defined properties";
        }

        public List<? extends DBPProperty> getProperties()
        {
            return propList;
        }

        public PropertyDescriptor addProperty(String name) {
            PropertyDescriptor prop = new PropertyDescriptor(this, name, name, null, DBPProperty.PropertyType.STRING, false, null, null);
            propList.add(prop);
            return prop;
        }

        public void removeProperty(DBPProperty prop) {
            propList.remove(prop);
        }
    }


}
