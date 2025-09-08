/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class DatabricksDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(DatabricksDataSource.class);

    public DatabricksDataSource(DBRProgressMonitor monitor,
                                DBPDataSourceContainer container,
                                GenericMetaModel metaModel) throws
                                                            DBException {
        super(monitor, container, metaModel, new DatabricksSQLDialect());
    }


    @Override
    protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
        String url = super.getConnectionURL(connectionInfo);
        if (!isLegacyDriver() && url.startsWith(DatabricksConstants.JDBC_LEGACY_URL_SUBPROTOCOL)) {
            log.debug("Detected a legacy connection URL in the Databricks native driver. Updating to the native URL.");
            url = url.replaceFirst(DatabricksConstants.JDBC_LEGACY_URL_SUBPROTOCOL, "jdbc:databricks://");
        }
        return url;
    }

    private boolean isLegacyDriver() {
        return CommonUtils.equalObjects(DatabricksConstants.DRIVER_CLASS_LEGACY, getContainer().getDriver().getDriverClassName());
    }

    @NotNull
    @Override
    protected Properties getAllConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        JDBCExecutionContext context,
        String purpose,
        DBPConnectionConfiguration connectionInfo
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
}
