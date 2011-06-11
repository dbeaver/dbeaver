/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

/**
 * OracleConnectionPage
 */
public class OracleConnectionPage extends DialogPage implements IDataSourceConnectionEditor
{
    private IDataSourceConnectionEditorSite site;
    private Text hostText;
    private Text portText;
    private Text serviceNameText;
    private Text userNameText;
    private Combo userRoleCombo;
    private Text passwordText;
    private ConnectionPropertiesControl connectionProps;
    private Button testButton;
    private PropertySourceCustom propertySource;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/oracle_logo.png");
    private ModifyListener controlModifyListener;


    @Override
    public void dispose()
    {
        super.dispose();
    }

    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        super.setImageDescriptor(logoImage);

        controlModifyListener = new ModifyListener()
        {
            public void modifyText(ModifyEvent e)
            {
                evaluateURL();
            }
        };

        TabFolder optionsFolder = new TabFolder(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        optionsFolder.setLayoutData(gd);

        TabItem addrTab = new TabItem(optionsFolder, SWT.NONE);
        addrTab.setText("General");
        addrTab.setToolTipText("General connection properties");
        addrTab.setControl(createGeneralTab(optionsFolder));

        final TabItem propsTab = new TabItem(optionsFolder, SWT.NONE);
        propsTab.setText("Advanced");
        propsTab.setToolTipText("Advanced/custom driver properties");
        final Composite placeholder = UIUtils.createPlaceholder(optionsFolder, 1);
        connectionProps = new ConnectionPropertiesControl(placeholder, SWT.NONE);
        propsTab.setControl(placeholder);

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
        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        final Group protocolGroup = UIUtils.createControlGroup(addrGroup, "Connection Type", 1, GridData.FILL_HORIZONTAL, 0);

        CTabFolder protocolFolder = new CTabFolder(protocolGroup, SWT.TOP | SWT.MULTI);
        protocolFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createBasicConnectionControls(protocolFolder);
        createTNSConnectionControls(protocolFolder);
        createCustomConnectionControls(protocolFolder);

        final Group securityGroup = UIUtils.createControlGroup(addrGroup, "Security", 4, GridData.FILL_HORIZONTAL, 0);
        createSecurityGroup(securityGroup);

        testButton = new Button(addrGroup, SWT.PUSH);
        testButton.setText("Test Connection ... ");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
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

    private void createBasicConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabBasic = new CTabItem(protocolFolder, SWT.NONE);
        protocolTabBasic.setText("Basic");

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(4, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabBasic.setControl(targetContainer);

        UIUtils.createControlLabel(targetContainer, "Host");

        hostText = new Text(targetContainer, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, "Port");

        portText = new Text(targetContainer, SWT.BORDER);
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 40;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
        portText.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, "SID/Service");

        serviceNameText = new Text(targetContainer, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        serviceNameText.setLayoutData(gd);
        serviceNameText.addModifyListener(controlModifyListener);
    }

    private void createTNSConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabTNS = new CTabItem(protocolFolder, SWT.NONE);
        protocolTabTNS.setText("TNS");

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabTNS.setControl(targetContainer);

        UIUtils.createControlLabel(targetContainer, "Network Alias");

        Combo hostText = new Combo(targetContainer, SWT.DROP_DOWN);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(controlModifyListener);
    }

    private void createCustomConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabTNS = new CTabItem(protocolFolder, SWT.NONE);
        protocolTabTNS.setText("Custom");

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        protocolTabTNS.setControl(targetContainer);

        final Label urlLabel = UIUtils.createControlLabel(targetContainer, "JDBC URL");
        urlLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        Text urlText = new Text(targetContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        urlText.setLayoutData(new GridData(GridData.FILL_BOTH));
        urlText.addModifyListener(controlModifyListener);
    }

    private void createSecurityGroup(Composite parent)
    {
        Label userNameLabel = UIUtils.createControlLabel(parent, "User name");
        userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        userNameText = new Text(parent, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        userNameText.setLayoutData(gd);
        userNameText.addModifyListener(controlModifyListener);

        Label userRoleLabel = UIUtils.createControlLabel(parent, "Role");
        userRoleLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        userRoleCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 60;
        userRoleCombo.setLayoutData(gd);
        userRoleCombo.add("Normal");
        userRoleCombo.add("SYSDBA");
        userRoleCombo.add("SYSOPER");
        userRoleCombo.select(0);

        Label passwordLabel = UIUtils.createControlLabel(parent, "Password");
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(controlModifyListener);

        final Button osAuthCheck = UIUtils.createCheckbox(parent, "OS Authentication", false);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        osAuthCheck.setLayoutData(gd);
    }

    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
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
                    portText.setText(String.valueOf(OracleConstants.DEFAULT_PORT));
                }
            }
            if (serviceNameText != null) {
                serviceNameText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
            }
            if (userNameText != null) {
                userNameText.setText(CommonUtils.getString(connectionInfo.getUserName()));
            }
            if (passwordText != null) {
                passwordText.setText(CommonUtils.getString(connectionInfo.getUserPassword()));
            }
        } else {
            if (portText != null) {
                portText.setText(String.valueOf(OracleConstants.DEFAULT_PORT));
            }
        }

        // Set props model
        if (connectionProps != null) {
            refreshDriverProperties();
        }
    }

    private void refreshDriverProperties()
    {
        DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
        saveSettings(tmpConnectionInfo);
        tmpConnectionInfo.setProperties(site.getConnectionInfo().getProperties());
        propertySource = connectionProps.makeProperties(site.getDriver(), tmpConnectionInfo/*.getUrl(), site.getConnectionInfo().getProperties()*/);
        connectionProps.loadProperties(propertySource);
    }

    public void saveSettings()
    {
        saveSettings(site.getConnectionInfo());
    }

    private void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo != null) {
            if (hostText != null) {
                connectionInfo.setHostName(hostText.getText());
            }
            if (portText != null) {
                connectionInfo.setHostPort(portText.getText());
            }
            if (serviceNameText != null) {
                connectionInfo.setDatabaseName(serviceNameText.getText());
            }
            if (userNameText != null) {
                connectionInfo.setUserName(userNameText.getText());
            }
            if (passwordText != null) {
                connectionInfo.setUserPassword(passwordText.getText());
            }
            if (propertySource != null) {
                connectionInfo.setProperties(propertySource.getProperties());
            }
            connectionInfo.setUrl(
                "jdbc:oracle://" + connectionInfo.getHostName() +
                    ":" + connectionInfo.getHostPort() +
                    "/" + connectionInfo.getDatabaseName());
        }
    }

    private void evaluateURL()
    {
        site.updateButtons();
        if (testButton != null) {
            testButton.setEnabled(this.isComplete());
        }
    }

}
