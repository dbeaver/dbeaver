/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Abstract connection wizard
 */

public abstract class ConnectionWizard extends Wizard implements INewWizard {
    static final Log log = Log.getLog(ConnectionWizard.class);

    // protected final IProject project;
    protected final DataSourceRegistry dataSourceRegistry;

    protected ConnectionWizard(DataSourceRegistry dataSourceRegistry)
    {
        setNeedsProgressMonitor(true);
        this.dataSourceRegistry = dataSourceRegistry;
    }

    public DataSourceRegistry getDataSourceRegistry() {
        return dataSourceRegistry;
    }

    abstract DriverDescriptor getSelectedDriver();

    public abstract ConnectionPageSettings getPageSettings();

    protected abstract void saveSettings(DataSourceDescriptor dataSource);

    public void testConnection()
    {
        DataSourceDescriptor dataSource = getPageSettings().getActiveDataSource();
        DataSourceDescriptor testDataSource = new DataSourceDescriptor(
            dataSourceRegistry,
            dataSource.getId(),
            getSelectedDriver(),
            dataSource.getConnectionInfo());
        try {
            saveSettings(testDataSource);

            ConnectionTester op = new ConnectionTester(testDataSource);

            try {
                long startTime = System.currentTimeMillis();
                RuntimeUtils.run(getContainer(), true, true, op);
                long connectTime = (System.currentTimeMillis() - startTime);
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
                message += NLS.bind(CoreMessages.dialog_connection_wizard_start_connection_monitor_connected, connectTime);

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
                        RuntimeUtils.makeExceptionStatus(ex.getTargetException()));
            }
        } finally {
            testDataSource.dispose();
        }
    }

    public boolean isNew()
    {
        return false;
    }

    private class ConnectionTester implements DBRRunnableWithProgress {
        String productName;
        String productVersion;
        String driverName;
        String driverVersion;
        private final DataSourceDescriptor testDataSource;
        private boolean initOnTest;

        public ConnectionTester(DataSourceDescriptor testDataSource)
        {
            this.testDataSource = testDataSource;
            this.initOnTest = CommonUtils.toBoolean(testDataSource.getDriver().getDriverParameter(DBConstants.PARAM_INIT_ON_TEST));
            productName = null;
            productVersion = null;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            monitor.beginTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_start, 3);
            Thread.currentThread().setName(CoreMessages.dialog_connection_wizard_start_connection_monitor_thread);

            try {
                testDataSource.setName(testDataSource.getConnectionInfo().getUrl());
                monitor.worked(1);
                testDataSource.connect(monitor, initOnTest, false);
                monitor.worked(1);
                DBPDataSource dataSource = testDataSource.getDataSource();
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
                    DBCSession session = dataSource.openSession(monitor, DBCExecutionPurpose.UTIL, "Test connection");
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
                        try {
                            monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_close);
                            testDataSource.disconnect(monitor, false);
                        } catch (DBException e) {
                            // ignore it
                            log.error(e);
                        } finally {
                            monitor.done();
                        }
                    } finally {
                        session.close();
                    }
                }
                monitor.subTask(CoreMessages.dialog_connection_wizard_start_connection_monitor_success);
            } catch (DBException ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }
}