/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Connection properties control
 */
public class ConnectionPropertiesControl extends PropertyTreeViewer {

    private static final Log log = Log.getLog(ConnectionPropertiesControl.class);

    private static final String USER_PROPERTIES_CATEGORY = UIConnectionMessages.controls_connection_properties_category_user_properties;

    private List<DBPPropertyDescriptor> driverProvidedProperties;
    private List<DBPPropertyDescriptor> customProperties;

    public ConnectionPropertiesControl(Composite parent, int style)
    {
        super(parent, style);
        setExpandSingleRoot(false);
        setNewPropertiesAllowed(true);
        //setNamesEditable(true);
    }

    PropertySourceCustom makeProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull DBPConnectionConfiguration connectionInfo,
        @Nullable DBPDriverSubstitutionDescriptor driverSubstitution
    ) {
        Map<String, Object> connectionProps = new HashMap<>();
        connectionProps.putAll(driver.getConnectionProperties());
        connectionProps.putAll(connectionInfo.getProperties());
        driverProvidedProperties = null;
        customProperties = null;

        if (driverSubstitution == null) {
            // for now, we do not support reading driver properties from substitution drivers
            loadDriverProperties(monitor, driver, connectionInfo);
        }
        loadCustomProperties(driver, connectionProps);

        return new PropertySourceCustom(
            getAllProperties(driver, true),
            connectionProps);
    }

    public PropertySourceCustom makeProperties(DBPDriver driver, Map<String, ?> properties)
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
            manager.add(new Action(UIConnectionMessages.controls_connection_properties_action_add_property) {
                @Override
                public void run() {
                    createNewProperty(node, category);
                }
            });
            if (property != null) {
                manager.add(new Action(UIConnectionMessages.controls_connection_properties_action_remove_property) {
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
        Class<?> dataType = property.getDataType();
        if (dataType != null && !(String.class.isAssignableFrom(dataType))) {
            return false;
        }
        String propName = CommonUtils.toString(property.getId()).toLowerCase(Locale.ENGLISH);
        return propName.contains("password") || propName.contains("token");
    }

    private void createNewProperty(Object node, String category) {
        // Ask user for new property name
        String propName = EnterNameDialog.chooseName(getControl().getShell(), UIConnectionMessages.controls_connection_properties_dialog_new_property_title);
        if (propName != null) {
            // Check property name (must be unique
            addProperty(node, new PropertyDescriptor(category, propName, propName, null, null, false, null, null, true), true);
        }
    }

    private List<DBPPropertyDescriptor> getAllProperties(DBPDriver driver, boolean includeCustom) {
        List<DBPPropertyDescriptor> propertyDescriptors = new ArrayList<>();
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

    private void loadCustomProperties(DBPDriver driver, Map<?, ?> properties)
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

    private static Comparator<DBPPropertyDescriptor> PROPERTIES_COMPARATOR = Comparator.comparing(DBPPropertyDescriptor::getDisplayName);

    void createPropertiesToolBar(Composite parent) {
        ToolBar toolBar = new ToolBar(parent, SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        toolBar.setLayoutData(gd);

        ToolItem addItem = new ToolItem(toolBar, SWT.NONE);
        addItem.setImage(DBeaverIcons.getImage(UIIcon.ROW_ADD));
        addItem.setToolTipText("Add user property");
        addItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewProperty(getCategoryNode(USER_PROPERTIES_CATEGORY), USER_PROPERTIES_CATEGORY);
            }
        });

        ToolItem removeItem = new ToolItem(toolBar, SWT.NONE);
        removeItem.setImage(DBeaverIcons.getImage(UIIcon.ROW_DELETE));
        removeItem.setToolTipText("Remove user property");
        removeItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ISelection selection = getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    removeProperty(((IStructuredSelection) selection).getFirstElement());
                }
            }
        });
        removeItem.setEnabled(false);

        addSelectionChangedListener(event -> {
            addItem.setEnabled(getCategoryNode(USER_PROPERTIES_CATEGORY) != null);
            boolean hasDelete = false;
            if (USER_PROPERTIES_CATEGORY.equals(getSelectedCategory())) {
                hasDelete = true;
            }
            removeItem.setEnabled(hasDelete);
        });
    }

}
