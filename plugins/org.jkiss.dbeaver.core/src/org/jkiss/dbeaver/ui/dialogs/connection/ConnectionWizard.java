/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.IDataSourceConnectionTester;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract connection wizard
 */

public abstract class ConnectionWizard extends Wizard implements INewWizard {

    static final String PROP_CONNECTION_TYPE = "connection-type";

    private static final Log log = Log.getLog(ConnectionWizard.class);

    // protected final IProject project;
    private final Map<DriverDescriptor, DataSourceDescriptor> infoMap = new HashMap<>();
    private final List<IPropertyChangeListener> propertyListeners = new ArrayList<>();

    protected ConnectionWizard()
    {
        setNeedsProgressMonitor(true);
    }

    @Override
    public void dispose() {
        // Dispose all temp data sources
        for (DataSourceDescriptor dataSource : infoMap.values()) {
            dataSource.dispose();
        }
        super.dispose();
    }

    abstract public DBPDataSourceRegistry getDataSourceRegistry();

    abstract DBPDriver getSelectedDriver();

    public abstract ConnectionPageSettings getPageSettings();

    protected abstract void saveSettings(DataSourceDescriptor dataSource);

    @NotNull
    public DataSourceDescriptor getActiveDataSource()
    {
        DriverDescriptor driver = (DriverDescriptor) getSelectedDriver();
        DataSourceDescriptor info = infoMap.get(driver);
        if (info == null) {
            DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
            info = new DataSourceDescriptor(
                getDataSourceRegistry(),
                DataSourceDescriptor.generateNewId(getSelectedDriver()),
                driver,
                connectionInfo);
            DBPNativeClientLocation defaultClientLocation = driver.getDefaultClientLocation();
            if (defaultClientLocation != null) {
                info.getConnectionConfiguration().setClientHomeId(defaultClientLocation.getName());
            }
            info.setSavePassword(true);
            infoMap.put(driver, info);
        }
        return info;
    }

    public DataSourceDescriptor getOriginalDataSource() {
        return null;
    }


