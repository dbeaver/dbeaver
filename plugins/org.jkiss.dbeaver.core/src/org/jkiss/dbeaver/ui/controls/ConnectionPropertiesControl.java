/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ui.controls;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Connection properties control
 */
public class ConnectionPropertiesControl extends PropertyTreeViewer {

    static final Log log = Log.getLog(ConnectionPropertiesControl.class);

    public static final String USER_PROPERTIES_CATEGORY = CoreMessages.controls_connection_properties_category_user_properties;

    private List<IPropertyDescriptor> driverProvidedProperties;
    private List<IPropertyDescriptor> customProperties;

    public ConnectionPropertiesControl(Composite parent, int style)
    {
        super(parent, style);
        setExpandSingleRoot(false);
    }

    public PropertySourceCustom makeProperties(IRunnableContext runnableContext, DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        Map<Object, Object> connectionProps = new HashMap<Object, Object>();
        connectionProps.putAll(driver.getConnectionProperties());
        connectionProps.putAll(connectionInfo.getProperties());
        driverProvidedProperties = null;
        customProperties = null;

        loadDriverProperties(runnableContext, driver, connectionInfo);
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

    @Override
    protected String[] getCustomCategories()
    {
        return new String[] { USER_PROPERTIES_CATEGORY };
    }

    @Override
    protected void contributeContextMenu(IMenuManager manager, final Object node, final String category, final IPropertyDescriptor property)
    {
        boolean isCustom = USER_PROPERTIES_CATEGORY.equals(category);
        if (isCustom) {
            manager.add(new Action(CoreMessages.controls_connection_properties_action_add_property) {
                @Override
                public void run() {
                    createNewProperty(node, category);
                }
            });
            if (property != null) {
                manager.add(new Action(CoreMessages.controls_connection_properties_action_remove_property) {
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
        String propName = EnterNameDialog.chooseName(getControl().getShell(), CoreMessages.controls_connection_properties_dialog_new_property_title);
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

    private void loadDriverProperties(IRunnableContext runnableContext, DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        try {
            final IPropertyDescriptor[] connectionsProps =
                driver.getDataSourceProvider().getConnectionProperties(runnableContext, driver, connectionInfo);
            driverProvidedProperties = new ArrayList<IPropertyDescriptor>();
            if (connectionsProps != null) {
                Collections.addAll(driverProvidedProperties, connectionsProps);
            }
        } catch (DBException e) {
            log.warn("Can't load driver properties", e); //$NON-NLS-1$
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
