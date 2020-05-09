/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.internal.GenericMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
 * GenericConnectionPage
 */
public class GenericConnectionPage extends ConnectionPageWithAuth implements ICompositeDialogPage
{
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
    private JDBCURL.MetaURL metaURL;

    private Composite settingsGroup;

    private Map<String, List<Control>> propGroupMap = new HashMap<>();

    private static final String GROUP_URL = "url"; //$NON-NLS-1$
    private static final String GROUP_HOST = "host"; //$NON-NLS-1$
    private static final String GROUP_SERVER = "server"; //$NON-NLS-1$
    private static final String GROUP_DB = "db"; //$NON-NLS-1$
    private static final String GROUP_PATH = "path"; //$NON-NLS-1$
    private static final String GROUP_LOGIN = "login"; //$NON-NLS-1$
    private boolean activated;
    private Button createButton;

    @Override
    public void createControl(Composite composite)
    {
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
            Label urlLabel = new Label(settingsGroup, SWT.NONE);
            urlLabel.setText(GenericMessages.dialog_connection_jdbc_url_);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            urlLabel.setLayoutData(gd);

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
            gd.widthHint = 60;
            portText.setLayoutData(gd);
            //portText.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
            portText.addModifyListener(textListener);

            addControlToGroup(GROUP_HOST, hostLabel);
            addControlToGroup(GROUP_HOST, hostText);
            addControlToGroup(GROUP_HOST, portLabel);
            addControlToGroup(GROUP_HOST, portText);
        }

