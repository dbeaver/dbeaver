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

package org.jkiss.dbeaver.ext.oceanbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.GenericMetaModelRegistry;
import org.jkiss.dbeaver.ext.oracle.model.OracleSQLDialect;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.osgi.framework.Version;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * OceanBase datasource for Oracle-mode tenants.
 * Uses the standard generic model (schema-based tree) with the Oracle SQL dialect.
 */
public class OceanbaseOracleDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(OceanbaseOracleDataSource.class);

    public OceanbaseOracleDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException {
        super(monitor, container, GenericMetaModelRegistry.getInstance().getMetaModel(container), new OracleSQLDialect());
    }

    @Override
    protected synchronized void readDatabaseServerVersion(Connection session, DatabaseMetaData metaData) {
        // OceanBase reports a MySQL wire protocol compatibility version via JDBC metadata.
        // Query the real server version instead - ob_version() is supported in both tenant modes.
        // FROM DUAL is required by Oracle mode syntax and is also valid in MySQL mode.
        if (databaseVersion == null) {
            try {
                String version = JDBCUtils.executeQuery(session, "SELECT ob_version() FROM DUAL");
                if (version != null) {
                    databaseVersion = new Version(version);
                }
            } catch (Throwable e) {
                log.warn("Error determining OceanBase server version", e);
            }
            if (databaseVersion == null) {
                super.readDatabaseServerVersion(session, metaData);
            }
        }
    }
}
