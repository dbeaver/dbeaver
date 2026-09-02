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
package org.jkiss.dbeaver.ext.databricks;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.databricks.model.types.DatabricksDataTypeCache;
import org.jkiss.dbeaver.ext.databricks.model.types.DatabricksMapDataType;
import org.jkiss.dbeaver.ext.databricks.model.types.DatabricksMapValueHandler;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class DatabricksDataSource extends GenericDataSource implements DBDValueHandlerProvider {

    private static final Log log = Log.getLog(DatabricksDataSource.class);

    @NotNull
    private final DatabricksDataTypeCache localDataTypes;

    public DatabricksDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new DatabricksSQLDialect());
        this.localDataTypes = new DatabricksDataTypeCache(this);
    }

    @Nullable
    @Override
    protected String getConnectionURL(@NotNull DBPConnectionConfiguration connectionInfo) throws DBException {
        String url = super.getConnectionURL(connectionInfo);
        if (!isLegacyDriver() && url != null && url.startsWith(DatabricksConstants.JDBC_LEGACY_URL_SUBPROTOCOL)) {
            log.debug("Detected a legacy connection URL in the Databricks native driver. Updating to the native URL.");
            url = url.replaceFirst(DatabricksConstants.JDBC_LEGACY_URL_SUBPROTOCOL, "jdbc:databricks://");
        }
        return url;
    }

    public boolean isLegacyDriver() {
        return CommonUtils.equalObjects(DatabricksConstants.DRIVER_CLASS_LEGACY, getContainer().getDriver().getDriverClassName());
    }

    @Nullable
    @Override
    protected String prepareConnectionURL(@Nullable String url, @NotNull Properties connectionProperties) {
        return isLegacyDriver() ? url : removeDuplicatedUrlParameters(url, connectionProperties);
    }

    @Nullable
    static String removeDuplicatedUrlParameters(@Nullable String url, @NotNull Properties connectionProperties) {
        if (url == null || connectionProperties.isEmpty()) {
            return url;
        }
        int parametersStart = url.indexOf(';');
        if (parametersStart < 0) {
            return url;
        }

        Set<String> propertyNames = new HashSet<>();
        for (Object key : connectionProperties.keySet()) {
            propertyNames.add(key.toString().toLowerCase(Locale.ENGLISH));
        }

        StringBuilder result = new StringBuilder(url.length());
        result.append(url, 0, parametersStart);
        List<String> removed = new ArrayList<>();
        for (String parameter : url.substring(parametersStart + 1).split(";", -1)) {
            int separator = parameter.indexOf('=');
            String name = separator < 0 ? parameter : parameter.substring(0, separator);
            if (propertyNames.contains(name.toLowerCase(Locale.ENGLISH))) {
                removed.add(name);
            } else {
                result.append(';').append(parameter);
            }
        }
        if (removed.isEmpty()) {
            return url;
        }
        log.debug("Skip JDBC URL parameters overridden by connection properties: " + removed);
        return result.toString();
    }

    @NotNull
    @Override
    protected Properties getAllConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        String userAgent = GeneralUtils.getProductName().replace(" ", "+") + "/" + GeneralUtils.getProductVersion();
        connectionInfo.setProperty(DatabricksConstants.USER_AGENT_ENTRY, userAgent);
        return super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);
    }

    @Override
    protected void initializeContextState(
        @NotNull DBRProgressMonitor monitor,
        @NotNull JDBCExecutionContext context,
        @Nullable JDBCExecutionContext initFrom
    ) throws DBException {
        DBCExecutionContextDefaults contextDefaults = context.getContextDefaults();
        if (contextDefaults == null) {
            return;
        }

        if (initFrom == null) {
            contextDefaults.refreshDefaults(monitor, true);
            return;
        }

        DBCExecutionContextDefaults initFromDefaults = initFrom.getContextDefaults();
        if (initFromDefaults != null) {
            GenericCatalog defaultCatalog = (GenericCatalog) initFromDefaults.getDefaultCatalog();
            if (defaultCatalog != null && contextDefaults.supportsCatalogChange()) {
                contextDefaults.setDefaultCatalog(monitor, defaultCatalog, null);
            }
        }
    }

    @Override
    public JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new DatabricksExecutionContext(instance, type);
    }

    @Nullable
    @Override
    public DBDValueHandler getValueHandler(
        @NotNull DBPDataSource dataSource,
        @NotNull DBDFormatSettings preferences,
        @NotNull DBSTypedObject typedObject
    ) {
        if (typedObject.getTypeID() == Types.ARRAY &&
            this.localDataTypes.getCachedObject(typedObject.getFullTypeName()) instanceof  DatabricksMapDataType t) {
            return DatabricksMapValueHandler.INSTANCE;
        } else {
            return null;
        }
    }
}
