/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.wmi.Activator;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAdvanced;

/**
 * WMIConnectionPage
 */
public abstract class WMIConnectionPage extends ConnectionPageAdvanced
{
    private Text domainText;
    private Text hostText;
    private Combo namespaceCombo;
    private Combo localeCombo;
    private Text usernameText;
    private Text passwordText;
    private Button testButton;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/wmi_logo.png");


    @Override
    public void dispose()
    {
        super.dispose();
    }

/*
    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        setImageDescriptor(logoImage);

        TabFolder optionsFolder = new TabFolder(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        optionsFolder.setLayoutData(gd);

        TabItem addrTab = new TabItem(optionsFolder, SWT.NONE);
        addrTab.setText(MySQLMessages.dialog_connection_general_tab);
        addrTab.setToolTipText(MySQLMessages.dialog_connection_general_tab_tooltip);
        addrTab.setControl(createGeneralTab(optionsFolder));

        final TabItem propsTab = new TabItem(optionsFolder, SWT.NONE);
        propsTab.setText(MySQLMessages.dialog_connection_advanced_tab);
        propsTab.setToolTipText(MySQLMessages.dialog_connection_advanced_tab_tooltip);
        propsTab.setControl(createPropertiesTab(optionsFolder));

        optionsFolder.addSelectionListener(
            new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    if (e.item == propsTab) {
                        //refreshDriverProperties();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            }
        );
        setControl(optionsFolder);
    }

    private Composite createGeneralTab(Composite parent)
    {
        ModifyListener textListener = new ModifyListener()
        {
            public void modifyText(ModifyEvent e)
            {
                evaluateURL();
            }
        };

        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = UIUtils.createControlLabel(addrGroup, "Host");
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        Label dbLabel = UIUtils.createControlLabel(addrGroup, "Namespace");
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

        Label usernameLabel = UIUtils.createControlLabel(addrGroup, "USer");
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(textListener);

        Label passwordLabel = UIUtils.createControlLabel(addrGroup, "Password");
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(textListener);

        homesSelector = new ClientHomesSelector(addrGroup, SWT.NONE, "Local Server");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 3;
        gd.minimumWidth = 300;
        homesSelector.setLayoutData(gd);

        testButton = new Button(addrGroup, SWT.PUSH);
        testButton.setText("Test");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 1;
        testButton.setLayoutData(gd);
        testButton.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                site.testConnection();
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
        testButton.setEnabled(false);
        return addrGroup;
    }

    public boolean isComplete()
    {
        return hostText != null && portText != null && 
            !CommonUtils.isEmpty(hostText.getText()) &&
            !CommonUtils.isEmpty(portText.getText());
    }

    public void loadSettings()
    {
        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            if (hostText != null) {
                hostText.setText(CommonUtils.getString(connectionInfo.getHostName()));
            }
            if (portText != null) {
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    portText.setText(String.valueOf(connectionInfo.getHostPort()));
                } else {
                    portText.setText(String.valueOf(MySQLConstants.DEFAULT_PORT));
                }
            }
            if (dbText != null) {
                dbText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
            }
            if (usernameText != null) {
                usernameText.setText(CommonUtils.getString(connectionInfo.getUserName()));
            }
            if (passwordText != null) {
                passwordText.setText(CommonUtils.getString(connectionInfo.getUserPassword()));
            }
            homesSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId());
        } else {
            if (portText != null) {
                portText.setText(String.valueOf(MySQLConstants.DEFAULT_PORT));
            }
        }

        super.loadSettings();
    }

    protected void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo != null) {
            if (hostText != null) {
                connectionInfo.setHostName(hostText.getText());
            }
            if (portText != null) {
                connectionInfo.setHostPort(portText.getText());
            }
            if (dbText != null) {
                connectionInfo.setDatabaseName(dbText.getText());
            }
            if (usernameText != null) {
                connectionInfo.setUserName(usernameText.getText());
            }
            if (passwordText != null) {
                connectionInfo.setUserPassword(passwordText.getText());
            }
            if (homesSelector != null) {
                connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
            }
            connectionInfo.setUrl(
                "jdbc:wmi://" + connectionInfo.getHostName() +
                    ":" + connectionInfo.getHostPort() +
                    "/" + connectionInfo.getDatabaseName());
            super.saveSettings(connectionInfo);
        }
    }

    private void evaluateURL()
    {
        site.updateButtons();
        if (testButton != null) {
            testButton.setEnabled(this.isComplete());
        }
    }
*/

}
