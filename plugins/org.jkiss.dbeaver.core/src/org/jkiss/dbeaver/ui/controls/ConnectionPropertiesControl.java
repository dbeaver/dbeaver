/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

import java.util.*;

/**
 * Connection properties control
 */
public class ConnectionPropertiesControl extends PropertyTreeViewer {

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

    protected String[] getCustomCategories()
    {
        return new String[] { USER_PROPERTIES_CATEGORY };
    }

    protected void contributeContextMenu(IMenuManager manager, final Object node, final String category, final IPropertyDescriptor property)
    {
        boolean isCustom = USER_PROPERTIES_CATEGORY.equals(category);
        if (isCustom) {
            manager.add(new Action("Add new property") {
                @Override
                public void run() {
                    createNewProperty(node, category);
                }
            });
            if (property != null) {
                manager.add(new Action("Remove property") {
                    @Override
                    public void run() {
                        removeProperty(node);
                    }
                });
            }
        }
    }

    private void createNewProperty(Object node, String category) {
        // Ask user for new property name
        String propName = EnterNameDialog.chooseName(getControl().getShell(), "Property Name");
        if (propName != null) {
            // Check property name (must be unique
            addProperty(node, new PropertyDescriptorEx(category, propName, propName, null, null, false, null, null, true));
        }
    }

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
            final Collection<IPropertyDescriptor> connectionsProps =
                driver.getDataSourceProvider().getConnectionProperties(driver, connectionInfo);
            driverProvidedProperties = new ArrayList<IPropertyDescriptor>();
            if (connectionsProps != null) {
                driverProvidedProperties.addAll(connectionsProps);
            }
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
        for (Object propId : properties.keySet()) {
            final String propName = propId.toString();
            if (propName.startsWith(DBConstants.INTERNAL_PROP_PREFIX)) {
                continue;
            }
            if (!propNames.contains(propName)) {
                customProperties.add(new PropertyDescriptorEx(
                    USER_PROPERTIES_CATEGORY,
                    propName,
                    propName,
                    null,
                    String.class,
                    false,
                    null,
                    null,
                    true));
            }
        }
    }

}
