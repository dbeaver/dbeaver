/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.ConnectJob;
import org.jkiss.dbeaver.runtime.jobs.DisconnectJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract connection wizard
 */

public abstract class ConnectionWizard extends Wizard implements INewWizard {
    static final Log log = Log.getLog(ConnectionWizard.class);

    // protected final IProject project;
    protected final DataSourceRegistry dataSourceRegistry;
    private final Map<DriverDescriptor, DataSourceDescriptor> infoMap = new HashMap<DriverDescriptor, DataSourceDescriptor>();

    protected ConnectionWizard(DataSourceRegistry dataSourceRegistry)
    {
        setNeedsProgressMonitor(true);
        this.dataSourceRegistry = dataSourceRegistry;
    }

    @Override
    public void dispose() {
        // Dispose all temp data sources
        for (DataSourceDescriptor dataSource : infoMap.values()) {
            dataSource.dispose();
        }
        super.dispose();
    }

    public DataSourceRegistry getDataSourceRegistry() {
        return dataSourceRegistry;
    }

    abstract DriverDescriptor getSelectedDriver();

    public abstract ConnectionPageSettings getPageSettings();

    protected abstract void saveSettings(DataSourceDescriptor dataSource);

    @NotNull
    public DataSourceDescriptor getActiveDataSource()
    {
        DriverDescriptor driver = getSelectedDriver();
        DataSourceDescriptor info = infoMap.get(driver);
        if (info == null) {
            DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
            info = new DataSourceDescriptor(
                getDataSourceRegistry(),
                DataSourceDescriptor.generateNewId(getSelectedDriver()),
                getSelectedDriver(),
                connectionInfo);
            info.getConnectionConfiguration().setClientHomeId(driver.getDefaultClientHomeId());
            infoMap.put(driver, info);
        }
        return info;
    }

    public void testConnection()
    {
        DataSourceDescriptor dataSource = getPageSettings().getActiveDataSource();
        DataSourceDescriptor testDataSource = new DataSourceDescriptor(
            dataSourceRegistry,
            dataSource.getId(),
            getSelectedDriver(),
            dataSource.getConnectionConfiguration());
        try {
            saveSettings(testDataSource);

            final ConnectionTester op = new ConnectionTester(testDataSource);

            try {
                getContainer().run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        // Wait for job to finish
                        op.ownerMonitor = new DefaultProgressMonitor(monitor);
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
                UIUtils.showErrorDialog(getShell(), CoreMessages.dialog_connection_wizard_start_dialog_interrupted_title,
                    CoreMessages.dialog_connection_wizard_start_dialog_interrupted_message);
            } catch (InvocationTargetException ex) {
                UIUtils.showErrorDialog(
                        getShell(),
                        CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                        null,
                        GeneralUtils.makeExceptionStatus(ex.getTargetException()));
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

        public ConnectionTester(DataSourceDescriptor testDataSource)
        {
            super(testDataSource);
            setSystem(true);
            super.initialize = CommonUtils.toBoolean(testDataSource.getDriver().getDriverParameter(DBConstants.PARAM_INIT_ON_TEST));
            productName = null;
            productVersion = null;
        }

        @Override
        public IStatus run(DBRProgressMonitor monitor)
        {
            if (ownerMonitor != null) {
                monitor = ownerMonitor;
            }
            monitor.beginTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_start, 3);
            Thread.currentThread().setName(CoreMessages.dialog_connection_wizard_start_connection_monitor_thread);

            try {
                container.setName(container.getConnectionConfiguration().getUrl());
                monitor.worked(1);
                long startTime = System.currentTimeMillis();
                super.run(monitor);
                connectTime = (System.currentTimeMillis() - startTime);
                if (connectError != null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                monitor.worked(1);
                DBPDataSource dataSource = container.getDataSource();
                if (dataSource == null) {
                    throw new DBException(CoreMessages.editors_sql_status_not_connected_to_database);
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
                    DBCSession session = dataSource.getDefaultContext(false).openSession(monitor, DBCExecutionPurpose.UTIL, "Test connection");
                    try {
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
                    } finally {
                        session.close();
                    }
                }
                new DisconnectJob(container).schedule();
                monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_success);
            } catch (DBException ex) {
                connectError = ex;
            }
            monitor.done();
            return Status.OK_STATUS;
        }
    }
}