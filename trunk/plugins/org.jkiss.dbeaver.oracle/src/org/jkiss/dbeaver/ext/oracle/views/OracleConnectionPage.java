/*
 * Copyright (C) 2010-2012 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.ext.oracle.OracleMessages;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionRole;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleLanguage;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleTerritory;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAdvanced;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * OracleConnectionPage
 */
public class OracleConnectionPage extends ConnectionPageAdvanced
{
    //static final Log log = LogFactory.getLog(OracleConnectionPage.class);

    private Text hostText;
    private Text portText;
    private Combo sidServiceCombo;
    private Combo serviceNameCombo;
    private Text userNameText;
    private Combo userRoleCombo;
    private Text passwordText;
    private Combo tnsNameCombo;
	private CTabFolder connectionTypeFolder;
    private Composite bottomControls;
    private ClientHomesSelector oraHomeSelector;
    //private Button ociDriverCheck;
    private Text connectionUrlText;
    private Button osAuthCheck;

    private ControlsListener controlModifyListener;
    private OracleConstants.ConnectionType connectionType = OracleConstants.ConnectionType.BASIC;
    private boolean isOCI;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/oracle_logo.png"); //$NON-NLS-1$
    private Combo languageCombo;
    private Combo territoryCombo;
    private Button hideEmptySchemasCheckbox;
    private Button showDBAAlwaysCheckbox;

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        super.setImageDescriptor(logoImage);

        controlModifyListener = new ControlsListener();

