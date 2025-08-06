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
package org.jkiss.dbeaver.ext.generic.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.internal.GenericMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.InvalidPathException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GenericConnectionPage
 */
public class GenericConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {
    private static final Log log = Log.getLog(GenericConnectionPage.class);

    // Host/port
    private Text hostText;
    private Text portText;
    // server/DB/path
    private Text serverText;
    private Text dbText;
    private Text pathText;
    // URL
    private Text urlText;

    private boolean isCustom;
    private DatabaseURL.MetaURL metaURL;
    private Collection<String> controlGroupsByUrl;
    private Composite settingsGroup;

    private static final String GROUP_URL = "url"; //$NON-NLS-1$
    private static final String GROUP_HOST = "host"; //$NON-NLS-1$
    private static final String GROUP_SERVER = "server"; //$NON-NLS-1$
    private static final String GROUP_DB = "db"; //$NON-NLS-1$
    private static final String GROUP_PATH = "path"; //$NON-NLS-1$
    private static final String GROUP_LOGIN = "login"; //$NON-NLS-1$
    private boolean activated;
    
    private static final Map<String, String> controlGroupByUrlProp = Map.of(
        DBConstants.PROP_HOST, GROUP_HOST,
        DBConstants.PROP_SERVER, GROUP_SERVER,
        DBConstants.PROP_DATABASE, GROUP_DB,
        DBConstants.PROP_FOLDER, GROUP_PATH,
        DBConstants.PROP_FILE, GROUP_PATH
    );

    @Override
    public void createControl(Composite composite) {
        ModifyListener textListener = e -> {
            if (activated) {
                saveAndUpdate();
            }
        };

        Composite addrGroup = new Composite(composite, SWT.NONE);
        addrGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        settingsGroup = UIUtils.createControlGroup(addrGroup, GenericMessages.dialog_connection_general_tab, 4, GridData.FILL_HORIZONTAL, 0);
        GridLayout gl = new GridLayout(4, false);
        settingsGroup.setLayout(gl);

        {
            SelectionAdapter typeSwitcher = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (controlGroupsByUrl.size() > 0) {
                        setupConnectionModeSelection(urlText, typeURLRadio.getSelection(), controlGroupsByUrl);
                    }
                    saveAndUpdate();
                }
            };
            createConnectionModeSwitcher(settingsGroup, typeSwitcher);

            
            Label urlLabel = UIUtils.createControlLabel(settingsGroup, GenericMessages.dialog_connection_jdbc_url_);
            urlLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            urlText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 355;
            urlText.setLayoutData(gd);
            urlText.addModifyListener(e -> site.updateButtons());