    public void testConnection()
    {
        DataSourceDescriptor dataSource = getPageSettings().getActiveDataSource();
        DataSourceDescriptor testDataSource = new DataSourceDescriptor(dataSource);

        saveSettings(testDataSource);
        testDataSource.setTemporary(true);

        // Generate new ID to avoid session conflicts in QM
        testDataSource.setId(DataSourceDescriptor.generateNewId(dataSource.getDriver()));
        testDataSource.getPreferenceStore().setValue(ModelPreferences.META_SEPARATE_CONNECTION, false);

        try {

            final ConnectionTester op = new ConnectionTester(testDataSource);

            try {
                getContainer().run(true, true, monitor -> {
                    // Wait for job to finish
                    op.ownerMonitor = RuntimeUtils.makeMonitor(monitor);
                    op.schedule();
                    while (op.getState() == Job.WAITING || op.getState() == Job.RUNNING) {
                        if (monitor.isCanceled()) {
                            op.cancel();
                            throw new InterruptedException();
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    if (op.getConnectError() != null) {
                        throw new InvocationTargetException(op.getConnectError());
                    }
                    if (op.getConnectStatus() == Status.CANCEL_STATUS) {
                        throw new InterruptedException("cancel");
                    }
                });

                String message = "";
                if (!CommonUtils.isEmpty(op.productName)) {
                    message += "Server: " + op.productName + " " + op.productVersion + "\n";
                }
                if (!CommonUtils.isEmpty(op.driverName)) {
                    message += "Driver: " + op.driverName + " " + op.driverVersion + "\n";
                }
                if (!CommonUtils.isEmpty(message)) {
                    message += "\n";
                }
                message += NLS.bind(CoreMessages.dialog_connection_wizard_start_connection_monitor_connected, op.connectTime);

                MessageDialog.openInformation(getShell(), CoreMessages.dialog_connection_wizard_start_connection_monitor_success,
                    message);
            } catch (InterruptedException ex) {
                if (!"cancel".equals(ex.getMessage())) {
                    DBWorkbench.getPlatformUI().showError(CoreMessages.dialog_connection_wizard_start_dialog_interrupted_title,
                        CoreMessages.dialog_connection_wizard_start_dialog_interrupted_message);
                }
            } catch (InvocationTargetException ex) {
                DBWorkbench.getPlatformUI().showError(
                        CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                        null,
                        GeneralUtils.makeExceptionStatus(ex.getTargetException()));
            } catch (Throwable ex) {
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                    null,
                    GeneralUtils.makeExceptionStatus(ex));
            }
        } finally {
            testDataSource.dispose();
        }
    }

    public boolean isNew()
    {
        return false;
    }

    private class ConnectionTester extends ConnectJob {
        String productName;
        String productVersion;
        String driverName;
        String driverVersion;
        long connectTime = -1;
        DBRProgressMonitor ownerMonitor;

        ConnectionTester(DataSourceDescriptor testDataSource)
        {
            super(testDataSource);
            setSystem(true);
            super.initialize = true;//CommonUtils.toBoolean(testDataSource.getDriver().getDriverParameter(DBConstants.PARAM_INIT_ON_TEST));
            productName = null;
            productVersion = null;
        }

        @Override
        public IStatus run(DBRProgressMonitor monitor)
        {
            if (ownerMonitor != null) {
                monitor = ownerMonitor;
            }
            Thread.currentThread().setName(CoreMessages.dialog_connection_wizard_start_connection_monitor_thread);

            try {
                container.setName(container.getConnectionConfiguration().getUrl());
                long startTime = System.currentTimeMillis();
                super.run(monitor);
                connectTime = (System.currentTimeMillis() - startTime);
                if (connectError != null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }
                if (connectStatus == Status.CANCEL_STATUS) {
                    return Status.CANCEL_STATUS;
                }

                // Start monitor task here becaue actual connection makes its own begin/end sequence
                monitor.beginTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_start, 3);

                DBPDataSource dataSource = container.getDataSource();
                if (dataSource == null) {
                    throw new DBException(ModelMessages.error_not_connected_to_database);
                }
                monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_subtask_test);

                DBPDataSourceInfo info = dataSource.getInfo();
                if (info != null) {
                    try {
                        productName = info.getDatabaseProductName();
                        productVersion = info.getDatabaseProductVersion();
                        driverName = info.getDriverName();
                        driverVersion = info.getDriverVersion();
                    } catch (Exception e) {
                        log.error("Can't obtain connection metadata", e);
                    }
                } else {
                    try (DBCSession session = DBUtils.openUtilSession(monitor, dataSource, "Test connection")) {
                        if (session instanceof Connection) {
                            try {
                                Connection connection = (Connection) session;
                                DatabaseMetaData metaData = connection.getMetaData();
                                productName = metaData.getDatabaseProductName();
                                productVersion = metaData.getDatabaseProductVersion();
                                driverName = metaData.getDriverName();
                                driverVersion = metaData.getDriverVersion();
                            } catch (Exception e) {
                                log.error("Can't obtain connection metadata", e);
                            }
                        }
                    }
                }
                if (driverName == null || driverVersion == null) {
                    try {
                        if (driverName == null) {
                            driverName = container.getDriver().getDriverClassName();
                        }
                        if (driverVersion == null) {
                            // Try to get driver version from driver instance
                            Object driverInstance = container.getDriver().getDriverInstance(monitor);
                            if (driverInstance instanceof Driver) {
                                driverVersion = ((Driver) driverInstance).getMajorVersion() + "." + ((Driver) driverInstance).getMinorVersion();
                            }
                        }
                    } catch (DBException e) {
                        log.debug(e);
                    }
                }
                monitor.worked(1);
                monitor.subTask("Load connection info");
                try (DBCSession session = DBUtils.openUtilSession(monitor, dataSource, "Call connection testers")) {
                    for (IWizardPage page : getPages()) {
                        testInPage(session, page);
                    }
                }
                monitor.worked(1);

                new DisconnectJob(container).schedule();
                monitor.worked(1);
                monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_success);
            } catch (DBException ex) {
                connectError = ex;
            }
            monitor.done();
            return Status.OK_STATUS;
        }

        private void testInPage(DBCSession session, IDialogPage page) {
            if (page instanceof IDataSourceConnectionTester) {
                if (page.getControl() != null && !page.getControl().isDisposed()) {
                    ((IDataSourceConnectionTester) page).testConnection(session);
                }
            }
            if (page instanceof ICompositeDialogPage) {
                for (IDialogPage subPage : ArrayUtils.safeArray(((ICompositeDialogPage) page).getSubPages())) {
                    testInPage(session, subPage);
                }
            }
        }
    }

    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        propertyListeners.add(listener);
    }

    public void firePropertyChangeEvent(String property, Object oldValue, Object newValue) {
        for (IPropertyChangeListener listener : propertyListeners) {
            listener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
        }
    }

    public boolean openSettingsPage(String pageId) {
        final IWizardPage page = getPage(pageId);
        if (page != null) {
            getContainer().showPage(page);
        }
        return false;
    }

}