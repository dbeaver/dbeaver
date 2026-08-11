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

package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.gbase8s.GBase8sUtils;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Chao Tian
 */
public class GBase8sDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(GBase8sDataType.class);

    private static final Set<Integer> FEATURE_NOT_SUPPORTED_CODES = Set.of(
        -79700,	// this is documented as "Feature not supported", see https://www.gbase.cn/docs/gbase-8s/06%20%E7%BC%96%E7%A8%8B%E6%8E%A5%E5%8F%A3/01%20JDBCDriver%E7%A8%8B%E5%BA%8F%E5%91%98%E6%8C%87%E5%8D%97/09%20%E9%99%84%E5%BD%95#-79700
        -79882	// this occurs when calling PreparedStatement.setNCharacterStream(), not documented
    );

    /**
     * driver properties that needs to be set dynamically.
     * if you don't need dynamic, just define this property in plugin.xml :)
     */
    private static final Map<String, Function<String, String>> DYNAMIC_DRIVER_PROPERTIES = Map.ofEntries(
        // according to documentation, when loading big CLOB/TEXT value, the driver need save it in temp file.
        // but sometimes it will fail (see #41669), give driver property `JDBCTEMP` will fix it.
        Map.entry("JDBCTEMP", GBase8sDataSource::configureDefaultJdbcTemp)
    );

    private static String configureDefaultJdbcTemp(String currentTemp) {
        if (!CommonUtils.isEmpty(currentTemp)) {
            return currentTemp;
        }

        try {
            String defaultTemp = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "gbase8s-jdbctemp").toAbsolutePath().toString(); //$NON-NLS-1$
            log.debug("Fallback to default JDBCTEMP: " + defaultTemp);
            return defaultTemp;
        } catch (IOException e) {
            log.warn("Failed to configure default JDBC temp", e);
            // Is it necessary to prompt to user?
            return currentTemp;
        }
    }

    public GBase8sDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        // replace the connector of the database/table in queryGetActiveDB
        replaceConnector4GetActiveDB(container);
        return new GenericExecutionContext(instance, type);
    }

    void replaceConnector4GetActiveDB(DBPDataSourceContainer container) {
        final DBPDriver driver = container.getDriver();
        String getActiveDBQuery = CommonUtils
                .toString(driver.getDriverParameter(GenericConstants.PARAM_QUERY_GET_ACTIVE_DB));
        if (GBase8sUtils.isOracleSqlMode(container)) {
            getActiveDBQuery = getActiveDBQuery.replaceFirst("\\?", ".");
        } else {
            getActiveDBQuery = getActiveDBQuery.replaceFirst("\\?", ":");
        }
        Field field;
        try {
            field = this.getClass().getSuperclass().getDeclaredField("queryGetActiveDB");
            field.setAccessible(true);
            field.set(this, getActiveDBQuery);
        } catch (Exception e) {
            log.error("Failed to replace the connector of the database/table in queryGetActiveDB", e);
        }
    }

    @Override
    public ErrorType discoverErrorType(Throwable error) {
        // #41319 when calling PreparedStatement.setNCharacterStream()
        // the driver should be thrown SQLFeatureNotSupportedException, but it doesn't
        // so detect it by analyzing errorCode and handle it properly
        if (error instanceof SQLException sqlEx) {
            int errorCode = sqlEx.getErrorCode();
            if (FEATURE_NOT_SUPPORTED_CODES.contains(errorCode)) {
                return ErrorType.FEATURE_UNSUPPORTED;
            }
        }

        return super.discoverErrorType(error);  // fallback case
    }

    /**
     * override this method to set some driver properties dynamically to fix #41669
     *
     * @param connectionInfo {@inheritDoc}
     * @param connectProps {@inheritDoc}
     */
    @Override
    protected void fillConnectionProperties(DBPConnectionConfiguration connectionInfo, Properties connectProps) {
        super.fillConnectionProperties(connectionInfo, connectProps);

        for (var propEntry : DYNAMIC_DRIVER_PROPERTIES.entrySet()) {
            String propertyName = propEntry.getKey();
            log.debug("Detected driver property that may be need to set dynamically: " + propertyName);
            connectProps.setProperty(propertyName, propEntry.getValue().apply(connectProps.getProperty(propertyName)));
        }
    }
}
