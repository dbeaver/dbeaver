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

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.debug.DBGController;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.internal.ui.DebugUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

public class DatabaseTab extends AbstractLaunchConfigurationTab {

    private Text driverText;
    private Text datasourceText;
    private Text databaseText;
    private Text schemaText;
    private Text oidText;
    private Text nameText;

    private Button attachLocal;
    private Button attachGlobal;
    private Text processText;
    private Button scriptExecute;
    private Text scriptText;

    /**
     * Modify listener that simply updates the owning launch configuration
     * dialog.
     */
    protected ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent evt) {
            scheduleUpdateJob();
        }
    };

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
        String groupText = DebugUIMessages.DatabaseTab_connection_group_text;
        Group group = UIUtils.createControlGroup(composite, groupText, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        {
            String label = DebugUIMessages.DatabaseTab_driver_label_text;
            String value = DebugCore.ATTR_DRIVER_ID_DEFAULT;
            driverText = UIUtils.createLabelText(group, label, value, SWT.READ_ONLY);
        }
        {
            String label = DebugUIMessages.DatabaseTab_datasource_label_text;
            String value = DebugCore.ATTR_DATASOURCE_ID_DEFAULT;
            datasourceText = UIUtils.createLabelText(group, label, value, SWT.READ_ONLY);
        }
    }

    protected void createDatabaseSettingsGroup(Composite composite) {
        String groupLabel = DebugUIMessages.DatabaseTab_procedure_group_text;
        Group group = UIUtils.createControlGroup(composite, groupLabel, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        {
            String label = DebugUIMessages.DatabaseTab_database_label_text;
            String value = DebugCore.ATTR_DATABASE_NAME_DEFAULT;
            databaseText = UIUtils.createLabelText(group, label, value, SWT.READ_ONLY);
        }
        {
            String label = DebugUIMessages.DatabaseTab_schema_label_text;
            String value = DebugCore.ATTR_SCHEMA_NAME_DEFAULT;
            schemaText = UIUtils.createLabelText(group, label, value, SWT.READ_ONLY);
        }
        {
            String label = DebugUIMessages.DatabaseTab_oid_label_text;
            String value = DebugCore.ATTR_PROCEDURE_OID_DEFAULT;
            oidText = UIUtils.createLabelText(group, label, value, SWT.READ_ONLY);
        }
        {
            String label = DebugUIMessages.DatabaseTab_name_label_text;
            String value = DebugCore.ATTR_PROCEDURE_NAME_DEFAULT;
            nameText = UIUtils.createLabelText(group, label, value, SWT.READ_ONLY);
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
            };
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

        processText = UIUtils.createLabelText(group, "PID", DebugCore.ATTR_ATTACH_PROCESS_DEFAULT, SWT.READ_ONLY,
                GridDataFactory.swtDefaults().span(1, 1).create());
        processText.addModifyListener(modifyListener);

        scriptExecute = new Button(group, SWT.CHECK);
        scriptExecute.setText(DebugUIMessages.DatabaseTab_script_execute_text);
        scriptExecute.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
        scriptExecute.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });

        scriptText = new Text(group, SWT.MULTI);
        scriptText.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).grab(true, true).create());
        scriptText.addModifyListener(modifyListener);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(DebugCore.ATTR_DRIVER_ID, DebugCore.ATTR_DRIVER_ID_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_DATASOURCE_ID, DebugCore.ATTR_DATASOURCE_ID_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_DATABASE_NAME, DebugCore.ATTR_DATABASE_NAME_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_SCHEMA_NAME, DebugCore.ATTR_SCHEMA_NAME_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_OID, DebugCore.ATTR_PROCEDURE_OID_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_NAME, DebugCore.ATTR_PROCEDURE_NAME_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_ATTACH_PROCESS, DebugCore.ATTR_ATTACH_PROCESS_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_ATTACH_KIND, DebugCore.ATTR_ATTACH_KIND_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_SCRIPT_EXECUTE, DebugCore.ATTR_SCRIPT_EXECUTE_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_SCRIPT_TEXT, DebugCore.ATTR_SCRIPT_TEXT_DEFAULT);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        initializeDriver(configuration);
        initializeDatasource(configuration);
        initializeDatabase(configuration);
        initializeSchema(configuration);
        initializeOid(configuration);
        initializeName(configuration);
        initializeProcess(configuration);
        initializeKind(configuration);
        initializeExecute(configuration);
        initializeText(configuration);
    }

    protected void initializeDriver(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractDriverId(configuration);
        DriverDescriptor driver = DataSourceProviderRegistry.getInstance().findDriver(extracted);
        String driverName = extracted;
        if (driver != null) {
            driverName = driver.getName();
        }
        driverText.setText(driverName);
        driverText.setData(extracted);
    }

    protected void initializeDatasource(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractDatasourceId(configuration);
        String datasourceName = extracted;
        DataSourceDescriptor dataSource = DataSourceRegistry.findDataSource(extracted);
        if (dataSource != null) {
            datasourceName = dataSource.getName();
        }
        datasourceText.setText(datasourceName);
        datasourceText.setData(extracted);
    }

    protected void initializeDatabase(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractDatabaseName(configuration);
        databaseText.setText(extracted);
    }

    protected void initializeSchema(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractSchemaName(configuration);
        schemaText.setText(extracted);
    }

    protected void initializeOid(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractProcedureOid(configuration);
        oidText.setText(extracted);
    }

    protected void initializeName(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractProcedureName(configuration);
        nameText.setText(extracted);
    }

    protected void initializeProcess(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractAttachProcess(configuration);
        processText.setText(extracted);
    }

    protected void initializeKind(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractAttachKind(configuration);
        if (DBGController.ATTACH_KIND_GLOBAL.equals(extracted)) {
            attachGlobal.setSelection(true);
            attachLocal.setSelection(false);
        } else {
            attachGlobal.setSelection(false);
            attachLocal.setSelection(true);
        }
        handleAttachKind(extracted);
    }

    protected void initializeExecute(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractScriptExecute(configuration);
        scriptExecute.setSelection(Boolean.parseBoolean(extracted));
    }

    protected void initializeText(ILaunchConfiguration configuration) {
        String extracted = DebugCore.extractScriptText(configuration);
        scriptText.setText(extracted);
    }

    void handleAttachKind(String attachKind) {
        if (DBGController.ATTACH_KIND_GLOBAL.equals(attachKind)) {
            processText.setEditable(true);
            scriptExecute.setSelection(false);
            scriptExecute.setEnabled(false);
            scriptText.setEnabled(false);
        } else {
            processText.setText(DebugCore.ATTR_ATTACH_PROCESS_DEFAULT);
            processText.setEditable(false);
            scriptExecute.setSelection(true);
            scriptExecute.setEnabled(false);
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
        configuration.setAttribute(DebugCore.ATTR_DRIVER_ID, String.valueOf(driverText.getData()));
        configuration.setAttribute(DebugCore.ATTR_DATASOURCE_ID, String.valueOf(datasourceText.getData()));
        configuration.setAttribute(DebugCore.ATTR_DATABASE_NAME, databaseText.getText());
        configuration.setAttribute(DebugCore.ATTR_SCHEMA_NAME, schemaText.getText());
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_OID, oidText.getText());
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_NAME, nameText.getText());
        configuration.setAttribute(DebugCore.ATTR_ATTACH_PROCESS, processText.getText());
        Widget kind = attachGlobal.getSelection() ? attachGlobal : attachLocal;
        configuration.setAttribute(DebugCore.ATTR_ATTACH_KIND, String.valueOf(kind.getData()));
        configuration.setAttribute(DebugCore.ATTR_SCRIPT_EXECUTE, String.valueOf(scriptExecute.getSelection()));
        configuration.setAttribute(DebugCore.ATTR_SCRIPT_TEXT, scriptText.getText());
    }

    @Override
    public String getName() {
        return DebugUIMessages.DatabaseTab_name;
    }

}
