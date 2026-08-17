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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.polardbx.model.plan.PolarDBXPlanAnalyzer;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolarDBXMySQLDataSource extends MySQLDataSource {
    private static final Log log = Log.getLog(PolarDBXMySQLDataSource.class);
    private static final String CONN_ATTR_NAME = "connectionAttributes";
    private static final String PROP_APPLICATION_NAME = "program_name";

    // Regular expression for recognizing the PolarDB-X Standard Edition version.
    private static final Pattern POLARDBX_STANDARD_VERSION_PATTERN = Pattern.compile(
        "^\\d+\\.\\d+\\.\\d+-(AliSQL-)?X-Cluster-(\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?)-.*"
    );
    private static final Pattern POLARDBX_ENTERPRISE_VERSION_PATTERN = Pattern.compile(
        "^\\d+\\.\\d+\\.\\d+-(?i:TDDL)-(\\d+\\.\\d+\\.\\d+)(?:-.*)?$"
    );

    private String serverVersion = "";
    private boolean isPolarDBXStandardEdition = false;

    @NotNull
    public String getServerVersion() {
        return this.serverVersion;
    }

    /**
     * Check whether this is a PolarDB-X Standard Edition.
     * @return true if it is the Standard Edition, false if it is the regular edition
     */
    public boolean isPolarDBXStandardEdition() {
        return isPolarDBXStandardEdition;
    }

    public PolarDBXMySQLDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        super(monitor, container, new PolarDBXDialect());
    }

    @Override
    protected synchronized void readDatabaseServerVersion(
        @NotNull Connection session,
        @NotNull DatabaseMetaData metaData
    ) {
        if (databaseVersion == null) {
            try {
                String version = JDBCUtils.executeQuery(session, "SELECT VERSION()");
                if (CommonUtils.isNotEmpty(version)) {
                    serverVersion = version;
                    isPolarDBXStandardEdition = POLARDBX_STANDARD_VERSION_PATTERN.matcher(version).matches();
                    databaseVersion = parseDatabaseVersion(version);
                }
            } catch (SQLException | IllegalArgumentException e) {
                log.debug("Error determining PolarDB-X server version", e);
            }
            if (databaseVersion == null) {
                super.readDatabaseServerVersion(session, metaData);
            }
        }
    }

    @Nullable
    private static Version parseDatabaseVersion(@NotNull String version) {
        Matcher standardMatcher = POLARDBX_STANDARD_VERSION_PATTERN.matcher(version);
        if (standardMatcher.matches()) {
            String[] versionParts = standardMatcher.group(2).split("\\.");
            return new Version(
                Integer.parseInt(versionParts[0]),
                Integer.parseInt(versionParts[1]),
                Integer.parseInt(versionParts[2])
            );
        }

        Matcher enterpriseMatcher = POLARDBX_ENTERPRISE_VERSION_PATTERN.matcher(version);
        if (enterpriseMatcher.matches()) {
            return new Version(enterpriseMatcher.group(1));
        }
        return null;
    }

    @NotNull
    @Override
    protected DBPDataSourceInfo createDataSourceInfo(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCDatabaseMetaData metaData
    ) {
        super.createDataSourceInfo(monitor, metaData);
        return new PolarDBXMySQLDataSourceInfo(this, metaData);
    }

    @NotNull
    @Override
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> props = super.getInternalConnectionProperties(monitor, driver, context, purpose, connectionInfo);
        String appName = DBUtils.getClientApplicationName(getContainer(), context, purpose);
        appName = "dbeaver_polardbx_plugin" + (CommonUtils.isEmpty(appName) ? "" : "(" + appName + ")");

        String connAttr = props.get(CONN_ATTR_NAME);
        connAttr = PROP_APPLICATION_NAME + ":" + appName + (CommonUtils.isEmpty(connAttr) ? "" : "," + connAttr);

        props.put(CONN_ATTR_NAME, connAttr);

        return props;
    }

    @NotNull
    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor,
                                       @Nullable JDBCExecutionContext context,
                                       @NotNull String purpose) throws DBCException {
        Connection connection = super.openConnection(monitor, context, purpose);

        // Only set COMPATIBLE_CHARSET_VARIABLES on PolarDB-X Enterprise Edition (regular edition).
        // The Standard Edition does not support this variable and will skip it.
        if (!isPolarDBXStandardEdition()) {
            try {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET GLOBAL COMPATIBLE_CHARSET_VARIABLES = true");
                }
            } catch (SQLException e) {
                // ignore: COMPATIBLE_CHARSET_VARIABLES not supported on this edition
            }
        }

        return connection;
    }

    @Nullable
    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        // For the Standard Edition, use MySQL's adapter logic entirely.
        if (isPolarDBXStandardEdition()) {
            return super.getAdapter(adapter);
        }

        // The regular edition uses PolarDB-X dedicated adapters.
        if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new PolarDBXPlanAnalyzer(this));
        } else if (adapter == DBDValueHandlerProvider.class) {
            return adapter.cast(new JDBCStandardValueHandlerProvider());
        } else {
            return super.getAdapter(adapter);
        }
    }

    @NotNull
    @Override
    public Class<? extends MySQLCatalog> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return MySQLCatalog.class;
    }

    @Override
    public boolean supportsInformationSchema() {
        return true;
    }

    @Override
    public boolean supportsSequences() {
        return this.isServerVersionAtLeast(4, 0);
    }

    @Nullable
    @Override
    public MySQLCatalog getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return getCatalog(childName);
    }

    @NotNull
    @Override
    public MySQLCatalog createCatalogInstance(@NotNull MySQLDataSource owner, @NotNull JDBCResultSet resultSet) {
        // For the Standard Edition, use MySQL's native Catalog directly.
        if (isPolarDBXStandardEdition()) {
            return new MySQLCatalog(owner, resultSet);
        }

        // The regular edition uses the PolarDB-X dedicated Catalog.
        return new PolarDBXCatalog(owner, resultSet);
    }
}
