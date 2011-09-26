/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * OracleConnectionPage
 */
public class OracleConnectionPage extends DialogPage implements IDataSourceConnectionEditor
{
    static final Log log = LogFactory.getLog(OracleConnectionPage.class);

    public static final String BROWSE = "Browse...";
    private IDataSourceConnectionEditorSite site;
    private Text hostText;
    private Text portText;
    private Combo serviceNameCombo;
    private Text userNameText;
    private Combo userRoleCombo;
    private Text passwordText;
    private ConnectionPropertiesControl connectionProps;
    private Button testButton;
    private PropertySourceCustom propertySource;
    private Combo oraHomeNameCombo;
    private Combo tnsNameCombo;
	private CTabFolder connectionTypeFolder;
    private Composite bottomControls;
    private Control oraHomeSelector;
    //private Button ociDriverCheck;
    private Text connectionUrlText;
    private Button osAuthCheck;

    private ControlsListener controlModifyListener;
    private OracleConstants.ConnectionType connectionType = OracleConstants.ConnectionType.BASIC;
    private boolean isOCI;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/oracle_logo.png");
    private Label oracleVersionLabel;
    private SelectionListener oraHomeSelectionListener;
    private DirectoryDialog directoryDialog;

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

        controlModifyListener = new ControlsListener();

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

        final Group protocolGroup = UIUtils.createControlGroup(addrGroup, "Connection Type", 1, GridData.FILL_HORIZONTAL, 0);

