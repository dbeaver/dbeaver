/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.hana.model.HANAConstants;
import org.jkiss.dbeaver.ext.hana.ui.internal.HANAEdition;
import org.jkiss.dbeaver.ext.hana.ui.internal.HANAMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.Map.Entry;

/*
 *  when edition==GENERIC, don't show/touch any driver properties for
 *  - compatibility with previous configuration
 *  - full control over all driver properties
 *  
 *  since JDBC 2.6 client the 'encrypt' property is automatically set for connections to port 443 (e.g. HANA Cloud) 
 *  https://help.sap.com/viewer/79ae9d3916b84356a89744c65793b924/2.6/en-US/22485d2937c4427fbbedefe3cc158571.html
 *  so we do not have to add checkboxes here.
 *  
 */
public class HANAConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    final static String PROP_DATABASE_NAME = "databaseName";
    final static String PROV_PROP_INSTANCE_NUMBER = "instanceNumber";
    final static String PROV_PROP_EDITION = "edition";

    private Combo editionCombo;
    private Text hostText;
    private Text portText;
    private Label instanceLabel;
    private Text instanceText;
    private Label databaseLabel;
    private Text databaseText;
    private boolean created;

    private HANAEdition edition;
    
    // saved custom value while Text is read-only
    private String portValue; 
    private String instanceValue;
    private String databaseValue;

    private final Image logoImage;

    public HANAConnectionPage() {
        logoImage = createImage("icons/sap_hana_logo.png"); //$NON-NLS-1$
    }

    @Override
    public void dispose() {
        super.dispose();
        UIUtils.dispose(logoImage);
    }

    @Override
    public Image getImage() {
        return logoImage;
    }

    @Override
    public void createControl(Composite composite) {
        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite addrGroup = UIUtils.createControlGroup(settingsGroup, HANAMessages.label_connection, 2, 0, 0);
        addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(addrGroup, HANAMessages.label_edition);
        editionCombo = new Combo(addrGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (HANAEdition edition : HANAEdition.values()) {
            editionCombo.add(edition.getTitle());
        }

        hostText = UIUtils.createLabelText(addrGroup, HANAMessages.label_host, "");
        portText = UIUtils.createLabelText(addrGroup, HANAMessages.label_port, "");
        portText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        ((GridData)portText.getLayoutData()).widthHint = UIUtils.getFontHeight(portText) * 5;
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

        instanceLabel = UIUtils.createControlLabel(addrGroup, HANAMessages.label_instance);
        instanceText = new Text(addrGroup, SWT.BORDER);
        instanceText.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        ((GridData) instanceText.getLayoutData()).widthHint = UIUtils.getFontHeight(instanceText) * 5;
        instanceText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        instanceText.setToolTipText(HANAMessages.tooltip_instance);
        databaseLabel = UIUtils.createControlLabel(addrGroup, HANAMessages.label_database);
        databaseText = new Text(addrGroup, SWT.BORDER);
        databaseText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        editionCombo.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { editionUpdated(); site.updateButtons(); }
        });
        hostText.addModifyListener(e -> { hostUpdated(); site.updateButtons(); });
        portText.addModifyListener(e -> site.updateButtons());
        instanceText.addModifyListener(e -> { instanceUpdated(); site.updateButtons(); });
        databaseText.addModifyListener(e -> site.updateButtons());

        createAuthPanel(settingsGroup, 1);
        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
        created = true;
    }

    @Override
    public boolean isComplete() {
        if (CommonUtils.isEmpty(hostText.getText().trim()))
            return false;
        if (CommonUtils.isEmpty(portText.getText().trim()))
            return false;
        if (edition != HANAEdition.GENERIC) {
            if (instanceText.getEditable() && !CommonUtils.isEmpty(instanceText.getText())) {
                int instance = CommonUtils.toInt(instanceText.getText().trim(), -1);
                if(instance < 0 || instance > 99) return false;
            }
            if (databaseText.getEditable() && CommonUtils.isEmpty(databaseText.getText().trim()))
                return false;
        }
        return super.isComplete();
    }

    /*
     * HANA driver properties are case insensitive. Reuse and cleanup properties set previously w/o HANA specific connection page
     */
    private String getProperty(DBPConnectionConfiguration connectionInfo, String name) {
        for (Entry<String, String> entry : connectionInfo.getProperties().entrySet()) {
            if(entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private void setProperty(DBPConnectionConfiguration connectionInfo, String name, String value) {
        for (Entry<String, String> entry : connectionInfo.getProperties().entrySet()) {
            if(entry.getKey().equalsIgnoreCase(name) && !entry.getKey().equals(name)) {
                connectionInfo.removeProperty(name);
            }
        }
        connectionInfo.setProperty(name, value);
    }

    private void removeProperty(DBPConnectionConfiguration connectionInfo, String name) {
        for (Entry<String, String> entry : connectionInfo.getProperties().entrySet()) {
            if(entry.getKey().equalsIgnoreCase(name)) {
                connectionInfo.removeProperty(name);
            }
        }
    }
    
    @Override
    public void loadSettings() {
        super.loadSettings();
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        edition = HANAEdition.fromName(connectionInfo.getProviderProperty(PROV_PROP_EDITION));
        portValue = CommonUtils.toString(connectionInfo.getHostPort()/*, site.getDriver().getDefaultPort()*/);
        instanceValue = CommonUtils.notEmpty(connectionInfo.getProviderProperty(PROV_PROP_INSTANCE_NUMBER));
        databaseValue = CommonUtils.notEmpty(getProperty(connectionInfo, PROP_DATABASE_NAME));
        if (created) {
            editionCombo.select(edition.ordinal());
            hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
            portText.setText(portValue);
            instanceText.setText(instanceValue);
            databaseText.setText(databaseValue);
            editionUpdated();
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        connectionInfo.setProviderProperty(PROV_PROP_EDITION, edition.name());
        if (created) {
            connectionInfo.setHostName(hostText.getText().trim());
            connectionInfo.setHostPort(portText.getText().trim());
            if (edition != HANAEdition.GENERIC) {
                instanceValue = instanceText.getText().trim();
                if (instanceValue.isEmpty()) {
                    connectionInfo.removeProviderProperty(PROV_PROP_INSTANCE_NUMBER);
                } else {
                    connectionInfo.setProviderProperty(PROV_PROP_INSTANCE_NUMBER, instanceValue);
                }
                databaseValue = databaseText.getText().trim();
                if (databaseValue.isEmpty()) {
                    removeProperty(connectionInfo, PROP_DATABASE_NAME);
                } else {
                    setProperty(connectionInfo, PROP_DATABASE_NAME, databaseValue);
                }
            }
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] { new DriverPropertiesDialogPage(this) };
    }

    private void editionUpdated() {
        // save old values
        if (portText.getEditable()) {
            portValue = portText.getText().trim();
        }
        if (instanceText.getEditable()) {
            instanceValue = instanceText.getText().trim();
        }
        if (databaseText.getEditable()) {
            databaseValue = databaseText.getText().trim();
        }
        
        edition = HANAEdition.fromTitle(editionCombo.getText());
        
        portText.setEditable(edition == HANAEdition.GENERIC || edition == HANAEdition.EXPRESS);
        UIUtils.fixReadonlyTextBackground(portText);
        switch (edition) {
        case GENERIC:
            portText.setText(portValue);
            break;
        case PLATFORM_SINGLE_DB:
        case PLATFORM_SYSTEM_DB:
        case PLATFORM_TENANT_DB:
            instanceUpdated();
            break;
        case EXPRESS:
            if(portValue.isEmpty())
                portText.setText("39015");
            else
                portText.setText(portValue);
            break;
        case CLOUD:
            portText.setText("443");
            break;
        default:
            break;
        }
        
        if (edition == HANAEdition.PLATFORM_SINGLE_DB || edition == HANAEdition.PLATFORM_SYSTEM_DB || edition == HANAEdition.PLATFORM_TENANT_DB) {
            if(instanceValue.isEmpty()) {
                int port = CommonUtils.toInt(portValue);
                if(port >= 30000 && port <= 39999)
                    instanceText.setText(String.valueOf((port-30000)/100));
            } else {
                instanceText.setText(instanceValue);
            }
            instanceText.setEditable(true);
        } else if (edition == HANAEdition.EXPRESS) {
            instanceText.setText("90");
            instanceText.setEditable(false);
        } else {
            instanceText.setText("");
            instanceText.setEditable(false);
        }
        UIUtils.fixReadonlyTextBackground(instanceText);

        if (edition == HANAEdition.PLATFORM_TENANT_DB) {
            databaseText.setText(databaseValue);
            databaseText.setEditable(true);
        } else {
            databaseText.setText("");
            databaseText.setEditable(false);
        }
        UIUtils.fixReadonlyTextBackground(databaseText);

        boolean visible = edition != HANAEdition.GENERIC && edition != HANAEdition.CLOUD;
        toggleControlVisibility(instanceLabel, visible);
        toggleControlVisibility(instanceText, visible);
        toggleControlVisibility(databaseLabel, visible);
        toggleControlVisibility(databaseText, visible);
        ((Composite)getControl()).layout(true, true);
    }

    private void toggleControlVisibility(Control control, boolean visible) {
        control.setVisible(visible);
        Object layoutData = control.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).exclude = !visible;
        }
    }

    private void hostUpdated() {
        if (CommonUtils.isEmpty(hostText.getText())) {
            return;
        }
        String host = hostText.getText().trim();
        if (edition == HANAEdition.GENERIC && CommonUtils.isEmpty(portText.getText()) && host.endsWith(HANAConstants.HTTPS_PORT_SUFFIX)) {
            hostText.setText(host.substring(0, host.length() - HANAConstants.HTTPS_PORT_SUFFIX.length()));
            editionCombo.select(editionCombo.indexOf(HANAEdition.CLOUD.getTitle()));
            editionUpdated();
        }
    }
    
    private void instanceUpdated() {
        if (CommonUtils.isEmpty(instanceText.getText())) {
            return;
        }
        int instance = CommonUtils.toInt(instanceText.getText().trim(), 0);
        switch (edition) {
            case PLATFORM_SINGLE_DB:
                portText.setText(String.format("3%02d15", instance));
                break;
            case PLATFORM_SYSTEM_DB:
            case PLATFORM_TENANT_DB:
                portText.setText(String.format("3%02d13", instance));
                break;
            default:
                break;
        }
    }
}