        TabFolder optionsFolder = new TabFolder(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        optionsFolder.setLayoutData(gd);

        TabItem addrTab = new TabItem(optionsFolder, SWT.NONE);
        addrTab.setText(OracleMessages.dialog_connection_general_tab);
        addrTab.setToolTipText(OracleMessages.dialog_connection_general_tab_tooltip);
        addrTab.setControl(createGeneralTab(optionsFolder));

        final TabItem cfgTab = new TabItem(optionsFolder, SWT.NONE);
        cfgTab.setText("Settings");
        cfgTab.setToolTipText("Additional connection settings");
        cfgTab.setControl(createConfigurationTab(optionsFolder));

        final TabItem propsTab = new TabItem(optionsFolder, SWT.NONE);
        propsTab.setText(OracleMessages.dialog_connection_advanced_tab);
        propsTab.setToolTipText(OracleMessages.dialog_connection_advanced_tab_tooltip);
        propsTab.setControl(super.createPropertiesTab(optionsFolder));

/*
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
*/
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

        final Group protocolGroup = UIUtils.createControlGroup(addrGroup, OracleMessages.dialog_connection_connection_type_group, 1, GridData.FILL_HORIZONTAL, 0);

        connectionTypeFolder = new CTabFolder(protocolGroup, SWT.TOP | SWT.MULTI);
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
                connectionType = (OracleConstants.ConnectionType) connectionTypeFolder.getSelection().getData();
                updateUI();
            }
        });

        final Group securityGroup = UIUtils.createControlGroup(addrGroup, OracleMessages.dialog_connection_security_group, 4, GridData.FILL_HORIZONTAL, 0);
        createSecurityGroup(securityGroup);

        bottomControls = UIUtils.createPlaceholder(addrGroup, 3);
        bottomControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createBottomGroup(bottomControls);
        return addrGroup;
    }

    private void createBasicConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabBasic = new CTabItem(protocolFolder, SWT.NONE);
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

    }

    private void createTNSConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabTNS = new CTabItem(protocolFolder, SWT.NONE);
        protocolTabTNS.setText(OracleMessages.dialog_connection_tns_tab);
        protocolTabTNS.setData(OracleConstants.ConnectionType.TNS);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(2, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabTNS.setControl(targetContainer);

        tnsNameCombo = new Combo(targetContainer, SWT.DROP_DOWN);
        tnsNameCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tnsNameCombo.addModifyListener(controlModifyListener);
    }

    private void populateTnsNameCombo() {
        tnsNameCombo.removeAll();
        String oraHome = null;
        if (isOCI) {
            if (oraHomeSelector != null) {
                oraHome = oraHomeSelector.getSelectedHome();
            }
            if (CommonUtils.isEmpty(oraHome)) {
                if (!OCIUtils.getOraHomes().isEmpty()) {
                    oraHome = OCIUtils.getOraHomes().get(0).getDisplayName();
                }
            }
            if (!CommonUtils.isEmpty(oraHome)) {
                OracleHomeDescriptor home = OCIUtils.getOraHomeByName(oraHome);
                if (home != null) {
                    for (String alias : home.getOraServiceNames()) {
                        tnsNameCombo.add(alias);
                    }
                }
            }
        }
        else {
            for (String alias : OCIUtils.readTnsNames(null, true)) {
                tnsNameCombo.add(alias);
            }
        }
    }

    private void createCustomConnectionControls(CTabFolder protocolFolder)
    {
        CTabItem protocolTabCustom = new CTabItem(protocolFolder, SWT.NONE);
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

        parent.setTabList(new Control[] {userNameText, passwordText, userRoleCombo, osAuthCheck});
    }

    private void createBottomGroup(Composite bottomControls)
    {
//        {
//            UIUtils.createControlLabel(bottomControls, "Oracle Home");
//            final Combo oraHomeCombo = new Combo(bottomControls, SWT.DROP_DOWN | SWT.READ_ONLY);
//            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//            gd.widthHint = 100;
//            oraHomeCombo.setLayoutData(gd);
//            Button oraHomeButton = new Button(bottomControls, SWT.PUSH);
//            oraHomeButton.setText("...");
//            oraHomeButton.addSelectionListener(new SelectionAdapter() {
//                @Override
//                public void widgetSelected(SelectionEvent e)
//                {
//                    OracleHomesDialog homesDialog = new OracleHomesDialog(getShell(), site.getDriver());
//                    homesDialog.open();
//                }
//            });
//            Label phLabel = new Label(bottomControls, SWT.NONE);
//            phLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        }

        oraHomeSelector = new ClientHomesSelector(bottomControls, SWT.NONE, OracleMessages.dialog_connection_ora_home) {
            @Override
            protected void handleHomeChange()
            {
                populateTnsNameCombo();
            }
        };
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 300;
        oraHomeSelector.setLayoutData(gd);

        Label ph = new Label(bottomControls, SWT.NONE);
        ph.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private Composite createConfigurationTab(Composite parent)
    {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        {
            final Group sessionGroup = UIUtils.createControlGroup(cfgGroup, "Session settings", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            languageCombo = UIUtils.createLabelCombo(sessionGroup, "Language", SWT.DROP_DOWN);
            languageCombo.setToolTipText("Session language");
            languageCombo.add(OracleConstants.NLS_DEFAULT_VALUE);
            for (OracleLanguage language : OracleLanguage.values()) {
                languageCombo.add(language.getLanguage());
            }
            languageCombo.setText(OracleConstants.NLS_DEFAULT_VALUE);

            territoryCombo = UIUtils.createLabelCombo(sessionGroup, "Territory", SWT.DROP_DOWN);
            territoryCombo.setToolTipText("Session territory");
            territoryCombo.add(OracleConstants.NLS_DEFAULT_VALUE);
            for (OracleTerritory territory : OracleTerritory.values()) {
                territoryCombo.add(territory.getTerritory());
            }
            territoryCombo.setText(OracleConstants.NLS_DEFAULT_VALUE);
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(cfgGroup, "Content", 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            hideEmptySchemasCheckbox = UIUtils.createCheckbox(contentGroup, "Hide empty schemas", true);
            hideEmptySchemasCheckbox.setToolTipText(
                "Check existence of objects within schema and do not show empty schemas in tree. " + ContentUtils.getDefaultLineSeparator() +
                "Enabled by default but it may cause performance problems on databases with very big number of objects.");

            showDBAAlwaysCheckbox = UIUtils.createCheckbox(contentGroup, "Always show DBA objects", false);
            showDBAAlwaysCheckbox.setToolTipText(
                "Always shows DBA-related metadata objects in tree even if user do not has DBA role.");
        }

        return cfgGroup;
    }

    @Override
    public boolean isComplete()
    {
        if (isOCI && CommonUtils.isEmpty(oraHomeSelector.getSelectedHome())) {
            return false;
        }
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
        isOCI = OCIUtils.isOciDriver(site.getDriver());

        oraHomeSelector.setVisible(isOCI);

        if (tnsNameCombo.getItemCount() == 0) {
            populateTnsNameCombo();
        }

        if (serviceNameCombo.getItemCount() == 0) {
            String oraHome = isOCI ? (!OCIUtils.getOraHomes().isEmpty() ? OCIUtils.getOraHomes().get(0).getHomeId() : null) : null;
            for (String alias : OCIUtils.readTnsNames(oraHome == null ? null : new File(oraHome), true)) {
                serviceNameCombo.add(alias);
            }
        }

        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            Map<Object,Object> connectionProperties = connectionInfo.getProperties();

            final Object sidService = connectionProperties.get(OracleConstants.PROP_SID_SERVICE);
            if (sidService != null) {
                sidServiceCombo.setText(OracleConnectionType.valueOf(sidService.toString()).getTitle());
            }

            if (isOCI) {
                oraHomeSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId());
            }

            Object conTypeProperty = connectionProperties.get(OracleConstants.PROP_CONNECTION_TYPE);
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

                    serviceNameCombo.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
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

            final Object roleName = connectionProperties.get(OracleConstants.PROP_INTERNAL_LOGON);
            if (roleName != null) {
                userRoleCombo.setText(roleName.toString().toUpperCase());
            }

            {
                // Settings
                final Object nlsLanguage = connectionProperties.get(OracleConstants.PROP_SESSION_LANGUAGE);
                if (nlsLanguage != null) {
                    languageCombo.setText(nlsLanguage.toString());
                }

                final Object nlsTerritory = connectionProperties.get(OracleConstants.PROP_SESSION_TERRITORY);
                if (nlsTerritory != null) {
                    territoryCombo.setText(nlsTerritory.toString());
                }

                final Object checkSchemaContent = connectionProperties.get(OracleConstants.PROP_CHECK_SCHEMA_CONTENT);
                if (checkSchemaContent != null) {
                    hideEmptySchemasCheckbox.setSelection(CommonUtils.getBoolean(checkSchemaContent, false));
                }

                final Object showDBAObjects = connectionProperties.get(OracleConstants.PROP_ALWAYS_SHOW_DBA);
                if (showDBAObjects != null) {
                    showDBAAlwaysCheckbox.setSelection(CommonUtils.getBoolean(showDBAObjects, false));
                }
            }
        } else {
            if (portText != null) {
                portText.setText(String.valueOf(OracleConstants.DEFAULT_PORT));
            }
        }

        super.loadSettings();
    }

    @Override
    protected void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo == null) {
            return;
        }
        super.saveSettings(connectionInfo);
        Map<Object, Object> connectionProperties = connectionInfo.getProperties();
        if (isOCI) {
            connectionInfo.setClientHomeId(oraHomeSelector.getSelectedHome());
        }

        connectionProperties.put(OracleConstants.PROP_CONNECTION_TYPE, connectionType.name());
        connectionProperties.put(
                OracleConstants.PROP_DRIVER_TYPE, isOCI ? OracleConstants.DRIVER_TYPE_OCI : OracleConstants.DRIVER_TYPE_THIN);