        connectionTypeFolder = new CTabFolder(protocolGroup, SWT.TOP | SWT.MULTI);
        connectionTypeFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createBasicConnectionControls(connectionTypeFolder);
		createTNSConnectionControls(connectionTypeFolder);
        createCustomConnectionControls(connectionTypeFolder);
        connectionTypeFolder.setSelection(connectionType.ordinal());
        connectionTypeFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                connectionType = (OracleConstants.ConnectionType) connectionTypeFolder.getSelection().getData();
                updateUI();
            }
        });

        final Group securityGroup = UIUtils.createControlGroup(addrGroup, "Security", 4, GridData.FILL_HORIZONTAL, 0);
        createSecurityGroup(securityGroup);

        bottomControls = UIUtils.createPlaceholder(addrGroup, 2);
        bottomControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createBottomGroup(bottomControls);
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

        serviceNameCombo = new Combo(targetContainer, SWT.DROP_DOWN);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        serviceNameCombo.setLayoutData(gd);
        serviceNameCombo.addModifyListener(controlModifyListener);

        if (!OCIUtils.oraHomes.isEmpty()) {
            for (String alias : OCIUtils.oraHomes.get(0).getOraServiceNames()) {
                serviceNameCombo.add(alias);
            }
        }
    }

    private Control createOraHomeSelector(Composite parent)
    {
        Composite selectorContainer = new Composite(parent, SWT.NONE);
        selectorContainer.setLayout(new GridLayout(3, false));
        selectorContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label label = UIUtils.createControlLabel(selectorContainer, "  Oracle Home");
        label.setFont(UIUtils.makeBoldFont(label.getFont()));
        oraHomeNameCombo = new Combo(selectorContainer, SWT.READ_ONLY);
        directoryDialog = new DirectoryDialog(selectorContainer.getShell(), SWT.OPEN);
        oraHomeSelectionListener = new OraHomeSelectionListener();
        populateOraHomeCombo();
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        oraHomeNameCombo.setLayoutData(gd);
        oraHomeNameCombo.addSelectionListener(oraHomeSelectionListener);
        oracleVersionLabel = new Label(selectorContainer, SWT.NONE);
        oracleVersionLabel.setText("                  ");
        return selectorContainer;
    }

    private void displayClientVersion() {
        OracleHomeDescriptor oraHome = OCIUtils.getOraHomeByName(oraHomeNameCombo.getText());
        if (oraHome != null) {
            // display Ora client version
            if (oraHome.getFullOraVersion() != null) {
                oracleVersionLabel.setText(oraHome.getFullOraVersion());
            } else {
                if (oraHome.getOraVersion() != null) {
                    oracleVersionLabel.setText("v." + oraHome.getOraVersion());
                } else {
                    oracleVersionLabel.setText("");
                }
            }
        } else {
            oracleVersionLabel.setText("");
        }
    }

    private void populateOraHomeCombo()
    {
        oraHomeNameCombo.removeAll();
        for (OracleHomeDescriptor home : OCIUtils.oraHomes) {
            oraHomeNameCombo.add(home.getOraHomeName());
        }
        oraHomeNameCombo.add(BROWSE);
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

        tnsNameCombo = new Combo(targetContainer, SWT.DROP_DOWN);
        tnsNameCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        populateTnsNameCombo();
        tnsNameCombo.addModifyListener(controlModifyListener);
    }

    private void populateTnsNameCombo() {
        tnsNameCombo.removeAll();
        String oraHome = null;
        if (oraHomeNameCombo != null) {
            oraHome = oraHomeNameCombo.getText();
        }
        if (CommonUtils.isEmpty(oraHome)) {
            if (!OCIUtils.oraHomes.isEmpty()) {
                oraHome = OCIUtils.oraHomes.get(0).getOraHomeName();
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
        {
            testButton = new Button(bottomControls, SWT.PUSH);
            testButton.setText("Test Connection ... ");
            testButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
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
    }

    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    public boolean isComplete()
    {
        if (isOCI && oraHomeNameCombo.getText().isEmpty()) {
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

    public void loadSettings()
    {
        isOCI = OCIUtils.isOciDriver(site.getDriver());
        if (isOCI) {
            if (oraHomeSelector == null || oraHomeSelector.isDisposed()) {
                oraHomeSelector = createOraHomeSelector(bottomControls);
            }
        }
        else {
            if (oraHomeSelector != null) {
                oraHomeSelector.dispose();
            }
        }

        tnsNameCombo.setEnabled(isOCI);

        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            Map<Object,Object> connectionProperties = connectionInfo.getProperties();

            if (isOCI) {
                Object oraHome = connectionProperties.get(OracleConstants.PROP_ORA_HOME);
                if (oraHome != null) {
                    oraHomeNameCombo.setText(oraHome.toString());
                }
                else {
                    if (!OCIUtils.oraHomes.isEmpty()) {
                        oraHomeNameCombo.setText(OCIUtils.oraHomes.get(0).getOraHomeName());
                    }
                }
                displayClientVersion();
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
            Map<Object, Object> connectionProperties = connectionInfo.getProperties();
            if (propertySource != null) {
                connectionProperties.putAll(propertySource.getProperties());
            }

            if (isOCI) {
                String oraHome = oraHomeNameCombo.getText();
                if (!BROWSE.equals(oraHome) && !oraHome.isEmpty()) {
                    connectionProperties.put(OracleConstants.PROP_ORA_HOME, oraHome);
                }
                else {
                    connectionProperties.remove(OracleConstants.PROP_ORA_HOME);
                }
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
                connectionProperties.put(OracleConstants.PROP_INTERNAL_LOGON, userRoleCombo.getText().toLowerCase());
            } else {
                connectionProperties.remove(OracleConstants.PROP_INTERNAL_LOGON);
            }
        }
    }

    private void generateConnectionURL(DBPConnectionInfo connectionInfo)
    {
        StringBuilder url = new StringBuilder(100);
        url.append("jdbc:oracle:");
        if (isOCI) {
            url.append("oci");
        } else {
            url.append("thin");
        }
        url.append(":@");
        if (CommonUtils.isEmpty(connectionInfo.getHostName()) && !CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            // TNS name specified
            url.append(connectionInfo.getDatabaseName());
        } else {
            // Basic connection info specified
            if (!isOCI) {
                url.append("//");
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                url.append(connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                url.append(":");
                url.append(connectionInfo.getHostPort());
            }
            if (isOCI) {
                url.append(":");
            } else {
                url.append("/");
            }
            if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                url.append(connectionInfo.getDatabaseName());
            }
        }
        connectionInfo.setUrl(url.toString());
    }

    private void updateUI()
    {
        site.updateButtons();
        if (testButton != null) {
            testButton.setEnabled(this.isComplete());
        }
    }

    private class ControlsListener implements ModifyListener, SelectionListener {
        public void modifyText(ModifyEvent e) {
            updateUI();
        }
        public void widgetSelected(SelectionEvent e) {
            updateUI();
        }
        public void widgetDefaultSelected(SelectionEvent e) {
            updateUI();
        }
    }

    private class OraHomeSelectionListener implements SelectionListener
    {
        public OraHomeSelectionListener()
        {
        }

        public void widgetSelected(SelectionEvent e)
        {
            if (BROWSE.equals(oraHomeNameCombo.getText())) {
                String dir = directoryDialog.open();
                if (dir != null) {
                    OracleHomeDescriptor oraHome = OCIUtils.getOraHome(dir);
                    if (oraHome == null) {
                        // add new Ora home
                        try {
                            oraHome = OCIUtils.addOraHome(dir);
                            populateOraHomeCombo();
                        } catch (DBException ex) {
                            log.warn("Wrong Oracle client home " + oraHome, ex);
                            UIUtils.showMessageBox(getShell(), "Select Oracle home", ex.getMessage(), SWT.ICON_ERROR);

                            // restore the previous home
                            String home = (String) site.getConnectionInfo().getProperties().get(OracleConstants.PROP_ORA_HOME);
                            if (!CommonUtils.isEmpty(home)) {
                                oraHomeNameCombo.setText(home);
                            }
                            return;
                        }
                    }
                    if (oraHome != null) {
                        oraHomeNameCombo.setText(oraHome.getOraHomeName());
                    }
                    else {
                        oraHomeNameCombo.setText("");
                    }
                } else {
                    oraHomeNameCombo.setText("");
                }
            }
            displayClientVersion();
            populateTnsNameCombo();
        }

        public void widgetDefaultSelected(SelectionEvent e)
        {
        }
    }
}
