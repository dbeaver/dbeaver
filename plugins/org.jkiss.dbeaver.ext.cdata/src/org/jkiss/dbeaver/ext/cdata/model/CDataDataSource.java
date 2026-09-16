/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cdata.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.CDataLicenseUIService;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCConnectException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.sql.Connection;
import java.sql.Driver;

public class CDataDataSource extends GenericDataSource {
    public CDataDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new CDataSQLDialect());
    }

    @NotNull
    @Override
    protected Connection openConnection(
        @NotNull DBRProgressMonitor monitor,
        @Nullable JDBCExecutionContext context,
        @NotNull String purpose
    ) throws DBCException {
        try {
            return super.openConnection(monitor, context, purpose);
        } catch (DBCException e) {
            if (getContainer().getDriver() instanceof CDataDriverDescriptor driver &&
                driver.reportLicenseError(e) && activatePurchasedLicense(driver)) {
                return super.openConnection(monitor, context, purpose);
            }
            throw e;
        }
    }

    @NotNull
    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        if (getContainer().getDriver() instanceof CDataDriverDescriptor driver && driver.reportLicenseError(error)) {
            AbstractJob job = new AbstractJob("Activate CData driver license") {
                @NotNull
                @Override
                protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    return activatePurchasedLicense(driver) ? Status.OK_STATUS : Status.CANCEL_STATUS;
                }
            };
            job.setSystem(true);
            job.schedule();
        }
        return super.discoverErrorType(error);
    }

    private static boolean activatePurchasedLicense(@NotNull CDataDriverDescriptor driver) {
        CDataLicenseUIService service = DBWorkbench.getService(CDataLicenseUIService.class);
        return service != null && service.activateLicense(driver, CDataLicenseType.PURCHASED) != null;
    }

    @NotNull
    @Override
    protected Driver createDriverInstance(@NotNull DBRProgressMonitor monitor, @NotNull DBPDriver driver) throws DBCConnectException {
        try {
            return driver.getDriverLoader(getContainer()).getDriverInstance(monitor);
        } catch (DBException e) {
            throw new DBCConnectException("Can't create CData driver instance", e, this);
        }
    }
}
