/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.internal.DebugConfigurationPanelDescriptor;
import org.jkiss.dbeaver.debug.internal.DebugConfigurationPanelRegistry;
import org.jkiss.dbeaver.debug.internal.ui.DebugUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.SelectDataSourceCombo;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseDebugConfigurationTab extends AbstractLaunchConfigurationTab {

    private DebugConfigurationPanelDescriptor selectedDebugType;
    private DBGConfigurationPanel selectedDebugPanel;
    private ILaunchConfiguration currentConfiguration;

    private Text driverText;

    /**
     * Modify listener that simply updates the owning launch configuration
     * dialog.
     */
    protected ModifyListener modifyListener = evt -> scheduleUpdateJob();
    private SelectDataSourceCombo connectionCombo;
    private Group typesGroup;
    private Composite panelPlaceholder;

    @Override
    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
        comp.setLayout(new GridLayout(1, true));
        comp.setFont(parent.getFont());

        createComponents(comp);
    }

    protected void createComponents(Composite comp) {
        createConnectionSettingsGroup(comp);
        createPanelListGroup(comp);
    }

    protected void createConnectionSettingsGroup(Composite composite) {
        Group group = UIUtils.createControlGroup(composite, DebugUIMessages.DatabaseTab_connection_group_text, 4, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        UIUtils.createControlLabel(group, DebugUIMessages.DatabaseTab_datasource_label_text);
        connectionCombo = new SelectDataSourceCombo(group) {
            @Override
            protected IProject getActiveProject() {
                return null;
            }

            @Override
            protected void onDataSourceChange(DBPDataSourceContainer dataSource) {
                String driverName = dataSource == null ? "" : dataSource.getDriver().getFullName();
                driverText.setText(driverName);
                loadConnectionDebugTypes();
                scheduleUpdateJob();
            }
        };
        connectionCombo.addItem(null);
        for (DBPDataSourceContainer ds : DataSourceRegistry.getAllDataSources()) {
            connectionCombo.addItem(ds);
        }

        driverText = UIUtils.createLabelText(group, DebugUIMessages.DatabaseTab_driver_label_text, "", SWT.READ_ONLY);
    }

    protected void createPanelListGroup(Composite composite) {
        typesGroup = UIUtils.createControlGroup(composite, DebugUIMessages.DatabaseTab_debug_type_group_text, 3, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        panelPlaceholder = UIUtils.createControlGroup(composite, DebugUIMessages.DatabaseTab_configuration_group_text, 1, GridData.FILL_BOTH, SWT.DEFAULT);
        loadConnectionDebugTypes();
    }

    private void loadConnectionDebugTypes() {
        for (Control c : typesGroup.getChildren()) {
            c.dispose();
        }

        DBPDataSourceContainer dataSource = connectionCombo.getSelectedItem();
        if (dataSource == null) {
            UIUtils.createInfoLabel(typesGroup, "Select a connection to see available debug types");
        } else {
            List<DebugConfigurationPanelDescriptor> panels = DebugConfigurationPanelRegistry.getInstance().getPanels(dataSource);
            if (CommonUtils.isEmpty(panels)) {
                UIUtils.createInfoLabel(typesGroup, "Driver '" + dataSource.getDriver().getFullName() + "' doesn't support debugging");
            } else {
                for (DebugConfigurationPanelDescriptor panel : panels) {
                    Button typeSelector = new Button(typesGroup, SWT.RADIO);
                    typeSelector.setText(panel.getName());
                    if (!CommonUtils.isEmpty(panel.getDescription())) {
                        typeSelector.setToolTipText(panel.getDescription());
                    }
                    typeSelector.setData(panel);
                    if (panel.isValid()) {
                        typeSelector.addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                if (typeSelector.getSelection()) {
                                    setDebugType(connectionCombo.getSelectedItem(), (DebugConfigurationPanelDescriptor) typeSelector.getData());
                                    updateLaunchConfigurationDialog();
                                }
                            }
                        });
                    } else {
                        typeSelector.setEnabled(false);
                    }
                }
            }
        }
        setDebugType(dataSource, null);
        typesGroup.getParent().layout(true, true);
    }

    private void setDebugType(DBPDataSourceContainer dataSource, DebugConfigurationPanelDescriptor debugPanel) {
        if (selectedDebugType == debugPanel) {
            return;
        }
        for (Control c : panelPlaceholder.getChildren()) {
            c.dispose();
        }
        if (debugPanel != null) {
            try {
                selectedDebugType = debugPanel;
                selectedDebugPanel = debugPanel.createPanel();
                selectedDebugPanel.createPanel(panelPlaceholder);
                if (dataSource != null && currentConfiguration != null) {
                    try {
                        selectedDebugPanel.loadConfiguration(dataSource, currentConfiguration.getAttributes());
                    } catch (CoreException e) {
                        setWarningMessage("Error loading panel configuration: " + e.getMessage());
                    }
                }
            } catch (DBException e) {
                selectedDebugType = null;
                selectedDebugPanel = null;
                DBUserInterface.getInstance().showError("Panel create error", "Can't create debugger config panel " + debugPanel.getId(), e);
            }
        } else {
            selectedDebugType = null;
            selectedDebugPanel = null;
        }
        if (selectedDebugType == null) {
            UIUtils.createInfoLabel(panelPlaceholder, "Select a debug type to see debug configuration");
        } else {
            for (Control c : typesGroup.getChildren()) {
                if (c instanceof Button && c.getData() == debugPanel) {
                    ((Button) c).setSelection(true);
                    break;
                }
            }
        }
        updateLaunchConfigurationDialog();
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        this.currentConfiguration = configuration;
        try {
            String dsId = configuration.getAttribute(DBGConstants.ATTR_DATASOURCE_ID, (String) null);
            DataSourceDescriptor dataSource = DataSourceRegistry.findDataSource(dsId);
            connectionCombo.select(dataSource);
            if (dataSource != null) {
                driverText.setText(dataSource.getDriver().getFullName());
            }
            loadConnectionDebugTypes();

            String typeId = configuration.getAttribute(DBGConstants.ATTR_DEBUG_TYPE, (String) null);
            DebugConfigurationPanelDescriptor savedPanel = null;
            if (typeId != null) {
                savedPanel = DebugConfigurationPanelRegistry.getInstance().getPanel(typeId);
                if (savedPanel == null) {
                    setWarningMessage("Debug type '" + typeId + "' cannot be resolved");
                }
            }
            setDebugType(dataSource, savedPanel);

        } catch (CoreException e) {
            setWarningMessage("Error loading debug configuration: " + e.getMessage());
        }
        updateLaunchConfigurationDialog();
    }

    @Override
    public Image getImage() {
        DBPImage image = extractDriverImage();
        if (image == null) {
            image = DBIcon.TREE_DATABASE;
        }
        return DBeaverIcons.getImage(image);
    }

    protected DBPImage extractDriverImage() {
        if (driverText == null || driverText.isDisposed()) {
            return null;
        }
        String driverName = driverText.getText();
        DriverDescriptor driver = DataSourceProviderRegistry.getInstance().findDriver(driverName);
        if (driver == null) {
            return null;
        }
        return driver.getIcon();
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        DBPDataSourceContainer dataSource = connectionCombo.getSelectedItem();
        configuration.setAttribute(DBGConstants.ATTR_DATASOURCE_ID, dataSource == null ? null : dataSource.getId());
        configuration.setAttribute(DBGConstants.ATTR_DEBUG_TYPE, selectedDebugType == null ? null : selectedDebugType.getId());
        if (selectedDebugPanel != null) {
            Map<String, Object> attrs = new HashMap<>();
            selectedDebugPanel.saveConfiguration(dataSource, attrs);
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                if (entry.getValue() == null) {
                    configuration.removeAttribute(entry.getKey());
                } else if (entry.getValue() instanceof Integer) {
                    configuration.setAttribute(entry.getKey(), (Integer)entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    configuration.setAttribute(entry.getKey(), (Boolean)entry.getValue());
                } else {
                    configuration.setAttribute(entry.getKey(), entry.getValue().toString());
                }
            }
        }
    }

    @Override
    public String getName() {
        return DebugUIMessages.DatabaseTab_name;
    }

    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        return connectionCombo.getSelectedItem() != null && selectedDebugType != null && selectedDebugPanel.isValid();
    }

    @Override
    public boolean canSave() {
        return connectionCombo.getSelectedItem() != null && selectedDebugType != null;
    }
}
