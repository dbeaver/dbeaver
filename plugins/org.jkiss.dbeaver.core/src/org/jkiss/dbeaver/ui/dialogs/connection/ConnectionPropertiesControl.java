/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Connection properties control
 */
public class ConnectionPropertiesControl extends PropertyTreeViewer {

    private static final Log log = Log.getLog(ConnectionPropertiesControl.class);

    public static final String USER_PROPERTIES_CATEGORY = CoreMessages.controls_connection_properties_category_user_properties;

    private List<DBPPropertyDescriptor> driverProvidedProperties;
    private List<DBPPropertyDescriptor> customProperties;

    public ConnectionPropertiesControl(Composite parent, int style)
    {
        super(parent, style);
        setExpandSingleRoot(false);
    }

    public PropertySourceCustom makeProperties(DBRProgressMonitor monitor, DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        Map<Object, Object> connectionProps = new HashMap<>();
        connectionProps.putAll(driver.getConnectionProperties());
        connectionProps.putAll(connectionInfo.getProperties());
        driverProvidedProperties = null;
        customProperties = null;

        loadDriverProperties(monitor, driver, connectionInfo);
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
    protected void contributeContextMenu(IMenuManager manager, final Object node, final String category, final DBPPropertyDescriptor property)
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

    @Override
    protected boolean isHidePropertyValue(DBPPropertyDescriptor property) {
        return CommonUtils.toString(property.getId()).toLowerCase(Locale.ENGLISH).contains("password");
    }

    private void createNewProperty(Object node, String category) {
        // Ask user for new property name
        String propName = EnterNameDialog.chooseName(getControl().getShell(), CoreMessages.controls_connection_properties_dialog_new_property_title);
        if (propName != null) {
            // Check property name (must be unique
            addProperty(node, new PropertyDescriptor(category, propName, propName, null, null, false, null, null, true));
        }
    }

    private List<DBPPropertyDescriptor> getAllProperties(DBPDriver driver, boolean includeCustom) {
        List<DBPPropertyDescriptor> propertyDescriptors = new ArrayList<>();
        propertyDescriptors.addAll(driver.getConnectionPropertyDescriptors());
        if (driverProvidedProperties != null) {
            propertyDescriptors.addAll(driverProvidedProperties);
        }
        if (includeCustom && customProperties != null) {
            propertyDescriptors.addAll(customProperties);
        }
        propertyDescriptors.sort(PROPERTIES_COMPARATOR);
        return propertyDescriptors;
    }

    private void loadDriverProperties(DBRProgressMonitor monitor, DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        try {
            final DBPPropertyDescriptor[] connectionsProps =
                driver.getDataSourceProvider().getConnectionProperties(monitor, driver, connectionInfo);
            driverProvidedProperties = new ArrayList<>();
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
        Set<String> propNames = new TreeSet<>();
        List<DBPPropertyDescriptor> allProperties = getAllProperties(driver, false);
        for (DBPPropertyDescriptor prop : allProperties) {
            propNames.add(CommonUtils.toString(prop.getId()));
        }

        customProperties = new ArrayList<>();
        // Find prop values which are not from driver
        for (Object propId : properties.keySet()) {
            final String propName = propId.toString();
            if (!propNames.contains(propName)) {
                customProperties.add(new PropertyDescriptor(
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
        customProperties.sort(PROPERTIES_COMPARATOR);
    }

    private static Comparator<DBPPropertyDescriptor> PROPERTIES_COMPARATOR = new Comparator<DBPPropertyDescriptor>() {
        @Override
        public int compare(DBPPropertyDescriptor o1, DBPPropertyDescriptor o2) {
            return o1.getDisplayName().compareTo(o2.getDisplayName());
        }
    };

}
