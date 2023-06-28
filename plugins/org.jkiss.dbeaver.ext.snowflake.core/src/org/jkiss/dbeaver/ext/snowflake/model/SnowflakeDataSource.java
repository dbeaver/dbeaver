/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SnowflakeDataSource extends GenericDataSource {

    private static final String APPLICATION_PROPERTY = "application";

    public SnowflakeDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, SnowflakeMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new SnowflakeSQLDialect());
    }
    
    public SnowflakeDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull SnowflakeMetaModel metaModel,
        @NotNull SnowflakeSQLDialect dialect
    ) throws DBException {
        super(monitor, container, metaModel, dialect);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) {
        Map<String, String> props = new HashMap<>();

        // Backward compatibility - use legacy provider property
        // Newer versions use auth model
        String authProp = connectionInfo.getProviderProperty(SnowflakeConstants.PROP_AUTHENTICATOR_LEGACY);
        if (!CommonUtils.isEmpty(authProp)) {
            props.put("authenticator", authProp);
        }

        return props;
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }

    @Override
    protected Properties getAllConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        JDBCExecutionContext context,
        String purpose,
        DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Properties props = super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);
        DBPPreferenceStore prefStore = getContainer().getPreferenceStore();
        if (!prefStore.getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            String appName;
            if (prefStore.getBoolean(ModelPreferences.META_CLIENT_NAME_OVERRIDE)) {
                appName = prefStore.getString(ModelPreferences.META_CLIENT_NAME_VALUE);
            } else {
                appName = getPartnerApplicationIdentifier(GeneralUtils.getProductName());
            }
            props.put(APPLICATION_PROPERTY, appName);
        }
        return props;
    }

    /**
     * Converts product name to the application identifier <br/>
     * <br/>
     * DBeaver <br/>
     *     CE = DBeaver_Community <br/>
     *     EE = DBeaver_Enterprise <br/>
     *     UE = DBeaver_Ultimate <br/>
     *     LE = DBeaver_Lite <br/>
     *     TE = DBeaver_Team <br/>
     * <br/>
     * CloudBeaver<br/>
     *     EE = DBeaver_CloudBeaverEnterprise <br/>
     *     TE = DBeaver_Team <br/>
     *     CB AWS = DBeaver_CloudBeaverAWS <br/>
     * <br/>
     */
    private String getPartnerApplicationIdentifier(String productName) {
        String toolName = "DBeaver_";
        if (productName.contains("Team")) {
            return toolName + "Team";
        }
        if (productName.contains("CloudBeaver")) {
            toolName = "CloudBeaver";
        }
        if (productName.contains("Enterprise")) {
            toolName += "Enterprise";
        } else if (productName.contains("Ultimate")) {
            toolName += "Ultimate";
        } else if (productName.contains("Lite")) {
            toolName += "Lite";
        } else if (productName.contains("Community") || productName.contains("CE") ||
            productName.equals("DBeaver") || productName.equals("CloudBeaver")
        ) {
            toolName += "Community";
        } else if (productName.contains("AWS")) {
            toolName += "AWS";
        } else {
            toolName = productName.replace(" ", "");
        }
        return toolName;
    }

    @NotNull
    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new SnowflakeExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context,
                                          @Nullable JDBCExecutionContext initFrom) throws DBException {
        SnowflakeExecutionContext executionContext = (SnowflakeExecutionContext) context;
        if (initFrom == null) {
            executionContext.refreshDefaults(monitor, true);
            return;
        }
        SnowflakeExecutionContext executionMetaContext = (SnowflakeExecutionContext) initFrom;
        GenericCatalog defaultCatalog = executionMetaContext.getDefaultCatalog();
        GenericSchema defaultSchema = executionMetaContext.getDefaultSchema();
        if (defaultCatalog != null) {
            executionContext.setDefaultCatalog(monitor, defaultCatalog, defaultSchema, true);
        } else if (defaultSchema != null) {
            executionContext.setDefaultSchema(monitor, defaultSchema, true);
        }
    }
}
