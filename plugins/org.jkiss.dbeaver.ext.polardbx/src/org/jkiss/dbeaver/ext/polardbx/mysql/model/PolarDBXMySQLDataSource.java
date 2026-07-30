/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.ext.polardbx.model.plan.PolarDBXPlanAnalyzer;
import org.osgi.framework.Version;
import org.jkiss.dbeaver.ext.polardbx.mysql.model.PolarDBXCatalog;

import java.sql.Connection;
import java.sql.Statement;

public class PolarDBXMySQLDataSource extends MySQLDataSource {
    private static final Log log = Log.getLog(PolarDBXMySQLDataSource.class);
    private static final String CONN_ATTR_NAME = "connectionAttributes";
    private static final String PROP_APPLICATION_NAME = "program_name";

    // Regular expression for recognizing the PolarDB-X Standard Edition version.
    private static final Pattern POLARDBX_STANDARD_VERSION_PATTERN = Pattern.compile(
        "^\\d+\\.\\d+\\.\\d+-(AliSQL-)?X-Cluster-(\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?)-.*"
    );

    private String serverVersion = "";
    private boolean isPolarDBXStandardEdition = false;
    private String polarDBXStandardVersion = null;

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

    /**
     * Get the version number of the PolarDB-X Standard Edition.
     * @return the Standard Edition version number, or null if it is not the Standard Edition
     */
    public String getPolarDBXStandardVersion() {
        return polarDBXStandardVersion;
    }

    public PolarDBXMySQLDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        super(monitor, container, new PolarDBXDialect());
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "PolarDB-X version fetch")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT VERSION() AS VERSION")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        this.serverVersion = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_VERSION);

                        // Detect whether this is a PolarDB-X Standard Edition.
                        detectPolarDBXStandardEdition();
                    }
                }
            } catch (SQLException ex) {
                // ignore version fetch failure
            }
        }
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        super.createDataSourceInfo(monitor, metaData);
        return new PolarDBXMySQLDataSourceInfo(this, metaData);
    }

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

    @Override
    public <T> T getAdapter(Class<T> adapter) {
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

    /**
     * Detect the PolarDB-X Standard Edition and extract its version information.
     */
    private void detectPolarDBXStandardEdition() {
        if (CommonUtils.isEmpty(serverVersion)) {
            return;
        }

        Matcher matcher = POLARDBX_STANDARD_VERSION_PATTERN.matcher(serverVersion);
        if (matcher.matches()) {
            isPolarDBXStandardEdition = true;
            polarDBXStandardVersion = matcher.group(2); // extract the version number part
        } else {
            isPolarDBXStandardEdition = false;
            polarDBXStandardVersion = null;
        }
    }

    @Override
    public boolean isServerVersionAtLeast(int major, int minor) {
        // For the Standard Edition, use MySQL-compatible version comparison logic.
        if (isPolarDBXStandardEdition && polarDBXStandardVersion != null) {
            try {
                String[] versionParts = polarDBXStandardVersion.split("\\.");
                int majorVer = Integer.parseInt(versionParts[0]);
                int minorVer = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;

                if (majorVer < major) {
                    return false;
                } else if (majorVer == major && minorVer < minor) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                // ignore: fall back to default version comparison below
            }
        }

        // The regular edition uses the original logic.
        Version dbVer = this.getInfo().getDatabaseVersion();
        if (dbVer.getMajor() < major) {
            return false;
        } else if (dbVer.getMajor() == major && dbVer.getMinor() < minor) {
            return false;
        }
        return true;
    }

    @Override
    public MySQLCatalog getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return getCatalog(childName);
    }

    @NotNull
    @Override
    public MySQLCatalog createCatalogInstance(@NotNull MySQLDataSource owner, @NotNull JDBCResultSet resultSet) {
        // Make sure version detection has completed; if not detected yet, detect it first.
        if (CommonUtils.isEmpty(serverVersion)) {
            try {
                // Try to obtain version information from the current session.
                JDBCSession session = resultSet.getSession();
                try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT VERSION() AS VERSION")) {
                    try (JDBCResultSet versionResult = dbStat.executeQuery()) {
                        if (versionResult.next()) {
                            this.serverVersion = JDBCUtils.safeGetString(versionResult, MySQLConstants.COL_VERSION);
                            detectPolarDBXStandardEdition();
                        }
                    }
                }
            } catch (SQLException ex) {
                // If detection fails, default to the PolarDB-X dedicated Catalog; it will be re-detected during initialize.
            }
        }

        // For the Standard Edition, use MySQL's native Catalog directly.
        if (isPolarDBXStandardEdition()) {
            return new MySQLCatalog(owner, resultSet);
        }

        // The regular edition uses the PolarDB-X dedicated Catalog.
        return new PolarDBXCatalog(owner, resultSet);
    }
}