            addControlToGroup(GROUP_URL, urlLabel);
            addControlToGroup(GROUP_URL, urlText);
        }
        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText(GenericMessages.dialog_connection_host_label);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            Label portLabel = new Label(settingsGroup, SWT.NONE);
            portLabel.setText(GenericMessages.dialog_connection_port_label);
            portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            portText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.CENTER);
            gd.widthHint = UIUtils.getFontHeight(portText) * 7;
            portText.setLayoutData(gd);
            //portText.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
            portText.addModifyListener(textListener);

            addControlToGroup(GROUP_HOST, hostLabel);
            addControlToGroup(GROUP_HOST, hostText);
            addControlToGroup(GROUP_HOST, portLabel);
            addControlToGroup(GROUP_HOST, portText);
        }

        {
            Label serverLabel;
            DBPDriver driver = site.getActiveDataSource().getDriver();
            String dbTerm = (String) driver.getDriverParameter(GenericConstants.PARAM_TERM_SERVER);
            if (CommonUtils.isNotEmpty(dbTerm)) {
                serverLabel = UIUtils.createControlLabel(settingsGroup, dbTerm);
            } else {
                serverLabel = new Label(settingsGroup, SWT.NONE);
                serverLabel.setText(GenericMessages.dialog_connection_server_label);
            }

            serverLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            serverText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.widthHint = 270;
            serverText.setLayoutData(gd);
            serverText.addModifyListener(textListener);

            Control emptyLabel = UIUtils.createEmptyLabel(settingsGroup, 2, 1);

            addControlToGroup(GROUP_SERVER, serverLabel);
            addControlToGroup(GROUP_SERVER, serverText);
            addControlToGroup(GROUP_SERVER, emptyLabel);
        }

        {
            Label dbLabel;
            DBPDriver driver = site.getActiveDataSource().getDriver();
            String dbTerm = (String) driver.getDriverParameter(GenericConstants.PARAM_TERM_DATABASE);
            if (CommonUtils.isNotEmpty(dbTerm)) {
                dbLabel = UIUtils.createControlLabel(settingsGroup, dbTerm);
            } else {
                dbLabel = new Label(settingsGroup, SWT.NONE);
                dbLabel.setText(GenericMessages.dialog_connection_database_schema_label);
            }
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            dbText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.widthHint = 270;
            //gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);
            dbText.addModifyListener(textListener);

            Control emptyLabel = UIUtils.createEmptyLabel(settingsGroup, 2, 1);

            addControlToGroup(GROUP_DB, dbLabel);
            addControlToGroup(GROUP_DB, dbText);
            addControlToGroup(GROUP_DB, emptyLabel);
        }

        // Path
        {
            Label pathLabel = new Label(settingsGroup, SWT.NONE);
            pathLabel.setText(GenericMessages.dialog_connection_path_label);
            pathLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            pathText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 200;
            gd.horizontalSpan = 2;
            pathText.setLayoutData(gd);
            pathText.addModifyListener(textListener);

            Composite buttonsPanel = new Composite(settingsGroup, SWT.NONE);
            gl = new GridLayout(1, true);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            buttonsPanel.setLayout(gl);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            //gd.widthHint = 150;
            buttonsPanel.setLayoutData(gd);

            UIUtils.createDialogButton(buttonsPanel, GenericMessages.dialog_connection_browse_button, null, GenericMessages.dialog_connection_browse_button_tip, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final String path = showDatabaseFileSelectorDialog(SWT.OPEN);
                    if (path != null) {
                        pathText.setText(path);
                    }
                }
            });

            if (CommonUtils.toBoolean(site.getDriver().getDriverParameter(GenericConstants.PARAM_SUPPORTS_EMBEDDED_DATABASE_CREATION))) {
                gl.numColumns += 1;
                UIUtils.createDialogButton(
                    buttonsPanel,
                    GenericMessages.dialog_connection_create_button,
                    null,
                    GenericMessages.dialog_connection_create_button_tip,
                    new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            final String path = showDatabaseFileSelectorDialog(SWT.SAVE);
                            if (path != null) {
                                pathText.setText(path);
                                if (canCreateEmbeddedDatabase()) {
                                    createEmbeddedDatabase();
                                }
                            }
                        }
                    });
            }

            addControlToGroup(GROUP_PATH, pathLabel);
            addControlToGroup(GROUP_PATH, pathText);
            addControlToGroup(GROUP_PATH, buttonsPanel);
        }

        if (isAuthEnabled()) {
            createAuthPanel(addrGroup, 4);
            addControlToGroup(GROUP_LOGIN, getAuthPanelComposite());
        }

        createAdvancedSettingsGroup(addrGroup);

        createDriverPanel(addrGroup);
        setControl(addrGroup);
    }

    @Nullable
    private String showDatabaseFileSelectorDialog(int style) {
        if (metaURL.getAvailableProperties().contains(DBConstants.PROP_FILE)) {
            FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE | style);
            String text = pathText.getText();
            dialog.setFileName(text);
            if (CommonUtils.isNotEmpty(text)) {
                try {
                    String directoryPath = IOUtils.getDirectoryPath(text);
                    if (CommonUtils.isNotEmpty(directoryPath)) {
                        dialog.setFilterPath(directoryPath);
                    }
                } catch (InvalidPathException ex) {
                    // Can't find directory path. Ignore it then.
                    log.debug("Can't find directory path", ex);
                }
            }
            dialog.setText(GenericMessages.dialog_connection_db_file_chooser_text);
            return dialog.open();
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
            dialog.setText(GenericMessages.dialog_connection_db_folder_chooser_text);
            dialog.setMessage(GenericMessages.dialog_connection_db_folder_chooser_message);
            return dialog.open();
        }
    }

    public void createAdvancedSettingsGroup(Composite composite) {

    }

    public void setPortText(String text) {
        portText.setText(text);
        saveAndUpdate();
    }

    @Override
    protected void updateDriverInfo(DBPDriver driver) {
        if (!isCustom) {
            site.getActiveDataSource().getConnectionConfiguration().setUrl(null);
        }
        parseSampleURL(driver);
        setupConnectionModeSelection(urlText, this.isCustomURL(), controlGroupsByUrl);
        saveAndUpdate();
    }

    @Override
    public boolean isComplete() {
        if (isCustomURL()) {
            return !CommonUtils.isEmpty(urlText.getText());
        } else {
            if (metaURL == null) {
                return false;
            }
            for (String prop : metaURL.getRequiredProperties()) {
                if (isConnectionPropertyOptional(prop)) {
                    continue;
                }
                if (
                    (prop.equals(DBConstants.PROP_HOST) && CommonUtils.isEmptyTrimmed(hostText.getText())) ||
                    (prop.equals(DBConstants.PROP_PORT) && CommonUtils.isEmptyTrimmed(portText.getText())) ||
                    (prop.equals(DBConstants.PROP_DATABASE) && CommonUtils.isEmptyTrimmed(dbText.getText())) ||
                    ((prop.equals(DBConstants.PROP_FILE) || prop.equals(DBConstants.PROP_FOLDER)) && CommonUtils.isEmptyTrimmed(pathText.getText())))
                {
                    return false;
                }
            }
            return true;
        }
    }

    // Needed to make some properties optional although they are specified as required in URL pattern
    // DS provider may pre-populate default values for them
    protected boolean isConnectionPropertyOptional(String property) {
        return false;
    }

    @Override
    protected boolean isCustomURL() {
        return isCustom || (typeURLRadio != null && typeURLRadio.getSelection());
    }

    @Override
    public Image getImage() {
        DBPDriver driver = getSite().getDriver();
        DBPImage icon = driver.getLogoImage();
        if (icon == null) {
            icon = driver.getIconBig();
        }
        if (icon != null) {
            try {
                Image image = DBeaverIcons.getImage(icon);
                if (image.getImageData().width >= 64) {
                    return image;
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        return super.getImage();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        // Load values from new connection info
        final DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        this.parseSampleURL(site.getDriver());
        final boolean useURL = connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL;
        if (controlGroupsByUrl.size() > 0) {
            setupConnectionModeSelection(urlText, useURL, controlGroupsByUrl);
        }
        site.updateButtons();
        if (!isCustom) {
            if (hostText != null) {
                if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                    hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
                } else {
                    hostText.setText(CommonUtils.toString(site.getDriver().getDefaultHost(), DBConstants.HOST_LOCALHOST)); //$NON-NLS-1$
                }
            }
            if (portText != null) {
                if (site.isNew() && CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    portText.setText(CommonUtils.notEmpty(site.getDriver().getDefaultPort()));
                } else if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    portText.setText(connectionInfo.getHostPort());
                }
            }
            if (serverText != null) {
                if (site.isNew() && CommonUtils.isEmpty(connectionInfo.getServerName())) {
                    serverText.setText(CommonUtils.notEmpty(site.getDriver().getDefaultServer()));
                } else {
                    serverText.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
                }
            }
            if (dbText != null) {
                if (site.isNew() && CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    dbText.setText(CommonUtils.notEmpty(site.getDriver().getDefaultDatabase()));
                } else {
                    dbText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
                }
            }
            if (pathText != null) {
                pathText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
            }
        } else {
            hostText.setText(""); //$NON-NLS-1$
            portText.setText(""); //$NON-NLS-1$
            serverText.setText(""); //$NON-NLS-1$
            dbText.setText(""); //$NON-NLS-1$
            pathText.setText(""); //$NON-NLS-1$
        }

        if (urlText != null) {
            if (CommonUtils.isEmpty(connectionInfo.getUrl())) {
                try {
                    saveSettings(dataSource);
                } catch (Exception e) {
                    setMessage(e.getMessage());
                }
            }

            if (isCustomURL() && typeURLRadio != null && !typeURLRadio.isVisible() && CommonUtils.isEmpty(connectionInfo.getUrl())) {
                urlText.setText(CommonUtils.notEmpty(dataSource.getDriver().getSampleURL()));
            } else if (connectionInfo.getUrl() != null) {
                urlText.setText(CommonUtils.notEmpty(connectionInfo.getUrl()));
            } else {
                urlText.setText(""); //$NON-NLS-1$
            }
        }

        activated = true;

        UIUtils.asyncExec(() -> {
            // Set first control
            if (CommonUtils.isEmpty(site.getDriver().getSampleURL())) {
                urlText.setFocus();
            } else  if (hostText != null && hostText.isVisible()) {
                hostText.setFocus();
            } else  if (serverText != null && serverText.isVisible()) {
                serverText.setFocus();
            } else  if (dbText != null && dbText.isVisible()) {
                dbText.setFocus();
            } else  if (pathText != null && pathText.isVisible()) {
                pathText.setFocus();
            }
        });

    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        final Set<String> properties = metaURL == null ? Collections.emptySet() : metaURL.getAvailableProperties();

        connectionInfo.setConfigurationType(
            typeURLRadio != null && typeURLRadio.getSelection() ? DBPDriverConfigurationType.URL : DBPDriverConfigurationType.MANUAL);

        if (hostText != null && properties.contains(DBConstants.PROP_HOST)) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null && properties.contains(DBConstants.PROP_PORT)) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (serverText != null && properties.contains(DBConstants.PROP_SERVER)) {
            connectionInfo.setServerName(serverText.getText().trim());
        }
        if (dbText != null && properties.contains(DBConstants.PROP_DATABASE)) {
            connectionInfo.setDatabaseName(dbText.getText().trim());
        }
        if (pathText != null && (properties.contains(DBConstants.PROP_FOLDER) || properties.contains(DBConstants.PROP_FILE))) {
            connectionInfo.setDatabaseName(pathText.getText().trim());
        }

        super.saveSettings(dataSource);

        if (isCustomURL()) {
            if (urlText != null) {
                connectionInfo.setUrl(urlText.getText().trim());
            }
        } else {
            if (urlText != null && connectionInfo.getUrl() != null) {
                urlText.setText(connectionInfo.getUrl());
            }
        }
    }

    private void parseSampleURL(DBPDriver driver) {
        metaURL = null;

        boolean useCustomUrl = CommonUtils.isEmpty(driver.getSampleURL());

        if (!useCustomUrl) {
            try {
                metaURL = DatabaseURL.parseSampleURL(driver.getSampleURL());
            } catch (DBException e) {
                setErrorMessage(e.getMessage());
            }
            final Set<String> properties = metaURL.getAvailableProperties();
            boolean isSampleUrlUsable = properties.contains(DBConstants.PROP_HOST) ||
                properties.contains(DBConstants.PROP_DATABASE) ||
                properties.contains(DBConstants.PROP_SERVER) ||
                properties.contains(DBConstants.PROP_FOLDER) ||
                properties.contains(DBConstants.PROP_FILE);
            if (isSampleUrlUsable) {
                isCustom = false;
                showControlGroup(GROUP_HOST, properties.contains(DBConstants.PROP_HOST));
                showControlGroup(GROUP_SERVER, properties.contains(DBConstants.PROP_SERVER));
                showControlGroup(GROUP_DB, properties.contains(DBConstants.PROP_DATABASE));
                showControlGroup(GROUP_PATH, properties.contains(DBConstants.PROP_FOLDER) || properties.contains(DBConstants.PROP_FILE));
                showControlGroup(GROUP_CONNECTION_MODE, true);
                urlText.setEditable(false);
                controlGroupsByUrl = properties.stream().map(controlGroupByUrlProp::get).collect(Collectors.toSet());
            } else {
                useCustomUrl = true;
            }
        }

        if (useCustomUrl) {
            isCustom = true;
            showControlGroup(GROUP_HOST, false);
            showControlGroup(GROUP_SERVER, false);
            showControlGroup(GROUP_DB, false);
            showControlGroup(GROUP_PATH, false);
            showControlGroup(GROUP_CONNECTION_MODE, false);
            urlText.setEditable(true);
            controlGroupsByUrl = Collections.emptyList();
        }
        UIUtils.fixReadonlyTextBackground(urlText);
        showControlGroup(GROUP_LOGIN, !driver.isAnonymousAccess());

        settingsGroup.getParent().layout();
    }

    private boolean canCreateEmbeddedDatabase() {
        final String param = CommonUtils.toString(site.getDriver().getDriverParameter(GenericConstants.PARAM_CREATE_URL_PARAM));
        return !CommonUtils.isEmpty(param) && !CommonUtils.isEmptyTrimmed(pathText.getText());
    }

    private void createEmbeddedDatabase() {
        String paramCreate = CommonUtils.toString(site.getDriver().getDriverParameter(GenericConstants.PARAM_CREATE_URL_PARAM));

        DataSourceDescriptor dataSource = (DataSourceDescriptor) site.getActiveDataSource();
        DataSourceDescriptor testDataSource = site.getDataSourceRegistry().createDataSource(
            dataSource.getId(),
            dataSource.getDriver(),
            new DBPConnectionConfiguration(dataSource.getConnectionConfiguration())
        );

        saveSettings(testDataSource);
        DBPConnectionConfiguration cfg = testDataSource.getConnectionConfiguration();
        cfg.setDatabaseName(cfg.getDatabaseName() + paramCreate);
        String databaseName = cfg.getDatabaseName();
        testDataSource.setName(databaseName);

        if (!UIUtils.confirmAction(getShell(), "Create Database", "Are you sure you want to create database '" + databaseName + "'?")) {
            testDataSource.dispose();
            return;
        }

        try {
            site.getRunnableContext().run(true, true, monitor -> {
                try {
                    createEmbeddedDatabase(monitor, testDataSource);
                } catch (DBException e1) {
                    throw new InvocationTargetException(e1);
                }
            });
            MessageDialog.openInformation(getShell(), "Database Create", "Database '" + databaseName + "' created!");
        } catch (InvocationTargetException e1) {
            DBWorkbench.getPlatformUI().showError("Create database", "Error creating database", e1.getTargetException());
        } catch (InterruptedException e1) {
            // Just ignore
        }
    }

    private void createEmbeddedDatabase(DBRProgressMonitor monitor, DataSourceDescriptor testDataSource) throws DBException {
        try {
            // Connect and disconnect immediately
            testDataSource.connect(monitor, true, true);

            testDataSource.disconnect(monitor);
        } finally {
            testDataSource.dispose();
        }
    }

    private void saveAndUpdate() {
        // Save settings to update URL
        saveSettings(site.getActiveDataSource());
        // Update buttons
        site.updateButtons();
    }

    private void showControlGroup(String group, boolean show) {
        Set<Control> controlList = propGroupMap.get(group);
        if (controlList != null) {
            for (Control control : controlList) {
                Object gd = control.getLayoutData();
                if (gd == null) {
                    gd = new GridData(GridData.BEGINNING);
                    control.setLayoutData(gd);
                }
                if (gd instanceof GridData) {
                    ((GridData)gd).exclude = !show;
                }
                control.setVisible(show);
            }
        }
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {
            new DriverPropertiesDialogPage(this)
        };
    }

}
