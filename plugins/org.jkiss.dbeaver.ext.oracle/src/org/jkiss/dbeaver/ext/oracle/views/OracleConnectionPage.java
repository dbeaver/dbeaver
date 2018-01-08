/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionRole;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * OracleConnectionPage
 */
public class OracleConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    //static final Log log = Log.getLog(OracleConnectionPage.class);

    private Text hostText;
    private Text portText;
    private Combo sidServiceCombo;
    private Combo serviceNameCombo;
    private Text userNameText;
    private Combo userRoleCombo;
    private Text passwordText;
    private Combo tnsNameCombo;
	private TabFolder connectionTypeFolder;
    private ClientHomesSelector oraHomeSelector;
    private Text connectionUrlText;
    private Button osAuthCheck;

    private ControlsListener controlModifyListener;
    private OracleConstants.ConnectionType connectionType = OracleConstants.ConnectionType.BASIC;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/oracle_logo.png"); //$NON-NLS-1$
    private TextWithOpenFolder tnsPathText;

    private boolean activated = false;

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        super.setImageDescriptor(logoImage);

        controlModifyListener = new ControlsListener();

        Composite addrGroup = new Composite(composite, SWT.NONE);
        addrGroup.setLayout(new GridLayout(1, false));
        addrGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Group protocolGroup = UIUtils.createControlGroup(addrGroup, OracleMessages.dialog_connection_connection_type_group, 1, GridData.FILL_HORIZONTAL, 0);

        connectionTypeFolder = new TabFolder(protocolGroup, SWT.TOP | SWT.MULTI);
        connectionTypeFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createBasicConnectionControls(connectionTypeFolder);
		createTNSConnectionControls(connectionTypeFolder);
        createCustomConnectionControls(connectionTypeFolder);
        connectionTypeFolder.setSelection(connectionType.ordinal());
        connectionTypeFolder.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                connectionType = (OracleConstants.ConnectionType) connectionTypeFolder.getSelection()[0].getData();
                site.getActiveDataSource().getConnectionConfiguration().setProviderProperty(OracleConstants.PROP_CONNECTION_TYPE, connectionType.name());
                updateUI();
            }
        });

        final Group securityGroup = UIUtils.createControlGroup(addrGroup, OracleMessages.dialog_connection_security_group, 4, GridData.FILL_HORIZONTAL, 0);
        createSecurityGroup(securityGroup);

        Composite bottomControls = UIUtils.createPlaceholder(addrGroup, 3);
        bottomControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        {
            createClientHomeGroup(bottomControls);
        }

        createDriverPanel(addrGroup);
        setControl(addrGroup);
    }

    private void createBasicConnectionControls(TabFolder protocolFolder)
    {
        TabItem protocolTabBasic = new TabItem(protocolFolder, SWT.NONE);
        protocolTabBasic.setText(OracleMessages.dialog_connection_basic_tab);
        protocolTabBasic.setData(OracleConstants.ConnectionType.BASIC);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(5, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabBasic.setControl(targetContainer);

        Label hostLabel = UIUtils.createControlLabel(targetContainer, OracleMessages.dialog_connection_host);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        hostLabel.setLayoutData(gd);

        hostText = new Text(targetContainer, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, OracleMessages.dialog_connection_port);

        portText = new Text(targetContainer, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 40;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, OracleMessages.dialog_connection_database);

        serviceNameCombo = new Combo(targetContainer, SWT.DROP_DOWN);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        serviceNameCombo.setLayoutData(gd);
        serviceNameCombo.addModifyListener(controlModifyListener);

        sidServiceCombo = new Combo(targetContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
        sidServiceCombo.add(OracleConnectionType.SID.getTitle());
        sidServiceCombo.add(OracleConnectionType.SERVICE.getTitle());
        sidServiceCombo.select(1);
        sidServiceCombo.addModifyListener(controlModifyListener);

    }

    private void createTNSConnectionControls(TabFolder protocolFolder)
    {
        TabItem protocolTabTNS = new TabItem(protocolFolder, SWT.NONE);
        protocolTabTNS.setText(OracleMessages.dialog_connection_tns_tab);
        protocolTabTNS.setData(OracleConstants.ConnectionType.TNS);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabTNS.setControl(targetContainer);

        UIUtils.createControlLabel(targetContainer, "Network Alias");
        tnsNameCombo = new Combo(targetContainer, SWT.DROP_DOWN);
        tnsNameCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tnsNameCombo.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, "TNS names path");
        tnsPathText = new TextWithOpenFolder(targetContainer, "Oracle TNS names path");
        tnsPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tnsPathText.setToolTipText("Path to TNSNAMES.ora file");
        tnsPathText.getTextControl().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                populateTnsNameCombo();
                updateUI();
            }
        });
    }

    private Collection<String> getAvailableServiceNames()
    {
        String tnsPath = tnsPathText.getText();
        if (!CommonUtils.isEmpty(tnsPath)) {
            File tnsFile = new File(tnsPath);
            if (tnsFile.exists()) {
                return OCIUtils.readTnsNames(tnsFile, false).keySet();
            }
            return Collections.emptyList();
        }
        String oraHome = oraHomeSelector.getSelectedHome();
        if (CommonUtils.isEmpty(oraHome)) {
            return OCIUtils.readTnsNames(null, true).keySet();
        } else {
            OracleHomeDescriptor home = OCIUtils.getOraHomeByName(oraHome);
            if (home != null) {
                return home.getOraServiceNames();
            } else {
                return OCIUtils.readTnsNames(new File(oraHome), true).keySet();
            }
        }
    }

    private void populateTnsNameCombo() {
        String oldText = tnsNameCombo.getText();
        tnsNameCombo.removeAll();
        Collection<String> serviceNames = getAvailableServiceNames();
        if (serviceNames.isEmpty()) {
            tnsNameCombo.setEnabled(false);
        } else {
            tnsNameCombo.setEnabled(true);
            for (String alias : serviceNames) {
                tnsNameCombo.add(alias);
            }
            if (!oldText.isEmpty()) {
                UIUtils.setComboSelection(tnsNameCombo, oldText);
            }
            if (tnsNameCombo.getSelectionIndex() < 0) {
                tnsNameCombo.select(0);
            }
        }
    }

    private void createCustomConnectionControls(TabFolder protocolFolder)
    {
        TabItem protocolTabCustom = new TabItem(protocolFolder, SWT.NONE);
        protocolTabCustom.setText(OracleMessages.dialog_connection_custom_tab);
        protocolTabCustom.setData(OracleConstants.ConnectionType.CUSTOM);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        protocolTabCustom.setControl(targetContainer);

        final Label urlLabel = UIUtils.createControlLabel(targetContainer, "JDBC URL"); //$NON-NLS-1$
        urlLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        connectionUrlText = new Text(targetContainer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        connectionUrlText.setLayoutData(new GridData(GridData.FILL_BOTH));
        connectionUrlText.addModifyListener(controlModifyListener);
    }

    private void createSecurityGroup(Composite parent)
    {
        Label userNameLabel = UIUtils.createControlLabel(parent, OracleMessages.dialog_connection_user_name);
        userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        userNameText = new Text(parent, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        userNameText.setLayoutData(gd);
        userNameText.addModifyListener(controlModifyListener);

        Label userRoleLabel = UIUtils.createControlLabel(parent, OracleMessages.dialog_connection_role);
        userRoleLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        userRoleCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 60;
        userRoleCombo.setLayoutData(gd);
        userRoleCombo.add(OracleConnectionRole.NORMAL.getTitle());
        userRoleCombo.add(OracleConnectionRole.SYSDBA.getTitle());
        userRoleCombo.add(OracleConnectionRole.SYSOPER.getTitle());
        userRoleCombo.select(0);

        Label passwordLabel = UIUtils.createControlLabel(parent, OracleMessages.dialog_connection_password);
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(controlModifyListener);

        osAuthCheck = UIUtils.createCheckbox(parent, OracleMessages.dialog_connection_os_authentication, false);
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

        parent.setTabList(new Control[]{userNameText, passwordText, userRoleCombo, osAuthCheck});
    }

    private void createClientHomeGroup(Composite bottomControls)
    {
        oraHomeSelector = new ClientHomesSelector(bottomControls, SWT.NONE, OracleMessages.dialog_connection_ora_home) {
            @Override
            protected void handleHomeChange()
            {
                populateTnsNameCombo();
            }
        };
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 300;
        oraHomeSelector.getPanel().setLayoutData(gd);

        Label ph = new Label(bottomControls, SWT.NONE);
        ph.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    @Override
    public boolean isComplete()
    {
//        if (isOCI && CommonUtils.isEmpty(oraHomeSelector.getSelectedHome())) {
//            return false;
//        }
        switch (connectionType) {
            case BASIC:
                return !CommonUtils.isEmpty(serviceNameCombo.getText());
            case TNS:
                return !CommonUtils.isEmpty(tnsNameCombo.getText());
            case CUSTOM:
                return !CommonUtils.isEmpty(connectionUrlText.getText());
            default:
                return false;
        }
    }

    @Override
    protected boolean isCustomURL()
    {
        return this.connectionType == OracleConstants.ConnectionType.CUSTOM;
    }

    @Override
    public void loadSettings()
    {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();

        final String sidService = connectionInfo.getProviderProperty(OracleConstants.PROP_SID_SERVICE);
        if (sidService != null) {
            sidServiceCombo.setText(OracleConnectionType.valueOf(sidService).getTitle());
        }

        //if (isOCI) {
        oraHomeSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId());
        //}

        if (tnsNameCombo.getItemCount() == 0) {
            populateTnsNameCombo();
        }

        if (serviceNameCombo.getItemCount() == 0) {
            for (String alias : getAvailableServiceNames()) {
                serviceNameCombo.add(alias);
            }
        }

        String conTypeProperty = connectionInfo.getProviderProperty(OracleConstants.PROP_CONNECTION_TYPE);
        if (conTypeProperty != null) {
            connectionType = OracleConstants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
        } else {
            connectionType = OracleConstants.ConnectionType.BASIC;
        }
        connectionTypeFolder.setSelection(connectionType.ordinal());

        switch (connectionType) {
            case BASIC:
                hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    portText.setText(String.valueOf(connectionInfo.getHostPort()));
                } else if (site.getDriver().getDefaultPort() != null) {
                    portText.setText(site.getDriver().getDefaultPort());
                } else {
                    portText.setText("");
                }

                serviceNameCombo.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
                break;
            case TNS: {
                tnsNameCombo.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
                String tnsPathProperty = connectionInfo.getProviderProperty(OracleConstants.PROP_TNS_PATH);
//                if (tnsPathProperty == null) {
//                    tnsPathProperty = System.getenv(OracleConstants.VAR_TNS_ADMIN);
//                }
                if (tnsPathProperty != null) {
                    tnsPathText.setText(tnsPathProperty);
                }
                break;
            }
            case CUSTOM:
                connectionUrlText.setText(CommonUtils.notEmpty(connectionInfo.getUrl()));
                break;
        }

        if (OracleConstants.OS_AUTH_USER_NAME.equals(connectionInfo.getUserName())) {
            userNameText.setEnabled(false);
            passwordText.setEnabled(false);
            osAuthCheck.setSelection(true);
        } else {
            userNameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
            osAuthCheck.setSelection(false);
        }

        final String roleName = connectionInfo.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
        if (roleName != null) {
            userRoleCombo.setText(roleName.toUpperCase(Locale.ENGLISH));
        }

        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        connectionInfo.setClientHomeId(oraHomeSelector.getSelectedHome());

        connectionInfo.setProviderProperty(OracleConstants.PROP_CONNECTION_TYPE, connectionType.name());
        switch (connectionType) {
            case BASIC:
                connectionInfo.setHostName(hostText.getText().trim());
                connectionInfo.setHostPort(portText.getText().trim());
                connectionInfo.setDatabaseName(serviceNameCombo.getText().trim());
                break;
            case TNS:
                connectionInfo.setDatabaseName(tnsNameCombo.getText().trim());
                connectionInfo.setProviderProperty(OracleConstants.PROP_TNS_PATH, tnsPathText.getText());
                break;
            case CUSTOM:
                connectionInfo.setUrl(connectionUrlText.getText());
                break;
        }
        if (osAuthCheck.getSelection()) {
            connectionInfo.setUserName(OracleConstants.OS_AUTH_USER_NAME);
            connectionInfo.setUserPassword(""); //$NON-NLS-1$
        } else {
            connectionInfo.setUserName(userNameText.getText());
            connectionInfo.setUserPassword(passwordText.getText());
        }

        connectionInfo.setProviderProperty(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.getTypeForTitle(sidServiceCombo.getText()).name());

        if (userRoleCombo.getSelectionIndex() > 0) {
            connectionInfo.setProviderProperty(OracleConstants.PROP_INTERNAL_LOGON, userRoleCombo.getText().toLowerCase(Locale.ENGLISH));
        } else {
            connectionInfo.getProviderProperties().remove(OracleConstants.PROP_INTERNAL_LOGON);
        }

        super.saveSettings(dataSource);
    }

    private void updateUI()
    {
        if (activated) {
            site.updateButtons();
        }
    }

    private class ControlsListener implements ModifyListener, SelectionListener {
        @Override
        public void modifyText(ModifyEvent e) {
            updateUI();
        }
        @Override
        public void widgetSelected(SelectionEvent e) {
            updateUI();
        }
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            updateUI();
        }
    }

    @Override
    public IDialogPage[] getSubPages()
    {
        return new IDialogPage[] {
            new OracleConnectionExtraPage(),
            new DriverPropertiesDialogPage(this),
        };
    }

}
