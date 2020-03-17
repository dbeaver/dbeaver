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
package org.jkiss.dbeaver.runtime.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;

public class ConnectionTestJob extends ConnectJob {
    private static final Log log = Log.getLog(ConnectionTestJob.class);

    private DBRRunnableParametrized<DBCSession> onTest;
    private String productName;
    private String productVersion;
    private String driverName;
    private String driverVersion;
    private String serverVersion = "?";
    private String clientVersion = "?";
    private long connectTime = -1;
    private DBRProgressMonitor ownerMonitor;

    public ConnectionTestJob(DBPDataSourceContainer testDataSource, DBRRunnableParametrized<DBCSession> onTest) {
        super(testDataSource);
        this.setSystem(true);
        this.setUser(false);
        super.initialize = true;//CommonUtils.toBoolean(testDataSource.getDriver().getDriverParameter(DBConstants.PARAM_INIT_ON_TEST));
        this.onTest = onTest;
        this.productName = null;
        this.productVersion = null;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public long getConnectTime() {
        return connectTime;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setOwnerMonitor(DBRProgressMonitor ownerMonitor) {
        this.ownerMonitor = ownerMonitor;
    }

    @Override
    public IStatus run(DBRProgressMonitor monitor) {
        if (ownerMonitor != null) {
            monitor = ownerMonitor;
        }
        Thread.currentThread().setName(ModelMessages.dialog_connection_wizard_start_connection_monitor_thread);

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
            monitor.beginTask(ModelMessages.dialog_connection_wizard_start_connection_monitor_start, 3);

            DBPDataSource dataSource = container.getDataSource();
            if (dataSource == null) {
                throw new DBException(ModelMessages.error_not_connected_to_database);
            }
            monitor.subTask(ModelMessages.dialog_connection_wizard_start_connection_monitor_subtask_test);

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
            try {
                try (DBCSession session = DBUtils.openUtilSession(monitor, dataSource, "Call connection testers")) {
                    onTest.run(session);
                }
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // ignore
            }
            monitor.worked(1);

            new DisconnectJob(container).schedule();
            monitor.worked(1);
            monitor.subTask(ModelMessages.dialog_connection_wizard_start_connection_monitor_success);

            if (!CommonUtils.isEmpty(this.productName)) {
                serverVersion = this.productName + " " + this.productVersion;
            }
            if (!CommonUtils.isEmpty(this.driverName)) {
                clientVersion = this.driverName + " " + this.driverVersion;
            }

        } catch (DBException ex) {
            connectError = ex;
        }
        monitor.done();
        return Status.OK_STATUS;
    }
}
