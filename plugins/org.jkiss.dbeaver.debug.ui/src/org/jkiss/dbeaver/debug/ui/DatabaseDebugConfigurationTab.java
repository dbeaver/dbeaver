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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.internal.ui.DebugUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartSelector;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.controls.SelectDataSourceCombo;
import org.jkiss.dbeaver.ui.perspective.DataSourceManagementToolbar;
import org.jkiss.utils.CommonUtils;

public class DatabaseDebugConfigurationTab extends AbstractLaunchConfigurationTab {

    private Text driverText;
    private Text oidText;
    private Text nameText;

    private Button attachLocal;
    private Button attachGlobal;
    private Text processText;
    private Text scriptText;

    /**
     * Modify listener that simply updates the owning launch configuration
     * dialog.
     */
    protected ModifyListener modifyListener = evt -> scheduleUpdateJob();
    private SelectDataSourceCombo connectionCombo;

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
        createDatabaseSettingsGroup(comp);
        createAttachSettingsGroup(comp);
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
            }
        };
        connectionCombo.addItem(null);
        for (DBPDataSourceContainer ds : DataSourceRegistry.getAllDataSources()) {
            connectionCombo.addItem(ds);
        }

        driverText = UIUtils.createLabelText(group, DebugUIMessages.DatabaseTab_driver_label_text, "", SWT.READ_ONLY);
    }

    protected void createDatabaseSettingsGroup(Composite composite) {
        String groupLabel = DebugUIMessages.DatabaseTab_procedure_group_text;
        Group group = UIUtils.createControlGroup(composite, groupLabel, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        {
            String label = DebugUIMessages.DatabaseTab_oid_label_text;
            oidText = UIUtils.createLabelText(group, label, "", SWT.READ_ONLY);
        }
        {
            String label = DebugUIMessages.DatabaseTab_name_label_text;
            nameText = UIUtils.createLabelText(group, label, "", SWT.READ_ONLY);
        }
    }

    protected void createAttachSettingsGroup(Composite composite) {
        String groupLabel = DebugUIMessages.DatabaseTab_attach_group_text;
        Group group = UIUtils.createControlGroup(composite, groupLabel, 2,
                GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL, SWT.DEFAULT);
        group.setLayout(new GridLayout(3, false));

        SelectionListener attachKindListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                Object data = event.widget.getData();
                String attachKind = String.valueOf(data);
                handleAttachKind(attachKind);
            }
        };

        attachLocal = new Button(group, SWT.RADIO);
        attachLocal.setData(DBGController.ATTACH_KIND_LOCAL);
        attachLocal.setText("Local");
        attachLocal.addSelectionListener(attachKindListener);
        attachLocal.setLayoutData(GridDataFactory.swtDefaults().span(3, 1).create());

        attachGlobal = new Button(group, SWT.RADIO);
        attachGlobal.setData(DBGController.ATTACH_KIND_GLOBAL);
        attachGlobal.setText("Global");
        attachGlobal.addSelectionListener(attachKindListener);
        attachGlobal.setLayoutData(GridDataFactory.swtDefaults().span(1, 1).create());

        processText = UIUtils.createLabelText(group, "PID", "", SWT.READ_ONLY,
                GridDataFactory.swtDefaults().span(1, 1).create());
        processText.addModifyListener(modifyListener);

        scriptText = new Text(group, SWT.MULTI);
        scriptText.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).grab(true, true).create());
        scriptText.addModifyListener(modifyListener);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        try {
            String dsId = configuration.getAttribute(DBGConstants.ATTR_DATASOURCE_ID, (String) null);
            DataSourceDescriptor dataSource = DataSourceRegistry.findDataSource(dsId);
            connectionCombo.select(dataSource);
            if (dataSource != null) {
                driverText.setText(dataSource.getDriver().getFullName());
            }

            oidText.setText(
                configuration.getAttribute(DBGConstants.ATTR_PROCEDURE_OID, ""));

            nameText.setText(
                configuration.getAttribute(DBGConstants.ATTR_PROCEDURE_NAME, ""));

            processText.setText(
                configuration.getAttribute(DBGConstants.ATTR_ATTACH_PROCESS, ""));

            String extracted = configuration.getAttribute(DBGConstants.ATTR_ATTACH_KIND, (String)null);
            if (DBGController.ATTACH_KIND_GLOBAL.equals(extracted)) {
                attachGlobal.setSelection(true);
                attachLocal.setSelection(false);
            } else {
                attachGlobal.setSelection(false);
                attachLocal.setSelection(true);
            }
            handleAttachKind(extracted);

            scriptText.setText(
                configuration.getAttribute(DBGConstants.ATTR_SCRIPT_TEXT, ""));

        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    void handleAttachKind(String attachKind) {
        if (DBGController.ATTACH_KIND_GLOBAL.equals(attachKind)) {
            processText.setEditable(true);
            scriptText.setEnabled(false);
        } else {
            processText.setText("");
            processText.setEditable(false);
            scriptText.setEnabled(true);
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
        configuration.setAttribute(DBGConstants.ATTR_PROCEDURE_OID, oidText.getText());
        configuration.setAttribute(DBGConstants.ATTR_PROCEDURE_NAME, nameText.getText());
        configuration.setAttribute(DBGConstants.ATTR_ATTACH_PROCESS, processText.getText());
        Widget kind = attachGlobal.getSelection() ? attachGlobal : attachLocal;
        configuration.setAttribute(DBGConstants.ATTR_ATTACH_KIND, String.valueOf(kind.getData()));
        configuration.setAttribute(DBGConstants.ATTR_SCRIPT_TEXT, scriptText.getText());
    }

    @Override
    public String getName() {
        return DebugUIMessages.DatabaseTab_name;
    }

}
