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
import org.eclipse.swt.widgets.Label;
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

    private static final int DEFAULT_WIDTH = 80;
	private Text driverText;
    private Text datasourceText;
    private Text databaseText;
    private Text schemaText;
    private Text oidText;
    private Text nameText;
    private Text callText;
    private GridData labelsGD;
    private GridData fieldsGD;
    
	
	
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
    	    createLayoutData();
        createConnectionSettingsGroup(comp);
        createDatabaseSettingsGroup(comp);
        createAdditionalSettingsGroup(comp);
       
    }

	private void createLayoutData() {
		labelsGD = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		labelsGD.widthHint = DEFAULT_WIDTH;
		fieldsGD = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);

	}

	protected void createConnectionSettingsGroup(Composite comp)
    {
        Group connectionSettingsGroup = 
        	UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_driver_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        
        // Driver
		createLabel(connectionSettingsGroup, DebugUIMessages.DatabaseTab_driver_label_text, labelsGD);
        driverText = createTextField(driverText, connectionSettingsGroup, DebugCore.ATTR_DRIVER_ID_DEFAULT, fieldsGD);

        // DataSource
		createLabel(connectionSettingsGroup, DebugUIMessages.DatabaseTab_datasource_group_text, labelsGD);
		datasourceText = createTextField(datasourceText, connectionSettingsGroup, DebugCore.ATTR_DRIVER_ID_DEFAULT, fieldsGD);
		
    }

	
    protected void createDatabaseSettingsGroup(Composite comp)
    {
        Group databaseSettingsGroup = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_database_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
      
        // Database
        createLabel(databaseSettingsGroup, DebugUIMessages.DatabaseTab_database_label_text, labelsGD);
        databaseText = createTextField(databaseText, databaseSettingsGroup, DebugCore.ATTR_DATABASE_NAME_DEFAULT, fieldsGD);
        
        // Schema
        createLabel(databaseSettingsGroup, DebugUIMessages.DatabaseTab_schema_label_text, labelsGD);
        schemaText = createTextField(schemaText, databaseSettingsGroup, DebugCore.ATTR_SCHEMA_NAME_DEFAULT, fieldsGD);
        
        // Oid
        createLabel(databaseSettingsGroup, DebugUIMessages.DatabaseTab_oid_label_text, labelsGD);
        oidText = createTextField(oidText, databaseSettingsGroup,  DebugCore.ATTR_PROCEDURE_OID_DEFAULT, fieldsGD);
        
    }

    protected void createAdditionalSettingsGroup(Composite comp)
    {
        Group additionalSettings = UIUtils.createControlGroup(comp, DebugUIMessages.DatabaseTab_name_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        // Name
        createLabel(additionalSettings, DebugUIMessages.DatabaseTab_name_label_text, labelsGD);
        nameText = createTextField(nameText, additionalSettings,  DebugCore.ATTR_PROCEDURE_NAME_DEFAULT, fieldsGD);
        
        // Call
        createLabel(additionalSettings, DebugUIMessages.DatabaseTab_call_label_text, labelsGD);
        callText = createTextField(callText, additionalSettings,  DebugCore.ATTR_PROCEDURE_CALL_DEFAULT, fieldsGD);
      
    }

    private Text createTextField(Text textFiled, Group connectionSettingsGroup, String text, GridData layoutData) {
		textFiled = new Text(connectionSettingsGroup, SWT.BORDER);
		textFiled.setLayoutData(fieldsGD);
		textFiled.setText(text);
		textFiled.addModifyListener(modifyListener);
		textFiled.setEditable(false);
		return textFiled;
	}

	private void createLabel(Group connectionSettingsGroup, String text, GridData layoutData) {
		Label lblDriverText = new Label(connectionSettingsGroup, SWT.BORDER);
        lblDriverText.setText(text+":");
        lblDriverText.setLayoutData(layoutData);
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
