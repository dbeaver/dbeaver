/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.internal.ui.DebugUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
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
    private Text callText;

    /**
     * Modify listener that simply updates the owning launch configuration
     * dialog.
     */
    protected ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent evt)
        {
            scheduleUpdateJob();
        }
    };

    @Override
    public void createControl(Composite parent)
    {
        Composite comp = new Composite(parent, SWT.NONE);
        setControl(comp);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
        comp.setLayout(new GridLayout(1, true));
        comp.setFont(parent.getFont());

        createComponents(comp);
    }

    protected void createComponents(Composite comp)
    {
        createDriverComponent(comp);
        createDatasourceComponent(comp);
        createDatabaseComponent(comp);
        createSchemaComponent(comp);
        createOidComponent(comp);
        createNameComponent(comp);
        createCallComponent(comp);
    }

    protected void createDriverComponent(Composite comp)
    {
        Group driverGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_driver_group_text, 2, GridData.FILL_HORIZONTAL,
                SWT.DEFAULT);

        driverText = UIUtils.createLabelText(driverGroup, DebugUIMessages.DatabaseTab_driver_label_text, DebugCore.ATTR_DRIVER_ID_DEFAULT);
        driverText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        driverText.addModifyListener(modifyListener);
        driverText.setEditable(false);
    }

    protected void createDatasourceComponent(Composite comp)
    {
        Group datasourceGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_datasource_group_text, 2, GridData.FILL_HORIZONTAL,
                SWT.DEFAULT);

        datasourceText = UIUtils.createLabelText(datasourceGroup, DebugUIMessages.DatabaseTab_datasource_label_text, DebugCore.ATTR_DATASOURCE_ID_DEFAULT);
        datasourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        datasourceText.addModifyListener(modifyListener);
        datasourceText.setEditable(false);
    }

    protected void createDatabaseComponent(Composite comp)
    {
        Group databaseGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_database_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        databaseText = UIUtils.createLabelText(databaseGroup, DebugUIMessages.DatabaseTab_database_label_text, DebugCore.ATTR_DATABASE_NAME_DEFAULT);
        databaseText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        databaseText.addModifyListener(modifyListener);
    }

    protected void createSchemaComponent(Composite comp)
    {
        Group databaseGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_schema_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        schemaText = UIUtils.createLabelText(databaseGroup, DebugUIMessages.DatabaseTab_schema_label_text, DebugCore.ATTR_SCHEMA_NAME_DEFAULT);
        schemaText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        schemaText.addModifyListener(modifyListener);
    }

    protected void createOidComponent(Composite comp)
    {
        Group oidGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_oid_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        oidText = UIUtils.createLabelText(oidGroup, DebugUIMessages.DatabaseTab_oid_label_text, DebugCore.ATTR_PROCEDURE_OID_DEFAULT);
        oidText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        oidText.addModifyListener(modifyListener);
    }

    protected void createNameComponent(Composite comp)
    {
        Group oidGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_name_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        nameText = UIUtils.createLabelText(oidGroup, DebugUIMessages.DatabaseTab_name_label_text, DebugCore.ATTR_PROCEDURE_NAME_DEFAULT);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameText.addModifyListener(modifyListener);
    }

    protected void createCallComponent(Composite comp)
    {
        Group callGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_call_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        callText = UIUtils.createLabelText(callGroup, DebugUIMessages.DatabaseTab_call_label_text, DebugCore.ATTR_PROCEDURE_CALL_DEFAULT);
        callText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        callText.addModifyListener(modifyListener);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
    {
        configuration.setAttribute(DebugCore.ATTR_DRIVER_ID, DebugCore.ATTR_DRIVER_ID);
        configuration.setAttribute(DebugCore.ATTR_DATASOURCE_ID, DebugCore.ATTR_DATASOURCE_ID_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_DATABASE_NAME, DebugCore.ATTR_DATABASE_NAME_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_SCHEMA_NAME, DebugCore.ATTR_SCHEMA_NAME_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_OID, DebugCore.ATTR_PROCEDURE_OID_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_NAME, DebugCore.ATTR_PROCEDURE_NAME_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_CALL, DebugCore.ATTR_PROCEDURE_CALL_DEFAULT);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration)
    {
        initializeDriver(configuration);
        initializeDatasource(configuration);
        initializeDatabase(configuration);
        initializeSchema(configuration);
        initializeOid(configuration);
        initializeName(configuration);
        initializeCall(configuration);
    }

    protected void initializeDriver(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractDriverId(configuration);
        driverText.setText(extracted);
    }

    protected void initializeDatasource(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractDatasourceId(configuration);
        datasourceText.setText(extracted);
    }

    protected void initializeDatabase(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractDatabaseName(configuration);
        databaseText.setText(extracted);
    }
    
    protected void initializeSchema(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractSchemaName(configuration);
        schemaText.setText(extracted);
    }
    
    protected void initializeOid(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractProcedureOid(configuration);
        oidText.setText(extracted);
    }
    
    protected void initializeName(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractProcedureName(configuration);
        nameText.setText(extracted);
    }
    
    protected void initializeCall(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractProcedureCall(configuration);
        callText.setText(extracted);
    }
    
    @Override
    public Image getImage() {
        DBPImage image = extractDriverImage();
        if (image == null) {
            image = DBIcon.TREE_DATABASE;
        }
        return DBeaverIcons.getImage(image );
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
    public void performApply(ILaunchConfigurationWorkingCopy configuration)
    {
        configuration.setAttribute(DebugCore.ATTR_DRIVER_ID, driverText.getText());
        configuration.setAttribute(DebugCore.ATTR_DATASOURCE_ID, datasourceText.getText());
        configuration.setAttribute(DebugCore.ATTR_DATABASE_NAME, databaseText.getText());
        configuration.setAttribute(DebugCore.ATTR_SCHEMA_NAME, schemaText.getText());
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_OID, oidText.getText());
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_NAME, nameText.getText());
        configuration.setAttribute(DebugCore.ATTR_PROCEDURE_CALL, callText.getText());
    }

    @Override
    public String getName()
    {
        return DebugUIMessages.DatabaseTab_name;
    }

}
