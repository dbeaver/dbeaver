/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

class ConnectionPageFinal extends WizardPage
{
    private ConnectionWizard wizard;
    private DataSourceDescriptor dataSource;
    private Text connectionNameText;
    private Button savePasswordCheck;
    private Button showSystemObjects;
    private Button testButton;

    ConnectionPageFinal(ConnectionWizard wizard)
    {
        super("newConnectionFinal");
        this.wizard = wizard;
        setTitle("Finish connection creation");
        setDescription("Set connection name.");
    }

    ConnectionPageFinal(ConnectionWizard wizard, DataSourceDescriptor dataSource)
    {
        this(wizard);
        this.dataSource = dataSource;
    }

    void activate()
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
        if (dataSource != null) {
            savePasswordCheck.setSelection(dataSource.isSavePassword());
            showSystemObjects.setSelection(dataSource.isShowSystemObjects());
        }
    }

    public void createControl(Composite parent)
    {
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 20;
        gl.marginWidth = 20;
        gl.verticalSpacing = 10;
        group.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_CENTER;
        //gd.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
        group.setLayoutData(gd);

        connectionNameText = UIUtils.createLabelText(group, "Connection name", dataSource == null ? "" : dataSource.getName());

        savePasswordCheck = new Button(group, SWT.CHECK);
        savePasswordCheck.setText("Save password locally");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        savePasswordCheck.setLayoutData(gd);
        if (dataSource != null) {
            savePasswordCheck.setSelection(dataSource.isSavePassword());
        }

        showSystemObjects = new Button(group, SWT.CHECK);
        showSystemObjects.setText("Show system objects");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        showSystemObjects.setLayoutData(gd);
        if (dataSource != null) {
            showSystemObjects.setSelection(dataSource.isShowSystemObjects());
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
    }

    private void testConnection()
    {
        wizard.testConnection(wizard.getPageSettings().getConnectionInfo());
    }

}