        {
            Label serverLabel = new Label(settingsGroup, SWT.NONE);
            serverLabel.setText(GenericMessages.dialog_connection_server_label);
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
            Label dbLabel = new Label(settingsGroup, SWT.NONE);
            dbLabel.setText(GenericMessages.dialog_connection_database_schema_label);
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
            pathText.addModifyListener(e -> updateCreateButton(site.getDriver()));

            Composite buttonsPanel = new Composite(settingsGroup, SWT.NONE);
            gl = new GridLayout(2, true);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            buttonsPanel.setLayout(gl);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            //gd.widthHint = 150;
            buttonsPanel.setLayoutData(gd);

            UIUtils.createDialogButton(
                buttonsPanel,
                GenericMessages.dialog_connection_browse_button,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        if (metaURL.getAvailableProperties().contains(JDBCConstants.PROP_FILE)) {
                            FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
                            dialog.setFileName(pathText.getText());
                            dialog.setText(GenericMessages.dialog_connection_db_file_chooser_text);
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
                            dialog.setText(GenericMessages.dialog_connection_db_folder_chooser_text);
                            dialog.setMessage(GenericMessages.dialog_connection_db_folder_chooser_message);
                            String folder = dialog.open();
                            if (folder != null) {
                                pathText.setText(folder);
                            }
                        }
                    }
                });

            createButton = UIUtils.createDialogButton(buttonsPanel, "Create",
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        createEmbeddedDatabase();
                    }
                });
            createButton.setEnabled(false);

            addControlToGroup(GROUP_PATH, pathLabel);
            addControlToGroup(GROUP_PATH, pathText);
            addControlToGroup(GROUP_PATH, buttonsPanel);
        }

        {
            createAuthPanel(addrGroup, 4);
            addControlToGroup(GROUP_LOGIN, getAuthPanelComposite());
        }

        createDriverPanel(addrGroup);
        setControl(addrGroup);
    }

    @Override
    protected void updateDriverInfo(DBPDriver driver) {
        if (!isCustom) {
            site.getActiveDataSource().getConnectionConfiguration().setUrl(null);
        }
        parseSampleURL(driver);
        saveAndUpdate();
    }

    @Override
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
                    (prop.equals(JDBCConstants.PROP_HOST) && CommonUtils.isEmptyTrimmed(hostText.getText())) ||
                    (prop.equals(JDBCConstants.PROP_PORT) && CommonUtils.isEmptyTrimmed(portText.getText())) ||
                    (prop.equals(JDBCConstants.PROP_DATABASE) && CommonUtils.isEmptyTrimmed(dbText.getText())) ||
                    ((prop.equals(JDBCConstants.PROP_FILE) || prop.equals(JDBCConstants.PROP_FOLDER)) && CommonUtils.isEmptyTrimmed(pathText.getText())))
                {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    protected boolean isCustomURL()
    {
        return isCustom;
    }

    @Override
    public void loadSettings()
    {
        super.loadSettings();

        {
            DBPDriver driver = getSite().getDriver();
            DBPImage iconBig = driver.getIconBig();
            if (iconBig != null) {
                try {
                    ImageDescriptor imageDescriptor = DBeaverIcons.getImageDescriptor(iconBig);
                    if (imageDescriptor.getImageData().width >= 64) {
                        setImageDescriptor(imageDescriptor);
                    } else {
                        setImageDescriptor(null);
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        this.parseSampleURL(site.getDriver());
        if (!isCustom) {
            if (hostText != null) {
                if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                    hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
                } else {
                    hostText.setText("localhost"); //$NON-NLS-1$
                }
            }
            if (portText != null) {
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    portText.setText(String.valueOf(connectionInfo.getHostPort()));
                } else if (site.getDriver().getDefaultPort() != null) {
                    portText.setText(site.getDriver().getDefaultPort());
                } else {
                    portText.setText(""); //$NON-NLS-1$
                }
            }
            if (serverText != null) {
                serverText.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
            }
            if (dbText != null) {
                dbText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
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
                    saveSettings(site.getActiveDataSource());
                } catch (Exception e) {
                    setMessage(e.getMessage());
                }
            }
            if (connectionInfo.getUrl() != null) {
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
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        final Set<String> properties = metaURL == null ? Collections.emptySet() : metaURL.getAvailableProperties();

        if (hostText != null && properties.contains(JDBCConstants.PROP_HOST)) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null && properties.contains(JDBCConstants.PROP_PORT)) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (serverText != null && properties.contains(JDBCConstants.PROP_SERVER)) {
            connectionInfo.setServerName(serverText.getText().trim());
        }
        if (dbText != null && properties.contains(JDBCConstants.PROP_DATABASE)) {
            connectionInfo.setDatabaseName(dbText.getText().trim());
        }
        if (pathText != null && (properties.contains(JDBCConstants.PROP_FOLDER) || properties.contains(JDBCConstants.PROP_FILE))) {
            connectionInfo.setDatabaseName(pathText.getText().trim());
        }

        super.saveSettings(dataSource);

        if (isCustom) {
            if (urlText != null) {
                connectionInfo.setUrl(urlText.getText().trim());
            }
        } else {
            if (urlText != null && connectionInfo.getUrl() != null) {
                urlText.setText(connectionInfo.getUrl());
            }
        }
    }

    private void parseSampleURL(DBPDriver driver)
    {
        metaURL = null;

        if (!CommonUtils.isEmpty(driver.getSampleURL())) {
            isCustom = false;
            try {
                metaURL = JDBCURL.parseSampleURL(driver.getSampleURL());
            } catch (DBException e) {
                setErrorMessage(e.getMessage());
            }
            final Set<String> properties = metaURL.getAvailableProperties();
            urlText.setEditable(false);

            showControlGroup(GROUP_HOST, properties.contains(JDBCConstants.PROP_HOST));
            showControlGroup(GROUP_SERVER, properties.contains(JDBCConstants.PROP_SERVER));
            showControlGroup(GROUP_DB, properties.contains(JDBCConstants.PROP_DATABASE));
            showControlGroup(GROUP_PATH, properties.contains(JDBCConstants.PROP_FOLDER) || properties.contains(JDBCConstants.PROP_FILE));
        } else {
            isCustom = true;
            showControlGroup(GROUP_HOST, false);
            showControlGroup(GROUP_SERVER, false);
            showControlGroup(GROUP_DB, false);
            showControlGroup(GROUP_PATH, false);
            urlText.setEditable(true);
        }
        UIUtils.fixReadonlyTextBackground(urlText);
        showControlGroup(GROUP_LOGIN, !driver.isAnonymousAccess());
        updateCreateButton(driver);

        settingsGroup.getParent().layout();
    }

    private void updateCreateButton(DBPDriver driver) {
        if (driver == null) {
            createButton.setEnabled(false);
            return;
        }
        // Enable ""Create" button
        String paramCreate = CommonUtils.toString(driver.getDriverParameter(GenericConstants.PARAM_CREATE_URL_PARAM));
        createButton.setEnabled(!CommonUtils.isEmpty(paramCreate) && !CommonUtils.isEmpty(pathText.getText()));
    }

    private void createEmbeddedDatabase() {
        String paramCreate = CommonUtils.toString(site.getDriver().getDriverParameter(GenericConstants.PARAM_CREATE_URL_PARAM));

        DataSourceDescriptor dataSource = (DataSourceDescriptor) site.getActiveDataSource();
        final DataSourceDescriptor testDataSource = new DataSourceDescriptor(
            site.getDataSourceRegistry(),
            dataSource.getId(),
            dataSource.getDriver(),
            new DBPConnectionConfiguration(dataSource.getConnectionConfiguration()));

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

    private void saveAndUpdate()
    {
        // Save settings to update URL
        saveSettings(site.getActiveDataSource());
        // Update buttons
        site.updateButtons();
    }

    private void showControlGroup(String group, boolean show)
    {
        List<Control> controlList = propGroupMap.get(group);
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

    private void addControlToGroup(String group, Control control)
    {
        List<Control> controlList = propGroupMap.computeIfAbsent(
            group,
            k -> new ArrayList<>());
        controlList.add(control);
    }

    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
            new DriverPropertiesDialogPage(this)
        };
    }

}
