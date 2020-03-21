/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.auth.OracleAuthOS;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIActivator;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * OracleConnectionPage
 */
public class OracleConnectionPage extends ConnectionPageWithAuth implements ICompositeDialogPage {

    private Text hostText;
    private Text portText;
    private Combo sidServiceCombo;
    private Combo serviceNameCombo;
    private Combo tnsNameCombo;
	private TabFolder connectionTypeFolder;
    private ClientHomesSelector oraHomeSelector;
    private Text connectionUrlText;

    private ControlsListener controlModifyListener;
    private OracleConstants.ConnectionType connectionType = OracleConstants.ConnectionType.BASIC;

    private static ImageDescriptor logoImage = OracleUIActivator.getImageDescriptor("icons/oracle_logo.png"); //$NON-NLS-1$
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

        UIUtils.createControlLabel(addrGroup, OracleUIMessages.dialog_connection_connection_type_group);

        connectionTypeFolder = new TabFolder(addrGroup, SWT.TOP | SWT.MULTI);
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

        createAuthPanel(addrGroup, 1);

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
        protocolTabBasic.setText(OracleUIMessages.dialog_connection_basic_tab);
        protocolTabBasic.setData(OracleConstants.ConnectionType.BASIC);

        Composite targetContainer = new Composite(protocolFolder, SWT.NONE);
        targetContainer.setLayout(new GridLayout(5, false));
        targetContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        protocolTabBasic.setControl(targetContainer);

        Label hostLabel = UIUtils.createControlLabel(targetContainer, OracleUIMessages.dialog_connection_host);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        hostLabel.setLayoutData(gd);

        hostText = new Text(targetContainer, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, OracleUIMessages.dialog_connection_port);

        portText = new Text(targetContainer, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(portText) * 5;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(controlModifyListener);

        UIUtils.createControlLabel(targetContainer, OracleUIMessages.dialog_connection_database);

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
        protocolTabTNS.setText(OracleUIMessages.dialog_connection_tns_tab);
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
        tnsPathText.getTextControl().addModifyListener(e -> {
            populateTnsNameCombo();
            updateUI();
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
        protocolTabCustom.setText(OracleUIMessages.dialog_connection_custom_tab);
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

    private void createClientHomeGroup(Composite bottomControls)
    {
        oraHomeSelector = new ClientHomesSelector(bottomControls, OracleUIMessages.dialog_connection_ora_home) {
            @Override
            protected void handleHomeChange()
            {
                populateTnsNameCombo();
            }
        };
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(bottomControls) * 30;
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
        oraHomeSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId(), site.isNew());
        //}

        if (tnsNameCombo.getItemCount() == 0) {
            UIUtils.asyncExec(this::populateTnsNameCombo);
        }

        if (serviceNameCombo.getItemCount() == 0) {
            UIUtils.asyncExec(() -> {
                for (String alias : getAvailableServiceNames()) {
                    serviceNameCombo.add(alias);
                }
            });
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
                    portText.setText(connectionInfo.getHostPort());
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

        activated = true;
    }

    @NotNull
    @Override
    protected String getDefaultAuthModelId(DBPDataSourceContainer dataSource) {
        // FIXME: left for backward compatibility. Replaced by auth model. Remove in future.
        if (CommonUtils.toBoolean(dataSource.getConnectionConfiguration().getProviderProperty(OracleConstants.OS_AUTH_PROP))) {
            return OracleAuthOS.ID;
        }
        return super.getDefaultAuthModelId(dataSource);
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
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
            new OracleConnectionExtraPage(),
            new DriverPropertiesDialogPage(this),
        };
    }

}
