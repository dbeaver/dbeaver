/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.properties.EditablePropertyTree;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

import java.util.*;

/**
 * Connection properties control
 */
public class ConnectionPropertiesControl extends EditablePropertyTree {

    static final Log log = LogFactory.getLog(ConnectionPropertiesControl.class);

    public static final String USER_PROPERTIES_CATEGORY = "User Properties";

    private List<IPropertyDescriptor> driverProvidedProperties;
    private List<IPropertyDescriptor> customProperties;

    public ConnectionPropertiesControl(Composite parent, int style)
    {
        super(parent, style);
        setExpandSingleRoot(false);
    }

    public PropertySourceCustom makeProperties(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        Map<Object, Object> connectionProps = new HashMap<Object, Object>();
        connectionProps.putAll(driver.getConnectionProperties());
        connectionProps.putAll(connectionInfo.getProperties());
        driverProvidedProperties = null;
        customProperties = null;

        loadDriverProperties(driver, connectionInfo);
        loadCustomProperties(driver, connectionProps);

        return new PropertySourceCustom(
            getAllProperties(driver, true),
            connectionProps);
    }

    public PropertySourceCustom makeProperties(DBPDriver driver, Map<Object, Object> properties)
    {
        driverProvidedProperties = null;
        customProperties = null;

        loadCustomProperties(driver, properties);

        return new PropertySourceCustom(
            getAllProperties(driver, true),
            properties);
    }

    protected boolean isCustomProperty(IPropertyDescriptor property)
    {
        return USER_PROPERTIES_CATEGORY.equals(property.getCategory());
    }

/*
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
*/

    private List<IPropertyDescriptor> getAllProperties(DBPDriver driver, boolean includeCustom) {
        List<IPropertyDescriptor> propertyDescriptors = new ArrayList<IPropertyDescriptor>();
        propertyDescriptors.addAll(driver.getConnectionPropertyDescriptors());
        if (driverProvidedProperties != null) {
            propertyDescriptors.addAll(driverProvidedProperties);
        }
        if (includeCustom && customProperties != null) {
            propertyDescriptors.addAll(customProperties);
        }
        return propertyDescriptors;
    }

    private void loadDriverProperties(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        try {
            driverProvidedProperties = new ArrayList<IPropertyDescriptor>(
                driver.getDataSourceProvider().getConnectionProperties(driver, connectionInfo));
        } catch (DBException e) {
            log.warn("Can't load driver properties", e);
        }
    }

    private void loadCustomProperties(DBPDriver driver, Map<Object, Object> properties)
    {
        // Collect all driver (and all other) properties
        Set<String> propNames = new TreeSet<String>();
        List<IPropertyDescriptor> allProperties = getAllProperties(driver, false);
        for (IPropertyDescriptor prop : allProperties) {
            propNames.add(CommonUtils.toString(prop.getId()));
        }

        customProperties = new ArrayList<IPropertyDescriptor>();
        // Find prop values which are not from driver
        for (Object propName : properties.keySet()) {
            if (!propNames.contains(propName.toString())) {
                customProperties.add(new PropertyDescriptor(
                    USER_PROPERTIES_CATEGORY,
                    propName.toString(),
                    propName.toString(),
                    null,
                    String.class,
                    false,
                    null,
                    null));
            }
        }
    }

}