//            connectionInfo.getProperties().put(OracleConstants.PROP_DRIVER_TYPE,
//                ociDriverCheck.getSelection() ? OracleConstants.DRIVER_TYPE_OCI : OracleConstants.DRIVER_TYPE_THIN);
        switch (connectionType) {
            case BASIC:
                connectionInfo.setHostName(hostText.getText());
                connectionInfo.setHostPort(portText.getText());
                connectionInfo.setDatabaseName(serviceNameCombo.getText());
                break;
            case TNS:
                connectionInfo.setDatabaseName(tnsNameCombo.getText());
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

        connectionProperties.put(OracleConstants.PROP_SID_SERVICE, OracleConnectionType.getTypeForTitle(sidServiceCombo.getText()).name());

        if (userRoleCombo.getSelectionIndex() > 0) {
            connectionProperties.put(OracleConstants.PROP_INTERNAL_LOGON, userRoleCombo.getText().toLowerCase());
        } else {
            connectionProperties.remove(OracleConstants.PROP_INTERNAL_LOGON);
        }

        {
            // Settings
            if (!OracleConstants.NLS_DEFAULT_VALUE.equals(languageCombo.getText())) {
                connectionProperties.put(OracleConstants.PROP_SESSION_LANGUAGE, languageCombo.getText());
            } else {
                connectionProperties.remove(OracleConstants.PROP_SESSION_LANGUAGE);
            }

            if (!OracleConstants.NLS_DEFAULT_VALUE.equals(territoryCombo.getText())) {
                connectionProperties.put(OracleConstants.PROP_SESSION_TERRITORY, territoryCombo.getText());
            } else {
                connectionProperties.remove(OracleConstants.PROP_SESSION_TERRITORY);
            }

            connectionProperties.put(
                OracleConstants.PROP_CHECK_SCHEMA_CONTENT,
                String.valueOf(hideEmptySchemasCheckbox.getSelection()));

            connectionProperties.put(
                OracleConstants.PROP_ALWAYS_SHOW_DBA,
                String.valueOf(showDBAAlwaysCheckbox.getSelection()));
        }
        saveConnectionURL(connectionInfo);
    }

    private void updateUI()
    {
        site.updateButtons();
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

}
