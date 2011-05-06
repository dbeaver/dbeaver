/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.views;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

import java.io.File;
import java.util.*;
import java.util.List;

/**
 * ConnectionEditorPage
 */
public class ConnectionEditorPage extends DialogPage implements IDataSourceConnectionEditor
{
    private static final String PROP_HOST = "host";
    private static final String PROP_PORT = "port";
    private static final String PROP_DATABASE = "database";
    private static final String PROP_SERVER = "server";
    private static final String PROP_FOLDER = "folder";
    private static final String PROP_FILE = "file";

    private IDataSourceConnectionEditorSite site;
    // Host/port
    private Text hostText;
    private Text portText;
    // server/DB/path
    private Text serverText;
    private Text dbText;
    private Text pathText;
    // Login
    private Text userNameText;
    private Text passwordText;
    // URL
    private Text urlText;

    private Button testButton;

    private boolean isCustom;
    private DriverDescriptor.MetaURL metaURL;

    private boolean driverPropsLoaded;
    private Composite settingsGroup;
    private ConnectionPropertiesControl propsControl;

    private Map<String, List<Control>> propGroupMap = new HashMap<String, List<Control>>();

    private static final String GROUP_URL = "url";
    private static final String GROUP_HOST = "host";
    private static final String GROUP_SERVER = "server";
    private static final String GROUP_DB = "db";
    private static final String GROUP_PATH = "path";
    private static final String GROUP_LOGIN = "login";
    private PropertySourceCustom propertySource;

    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));

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
        propsTab.setControl(createPropertiesTab(optionsFolder));

        optionsFolder.addSelectionListener(
            new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    if (e.item == propsTab) {
                        refreshDriverProperties();
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

        settingsGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        settingsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        {
            Label urlLabel = new Label(settingsGroup, SWT.NONE);
            urlLabel.setText("JDBC URL:");
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            urlLabel.setLayoutData(gd);

            urlText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 355;
            urlText.setLayoutData(gd);
            urlText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e)
                {
                    site.updateButtons();
                    testButton.setEnabled(isComplete());
                }
            });

            addControlToGroup(GROUP_URL, urlLabel);
            addControlToGroup(GROUP_URL, urlText);
        }
        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText("Host:");
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            Label portLabel = new Label(settingsGroup, SWT.NONE);
            portLabel.setText("Port:");
            portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            portText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.CENTER);
            gd.widthHint = 60;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
            portText.addModifyListener(textListener);

            addControlToGroup(GROUP_HOST, hostLabel);
            addControlToGroup(GROUP_HOST, hostText);
            addControlToGroup(GROUP_HOST, portLabel);
            addControlToGroup(GROUP_HOST, portText);
        }

        {
            Label serverLabel = new Label(settingsGroup, SWT.NONE);
            serverLabel.setText("Server:");
            serverLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            serverText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 270;
            serverText.setLayoutData(gd);
            serverText.addModifyListener(textListener);

            Control emptyLabel = createEmptyLabel(settingsGroup, 1);

            addControlToGroup(GROUP_SERVER, serverLabel);
            addControlToGroup(GROUP_SERVER, serverText);
            addControlToGroup(GROUP_SERVER, emptyLabel);
        }

        {
            Label dbLabel = new Label(settingsGroup, SWT.NONE);
            dbLabel.setText("Database/Schema:");
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            dbText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 270;
            //gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);
            dbText.addModifyListener(textListener);

            Control emptyLabel = createEmptyLabel(settingsGroup, 1);

            addControlToGroup(GROUP_DB, dbLabel);
            addControlToGroup(GROUP_DB, dbText);
            addControlToGroup(GROUP_DB, emptyLabel);
        }

        {
            Label pathLabel = new Label(settingsGroup, SWT.NONE);
            pathLabel.setText("Path:");
            pathLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            pathText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            //gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 200;
            //gd.horizontalSpan = 3;
            pathText.setLayoutData(gd);
            pathText.addModifyListener(textListener);

            Button browseButton = new Button(settingsGroup, SWT.PUSH);
            browseButton.setText("Browse ... ");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            browseButton.setLayoutData(gd);
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (metaURL.getAvailableProperties().contains(PROP_FILE)) {
                        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
                        dialog.setFileName(pathText.getText());
                        dialog.setText("Choose database file");
                        String file = dialog.open();
                        if (file != null) {
                            pathText.setText(file);
                        }
                    } else {
                        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                        final String curPath = pathText.getText();
                        File curFolder = new File(curPath);
                        if (curFolder.exists()) {
                            if (curFolder.isDirectory()) {
                                dialog.setFilterPath(curFolder.getAbsolutePath());
                            } else {
                                dialog.setFilterPath(curFolder.getParentFile().getAbsolutePath());
                            }
                        }
                        dialog.setText("Choose database folder");
                        dialog.setMessage("Choose folder with database files");
                        String folder = dialog.open();
                        if (folder != null) {
                            pathText.setText(folder);
                        }
                    }
                }
            });

            addControlToGroup(GROUP_PATH, pathLabel);
            addControlToGroup(GROUP_PATH, pathText);
            addControlToGroup(GROUP_PATH, browseButton);
        }

        {
            Label userNameLabel = new Label(settingsGroup, SWT.NONE);
            userNameLabel.setText("User name:");
            userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            userNameText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.horizontalSpan = 3;
            userNameText.setLayoutData(gd);
            userNameText.addModifyListener(textListener);

            Control emptyLabel = createEmptyLabel(settingsGroup, 2);

            Label passwordLabel = new Label(settingsGroup, SWT.NONE);
            passwordLabel.setText("Password:");
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);

            addControlToGroup(GROUP_LOGIN, userNameLabel);
            addControlToGroup(GROUP_LOGIN, userNameText);
            addControlToGroup(GROUP_LOGIN, emptyLabel);
            addControlToGroup(GROUP_LOGIN, passwordLabel);
            addControlToGroup(GROUP_LOGIN, passwordText);
        }

        {
            Composite buttonsPanel = UIUtils.createPlaceholder(settingsGroup, 2);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 4;
            buttonsPanel.setLayoutData(gd);

            Button driverButton = new Button(buttonsPanel, SWT.PUSH);
            driverButton.setText("Edit Driver Settings");
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_END);
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            driverButton.setLayoutData(gd);
            driverButton.addSelectionListener(new SelectionListener()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    if (site.openDriverEditor()) {
                        parseSampleURL(site.getDriver());
                        evaluateURL();
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e)
                {
                }
            });

            testButton = new Button(buttonsPanel, SWT.PUSH);
            testButton.setText("Test Connection ... ");
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END);
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
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
        return settingsGroup;
    }

    private Control createEmptyLabel(Composite parent, int verticalSpan)
    {
        Label emptyLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 2;
        gd.verticalSpan = verticalSpan;
        gd.widthHint = 0;
        emptyLabel.setLayoutData(gd);
        return emptyLabel;
    }

    private ConnectionPropertiesControl createPropertiesTab(Composite parent)
    {
        propsControl = new ConnectionPropertiesControl(parent, SWT.NONE);
        return propsControl;
    }

    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    public boolean isComplete()
    {
        if (isCustom) {
            return !CommonUtils.isEmpty(urlText.getText());
        } else {
            if (metaURL == null) {
                return false;
            }
            for (String prop : metaURL.getRequiredProperties()) {
                if (
                    (prop.equals(PROP_HOST) && CommonUtils.isEmpty(hostText.getText())) ||
                        (prop.equals(PROP_PORT) && CommonUtils.isEmpty(portText.getText())) ||
                        (prop.equals(PROP_DATABASE) && CommonUtils.isEmpty(dbText.getText()))) {
                    return false;
                }
            }
            return true;
        }
    }

    public void loadSettings()
    {
        // Load values from new connection info
        driverPropsLoaded = false;
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (connectionInfo != null) {
            this.parseSampleURL(site.getDriver());
            if (!isCustom) {
                if (hostText != null) {
                    if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                        hostText.setText(CommonUtils.getString(connectionInfo.getHostName()));
                    } else {
                        hostText.setText("localhost");
                    }
                }
                if (portText != null) {
                    if (connectionInfo.getHostPort() > 0) {
                        portText.setText(String.valueOf(connectionInfo.getHostPort()));
                    } else if (site.getDriver().getDefaultPort() != null) {
                        portText.setText(site.getDriver().getDefaultPort().toString());
                    } else {
                        portText.setText("");
                    }
                }
                if (serverText != null) {
                    serverText.setText(CommonUtils.getString(connectionInfo.getServerName()));
                }
                if (dbText != null) {
                    dbText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
                }
                if (pathText != null) {
                    pathText.setText(CommonUtils.getString(connectionInfo.getDatabaseName()));
                }
            } else {
                hostText.setText("");
                portText.setText("");
                serverText.setText("");
                dbText.setText("");
                pathText.setText("");
            }
            if (userNameText != null) {
                userNameText.setText(CommonUtils.getString(connectionInfo.getUserName()));
            }
            if (passwordText != null) {
                passwordText.setText(CommonUtils.getString(connectionInfo.getUserPassword()));
            }

            if (urlText != null) {
                if (connectionInfo.getUrl() != null) {
                    urlText.setText(CommonUtils.getString(connectionInfo.getUrl()));
                } else {
                    if (!isCustom) {
                        evaluateURL();
                    } else {
                        urlText.setText("");
                    }
                }
            }
        }

        // Set props model
        if (propsControl != null) {
            refreshDriverProperties();
        }
    }

    private void refreshDriverProperties()
    {
        if (!driverPropsLoaded) {
            DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
            saveSettings(tmpConnectionInfo);
            tmpConnectionInfo.setProperties(site.getConnectionInfo().getProperties());
            propertySource = propsControl.makeProperties(site.getDriver(), tmpConnectionInfo/*.getUrl(), site.getConnectionInfo().getProperties()*/);
            propsControl.loadProperties(propertySource);
            driverPropsLoaded = true;
        }
    }

    public void saveSettings()
    {
        saveSettings(site.getConnectionInfo());
    }

    private void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo != null) {
            final Set<String> properties = metaURL == null ? Collections.<String>emptySet() : metaURL.getAvailableProperties();

            if (hostText != null && properties.contains(PROP_HOST)) {
                connectionInfo.setHostName(hostText.getText());
            }
            if (portText != null && properties.contains(PROP_PORT)) {
                connectionInfo.setHostPort(CommonUtils.toInt(portText.getText()));
            }
            if (serverText != null && properties.contains(PROP_SERVER)) {
                connectionInfo.setServerName(serverText.getText());
            }
            if (dbText != null && properties.contains(PROP_DATABASE)) {
                connectionInfo.setDatabaseName(dbText.getText());
            }
            if (pathText != null && (properties.contains(PROP_FOLDER) || properties.contains(PROP_FILE))) {
                connectionInfo.setDatabaseName(pathText.getText());
            }
            if (userNameText != null) {
                connectionInfo.setUserName(userNameText.getText());
            }
            if (passwordText != null) {
                connectionInfo.setUserPassword(passwordText.getText());
            }
            if (urlText != null) {
                connectionInfo.setUrl(urlText.getText());
            }
            if (propertySource != null) {
                connectionInfo.setProperties(propertySource.getProperties());
            }
        }
    }

    private void parseSampleURL(DBPDriver driver)
    {
        metaURL = null;

        if (!CommonUtils.isEmpty(driver.getSampleURL())) {
            isCustom = false;
            try {
                metaURL = DriverDescriptor.parseSampleURL(driver.getSampleURL());
            } catch (DBException e) {
                setErrorMessage(e.getMessage());
            }
/*
            // Check for required parts
            for (String component : urlComponents) {
                boolean isRequired = !component.startsWith("[");
                int divPos = component.indexOf('{');
                if (divPos != -1) {
                    int divPos2 = component.indexOf('}', divPos);
                    if (divPos2 != -1) {
                        String propName = component.substring(divPos + 1, divPos2);
                        availableProperties.add(propName);
                        if (isRequired) {
                            requiredProperties.add(propName);
                        }
                    }
                }
            }
*/
            final Set<String> properties = metaURL.getAvailableProperties();
            hostText.setEditable(properties.contains(PROP_HOST));
            portText.setEditable(properties.contains(PROP_PORT));
            dbText.setEditable(properties.contains(PROP_DATABASE));
            urlText.setEditable(false);

            showControlGroup(GROUP_HOST, properties.contains(PROP_HOST));
            showControlGroup(GROUP_SERVER, properties.contains(PROP_SERVER));
            showControlGroup(GROUP_DB, properties.contains(PROP_DATABASE));
            showControlGroup(GROUP_PATH, properties.contains(PROP_FOLDER) || properties.contains(PROP_FILE));
        } else {
            isCustom = true;
            hostText.setEditable(false);
            portText.setEditable(false);
            dbText.setEditable(false);
            showControlGroup(GROUP_HOST, false);
            showControlGroup(GROUP_SERVER, false);
            showControlGroup(GROUP_DB, false);
            showControlGroup(GROUP_PATH, false);
            urlText.setEditable(true);
        }
        showControlGroup(GROUP_LOGIN, !driver.isAnonymousAccess());

        settingsGroup.layout();
    }

    private void evaluateURL()
    {
        if (!isCustom && metaURL != null) {
            StringBuilder url = new StringBuilder();
            for (String component : metaURL.getUrlComponents()) {
                String newComponent = component;
                if (!CommonUtils.isEmpty(hostText.getText())) {
                    newComponent = newComponent.replace(makePropPattern(PROP_HOST), hostText.getText());
                }
                if (!CommonUtils.isEmpty(portText.getText())) {
                    newComponent = newComponent.replace(makePropPattern(PROP_PORT), portText.getText());
                }
                if (!CommonUtils.isEmpty(serverText.getText())) {
                    newComponent = newComponent.replace(makePropPattern(PROP_SERVER), serverText.getText());
                }
                if (!CommonUtils.isEmpty(dbText.getText())) {
                    newComponent = newComponent.replace(makePropPattern(PROP_DATABASE), dbText.getText());
                }
                if (!CommonUtils.isEmpty(pathText.getText())) {
                    newComponent = newComponent.replace(makePropPattern(PROP_FOLDER), pathText.getText());
                    newComponent = newComponent.replace(makePropPattern(PROP_FILE), pathText.getText());
                }
                if (newComponent.startsWith("[")) {
                    if (!newComponent.equals(component)) {
                        url.append(newComponent.substring(1, newComponent.length() - 1));
                    }
                } else {
                    url.append(newComponent);
                }
            }
            urlText.setText(url.toString());
        }
        site.updateButtons();
        testButton.setEnabled(this.isComplete());
    }

    private void showControlGroup(String group, boolean show)
    {
        List<Control> controlList = propGroupMap.get(group);
        if (controlList != null) {
            for (Control control : controlList) {
                GridData gd = (GridData)control.getLayoutData();
                if (gd == null) {
                    gd = new GridData(GridData.BEGINNING);
                    control.setLayoutData(gd);
                }
                gd.exclude = !show;
                control.setVisible(show);
            }
        }
    }

    private void addControlToGroup(String group, Control control)
    {
        List<Control> controlList = propGroupMap.get(group);
        if (controlList == null) {
            controlList = new ArrayList<Control>();
            propGroupMap.put(group, controlList);
        }
        controlList.add(control);
    }

    private static String makePropPattern(String prop)
    {
        return "{" + prop + "}";
    }

}
