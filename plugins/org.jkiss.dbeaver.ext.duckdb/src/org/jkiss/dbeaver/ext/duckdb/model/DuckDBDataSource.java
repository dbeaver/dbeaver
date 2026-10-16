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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * DuckDB Data source
 */
public class DuckDBDataSource extends GenericDataSource {
    private static final Pattern ARRAY_TYPE_NAME_PATTERN = Pattern.compile("\\[\\s*\\d*\\s*\\]\\s*$");

    public DuckDBDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel, new DuckDBSQLDialect());
    }

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        if (DuckDBConstants.isGeometryType(typeName)) {
            return DBPDataKind.OBJECT;
        } else if (ARRAY_TYPE_NAME_PATTERN.matcher(typeName).find()) {
            return DBPDataKind.ARRAY;
        } else {
            return super.resolveDataKind(typeName, valueType);
        }
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new DuckDBDataSourceInfo(getContainer().getDriver(), metaData);
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new DuckDBExecutionContext(instance, type);
    }

    @Nullable
    @Override
    protected Driver createDriverInstance(@NotNull DBRProgressMonitor monitor, @NotNull DBPDriver driver) throws DBCException {
        Driver driverInstance = super.createDriverInstance(monitor, driver);
        if (driverInstance != null && CommonUtils.toBoolean(driver.getDriverParameter(DuckDBConstants.PARAM_MOTHERDUCK_TOKEN_AUTH))) {
            return new MotherDuckTokenDriver(driverInstance);
        }
        return driverInstance;
    }

    /**
     * MotherDuck reads the token from the {@code motherduck_token} property, so it can live in secure storage
     * instead of the URL. The password is copied at connect time, after the auth model (including
     * "password from shell command") has put it into the connection properties.
     */
    private record MotherDuckTokenDriver(@NotNull Driver delegate) implements Driver {
        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            String token = info == null ? null : info.getProperty(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD);
            if (CommonUtils.isEmpty(token) || info.containsKey(DuckDBConstants.PROP_MOTHERDUCK_TOKEN)) {
                return delegate.connect(url, info);
            }
            Properties props = new Properties();
            props.putAll(info);
            props.setProperty(DuckDBConstants.PROP_MOTHERDUCK_TOKEN, token);
            return delegate.connect(url, props);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
