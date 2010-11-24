/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.ui.IEmbeddedPart;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

class ConnectionPageFinal extends WizardPage implements IEmbeddedPart
{
    private ConnectionWizard wizard;
    private DataSourceDescriptor dataSourceDescriptor;
    private Text connectionNameText;
    private Button savePasswordCheck;
    private Button showSystemObjects;
    private Button testButton;
    private Text catFilterText;
    private Text schemaFilterText;

    ConnectionPageFinal(ConnectionWizard wizard)
    {
        super("newConnectionFinal");
        this.wizard = wizard;
        setTitle("Finish connection creation");
        setDescription("Set connection name.");
    }

    ConnectionPageFinal(ConnectionWizard wizard, DataSourceDescriptor dataSourceDescriptor)
    {
        this(wizard);
        this.dataSourceDescriptor = dataSourceDescriptor;
    }

    public void activatePart()
    {
        if (testButton != null) {
            ConnectionPageSettings settings = wizard.getPageSettings();
            testButton.setEnabled(settings != null && settings.isPageComplete());
            if (settings != null && connectionNameText != null && CommonUtils.isEmpty(connectionNameText.getText())) {
                DBPConnectionInfo connectionInfo = settings.getConnectionInfo();
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    connectionNameText.setText(connectionInfo.getDatabaseName());
                } else if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                    connectionNameText.setText(connectionInfo.getHostName());
                }
            }
        }
        if (dataSourceDescriptor != null) {
            savePasswordCheck.setSelection(dataSourceDescriptor.isSavePassword());
            showSystemObjects.setSelection(dataSourceDescriptor.isShowSystemObjects());
        }
    }

    public void deactivatePart() {
    }

    public void createControl(Composite parent)
    {
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        //gl.marginHeight = 20;
        //gl.marginWidth = 20;
        //gl.verticalSpacing = 10;
        group.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        //gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_CENTER;
        //gd.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
        //group.setLayoutData(gd);

        connectionNameText = UIUtils.createLabelText(group, "Connection name", dataSourceDescriptor == null ? "" : dataSourceDescriptor.getName());

        {
            Group securityGroup = UIUtils.createControlGroup(group, "Security", 1, GridData.FILL_HORIZONTAL, 0);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            securityGroup.setLayoutData(gd);
         
            savePasswordCheck = UIUtils.createCheckbox(securityGroup, "Save password locally", dataSourceDescriptor != null && dataSourceDescriptor.isSavePassword());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            //gd.horizontalSpan = 2;
            savePasswordCheck.setLayoutData(gd);
        }

        {
            Group filterGroup = UIUtils.createControlGroup(group, "Filters", 2, GridData.FILL_HORIZONTAL, 0);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            filterGroup.setLayoutData(gd);
            
            showSystemObjects = UIUtils.createCheckbox(filterGroup, "Show system objects", dataSourceDescriptor == null || dataSourceDescriptor.isShowSystemObjects());
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            showSystemObjects.setLayoutData(gd);

            String catFilter = dataSourceDescriptor == null ? null : dataSourceDescriptor.getCatalogFilter();
            catFilterText = UIUtils.createCheckText(filterGroup, "Filter catalogs", CommonUtils.getString(catFilter), !CommonUtils.isEmpty(catFilter), 200);

            String schFilter = dataSourceDescriptor == null ? "" : dataSourceDescriptor.getSchemaFilter();
            schemaFilterText = UIUtils.createCheckText(filterGroup, "Filter schemas", CommonUtils.getString(schFilter), !CommonUtils.isEmpty(schFilter), 200);
        }

        testButton = new Button(group, SWT.PUSH);
        testButton.setText("Test Connection ... ");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        testButton.setLayoutData(gd);
        testButton.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                testConnection();
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });

        setControl(group);
    }

    public boolean isPageComplete()
    {
        return connectionNameText != null &&
            !CommonUtils.isEmpty(connectionNameText.getText());
    }

    void saveSettings(DataSourceDescriptor dataSource)
    {
        dataSource.setName(connectionNameText.getText());
        dataSource.setSavePassword(savePasswordCheck.getSelection());
        dataSource.setShowSystemObjects(showSystemObjects.getSelection());
        if (!dataSource.isSavePassword()) {
            dataSource.resetPassword();
        }
        dataSource.setCatalogFilter(catFilterText == null ? null : catFilterText.getText());
        dataSource.setSchemaFilter(schemaFilterText == null ? null : schemaFilterText.getText());
    }

    private void testConnection()
    {
        wizard.testConnection(wizard.getPageSettings().getConnectionInfo());
    }

}