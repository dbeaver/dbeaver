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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.internal.ui.DebugUiMessages;
import org.jkiss.dbeaver.ui.UIUtils;

public class DatabaseTab extends AbstractLaunchConfigurationTab {

    private Text datasourceText;
    private Text databaseText;

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
        createDatasourceComponent(comp);
        createDatabaseComponent(comp);
    }

    protected void createDatasourceComponent(Composite comp)
    {
        Group datasourceGroup = UIUtils.createControlGroup(comp, DebugUiMessages.DatabaseTab_datasource_group_text, 2, GridData.FILL_HORIZONTAL,
                SWT.DEFAULT);

        datasourceText = UIUtils.createLabelText(datasourceGroup, DebugUiMessages.DatabaseTab_datasource_label_text, DebugCore.ATTR_DATASOURCE_DEFAULT);
        datasourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        datasourceText.addModifyListener(modifyListener);
        datasourceText.setEditable(false);
    }

    protected void createDatabaseComponent(Composite comp)
    {
        Group databaseGroup = UIUtils.createControlGroup(comp, DebugUiMessages.DatabaseTab_database_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        databaseText = UIUtils.createLabelText(databaseGroup, DebugUiMessages.DatabaseTab_database_label_text, DebugCore.ATTR_DATABASE_DEFAULT);
        databaseText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        databaseText.addModifyListener(modifyListener);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
    {
        configuration.setAttribute(DebugCore.ATTR_DATASOURCE, DebugCore.ATTR_DATASOURCE_DEFAULT);
        configuration.setAttribute(DebugCore.ATTR_DATABASE, DebugCore.ATTR_DATABASE_DEFAULT);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration)
    {
        initializeDatasource(configuration);
        initializeDatabase(configuration);
    }

    protected void initializeDatasource(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractDatasource(configuration);
        datasourceText.setText(extracted);
    }

    protected void initializeDatabase(ILaunchConfiguration configuration)
    {
        String extracted = DebugCore.extractDatabase(configuration);
        databaseText.setText(extracted);
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration)
    {
        configuration.setAttribute(DebugCore.ATTR_DATASOURCE, datasourceText.getText());
        configuration.setAttribute(DebugCore.ATTR_DATABASE, databaseText.getText());
    }

    @Override
    public String getName()
    {
        return DebugUiMessages.DatabaseTab_name;
    }

}
