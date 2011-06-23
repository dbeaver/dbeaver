/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.*;
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

    private ModifyListener controlModifyListener;
    private Button ociDriverCheck;
    private Text connectionUrlText;
    private Button osAuthCheck;
    private OracleConstants.ConnectionType connectionType = OracleConstants.ConnectionType.BASIC;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/oracle_logo.png");
    private Combo tnsNameCombo;
    private CTabFolder connectionTypeFolder;

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
                updateButtons();
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

        connectionTypeFolder = new CTabFolder(protocolGroup, SWT.TOP | SWT.MULTI);
        connectionTypeFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ociDriverCheck = UIUtils.createCheckbox(connectionTypeFolder, "OCI Driver", false);
        connectionTypeFolder.setTopRight(ociDriverCheck);

        createBasicConnectionControls(connectionTypeFolder);
        createTNSConnectionControls(connectionTypeFolder);
        createCustomConnectionControls(connectionTypeFolder);
        connectionTypeFolder.setSelection(connectionType.ordinal());
        connectionTypeFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                connectionType = (OracleConstants.ConnectionType) connectionTypeFolder.getSelection().getData();
                updateButtons();
            }
        });

        final Group securityGroup = UIUtils.createControlGroup(addrGroup, "Security", 4, GridData.FILL_HORIZONTAL, 0);
        createSecurityGroup(securityGroup);

        final Composite bottomControls = UIUtils.createPlaceholder(addrGroup, 5);
        bottomControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            UIUtils.createControlLabel(bottomControls, "Oracle Home");
            final Combo oraHomeCombo = new Combo(bottomControls, SWT.DROP_DOWN);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 100;
            oraHomeCombo.setLayoutData(gd);
            Button oraHomeButton = new Button(bottomControls, SWT.PUSH);
            oraHomeButton.setText("...");
            Label phLabel = new Label(bottomControls, SWT.NONE);
            phLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        {
            testButton = new Button(bottomControls, SWT.PUSH);
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
        }
        return addrGroup;
    }

    private void createBasicConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabBasic = new CTabItem(protocolFolder, SWT.NONE);
        protocolTabBasic.setText("Basic");
        protocolTabBasic.setData(OracleConstants.ConnectionType.BASIC);

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
        protocolTabTNS.setData(OracleConstants.ConnectionType.TNS);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabTNS.setControl(targetContainer);

        UIUtils.createControlLabel(targetContainer, "Network Alias");

        tnsNameCombo = new Combo(targetContainer, SWT.DROP_DOWN);
        tnsNameCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tnsNameCombo.addModifyListener(controlModifyListener);
    }

    private void createCustomConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabCustom = new CTabItem(protocolFolder, SWT.NONE);
        protocolTabCustom.setText("Custom");
        protocolTabCustom.setData(OracleConstants.ConnectionType.CUSTOM);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        protocolTabCustom.setControl(targetContainer);

        final Label urlLabel = UIUtils.createControlLabel(targetContainer, "JDBC URL");
        urlLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        connectionUrlText = new Text(targetContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        connectionUrlText.setLayoutData(new GridData(GridData.FILL_BOTH));
        connectionUrlText.addModifyListener(controlModifyListener);
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
        userRoleCombo.add(OracleConstants.ConnectionRole.NORMAL.getTitle());
        userRoleCombo.add(OracleConstants.ConnectionRole.SYSDBA.getTitle());
        userRoleCombo.add(OracleConstants.ConnectionRole.SYSOPER.getTitle());
        userRoleCombo.select(0);

        Label passwordLabel = UIUtils.createControlLabel(parent, "Password");
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(controlModifyListener);

        osAuthCheck = UIUtils.createCheckbox(parent, "OS Authentication", false);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        osAuthCheck.setLayoutData(gd);
        osAuthCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                boolean osAuth = osAuthCheck.getSelection();
                userNameText.setEnabled(!osAuth);
                passwordText.setEnabled(!osAuth);
            }
        });
    }

    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    public boolean isComplete()
    {
        switch (connectionType) {
            case BASIC:
                return !CommonUtils.isEmpty(serviceNameText.getText());
            case TNS:
                return !CommonUtils.isEmpty(tnsNameCombo.getText());
            case CUSTOM:
                return !CommonUtils.isEmpty(connectionUrlText.getText());
            default:
                return false;
        }
    }

    public void loadSettings()
    {
        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            final Object conTypeProperty = connectionInfo.getProperties().get(OracleConstants.PROP_CONNECTION_TYPE);
            if (conTypeProperty != null) {
                connectionType = OracleConstants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
            } else {
                connectionType = OracleConstants.ConnectionType.BASIC;
            }
            connectionTypeFolder.setSelection(connectionType.ordinal());

            switch (connectionType) {
                case BASIC:
                    hostText.setText(CommonUtils.getString(connectionInfo.getHostName()));
                    if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                        portText.setText(String.valueOf(connectionInfo.getHostPort()));
                    } else {
                        portText.setText(String.valueOf(OracleConstants.DEFAULT_PORT));
                    }

                    serviceNameText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
                    break;
                case TNS:
                    tnsNameCombo.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
                    break;
                case CUSTOM:
                    connectionUrlText.setText(CommonUtils.getString(connectionInfo.getUrl()));
                    break;
            }

            if (OracleConstants.OS_AUTH_USER_NAME.equals(connectionInfo.getUserName())) {
                userNameText.setEnabled(false);
                passwordText.setEnabled(false);
                osAuthCheck.setSelection(true);
            } else {
                userNameText.setText(CommonUtils.getString(connectionInfo.getUserName()));
                passwordText.setText(CommonUtils.getString(connectionInfo.getUserPassword()));
                osAuthCheck.setSelection(false);
            }

            final Object roleName = connectionInfo.getProperties().get(OracleConstants.PROP_INTERNAL_LOGON);
            if (roleName != null) {
                userRoleCombo.setText(roleName.toString().toUpperCase());
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
            if (propertySource != null) {
                connectionInfo.getProperties().putAll(propertySource.getProperties());
            }

            connectionInfo.getProperties().put(OracleConstants.PROP_CONNECTION_TYPE, connectionType.name());
            connectionInfo.getProperties().put(OracleConstants.PROP_DRIVER_TYPE,
                ociDriverCheck.getSelection() ? OracleConstants.DRIVER_TYPE_OCI : OracleConstants.DRIVER_TYPE_THIN);
            switch (connectionType) {
                case BASIC:
                    connectionInfo.setHostName(hostText.getText());
                    connectionInfo.setHostPort(portText.getText());
                    connectionInfo.setDatabaseName(serviceNameText.getText());
                    generateConnectionURL(connectionInfo);
                    break;
                case TNS:
                    connectionInfo.setDatabaseName(tnsNameCombo.getText());
                    generateConnectionURL(connectionInfo);
                    break;
                case CUSTOM:
                    connectionInfo.setUrl(connectionUrlText.getText());
                    break;
            }
            if (osAuthCheck.getSelection()) {
                connectionInfo.setUserName(OracleConstants.OS_AUTH_USER_NAME);
                connectionInfo.setUserPassword("");
            } else {
                connectionInfo.setUserName(userNameText.getText());
                connectionInfo.setUserPassword(passwordText.getText());
            }
            if (userRoleCombo.getSelectionIndex() > 0) {
                connectionInfo.getProperties().put(OracleConstants.PROP_INTERNAL_LOGON, userRoleCombo.getText().toLowerCase());
            } else {
                connectionInfo.getProperties().remove(OracleConstants.PROP_INTERNAL_LOGON);
            }
        }
    }

    private void generateConnectionURL(DBPConnectionInfo connectionInfo)
    {
        boolean isOCI = OracleConstants.DRIVER_TYPE_OCI.equals(
            connectionInfo.getProperties().get(OracleConstants.PROP_DRIVER_TYPE));
        StringBuilder url = new StringBuilder(100);
        url.append("jdbc:oracle:");
        if (isOCI) {
            url.append("oci");
        } else {
            url.append("thin");
        }
        url.append(":@//");
        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            url.append(connectionInfo.getHostName());
        }
        url.append(":");
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(connectionInfo.getHostPort());
        }
        url.append("/");
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            url.append(connectionInfo.getDatabaseName());
        }
        connectionInfo.setUrl(url.toString());
    }

    private void updateButtons()
    {
        site.updateButtons();
        if (testButton != null) {
            testButton.setEnabled(this.isComplete());
        }
    }

}
