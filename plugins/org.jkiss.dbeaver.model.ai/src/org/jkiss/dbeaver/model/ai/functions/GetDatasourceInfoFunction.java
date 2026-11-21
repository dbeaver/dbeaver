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
package org.jkiss.dbeaver.model.ai.functions;

import com.dbeaver.datadam.mcp.api.model.DDMcpDatasourceInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.Map;

public class GetDatasourceInfoFunction implements AIFunction {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @NotNull
    @Override
    public AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull Map<String, Object> parameters
    ) throws DBException {
        AIDatabaseContext aiDatabaseContext = context.getContext();
        if (aiDatabaseContext == null) {
            throw new DBException("No database connection");
        }

        DBPDataSource dataSource = aiDatabaseContext.getExecutionContext().getDataSource();
        DBPDataSourceInfo info = dataSource.getInfo();
        boolean connected = dataSource.getContainer().isConnected();
        DBCExecutionContextDefaults<?, ?> defaults = connected ? aiDatabaseContext.getExecutionContext().getContextDefaults() : null;
        boolean supportsCatalogs = defaults != null && (defaults.getDefaultCatalog() != null || defaults.supportsCatalogChange());
        boolean supportsSchemas = defaults != null && (defaults.getDefaultSchema() != null || defaults.supportsSchemaChange());
        if (connected && dataSource instanceof DBSObjectContainer container) {
            Class<? extends DBSObject> childType = container.getPrimaryChildType(context.getMonitor());
            supportsCatalogs |= DBSCatalog.class.isAssignableFrom(childType);
            supportsSchemas |= DBSSchema.class.isAssignableFrom(childType);
        }
        var response = new DDMcpDatasourceInfo(
            dataSource.getContainer().getId(),
            dataSource.getContainer().getName(),
            dataSource.getContainer().getConnectionConfiguration().getUrl(),
            info.getDriverName(),
            info.getDriverVersion(),
            connected ? info.getDatabaseProductName() : null,
            connected ? info.getDatabaseProductVersion() : null,
            dataSource.getSQLDialect().getDialectName(),
            supportsCatalogs,
            supportsSchemas,
            connected);
        return new AIFunctionResult(
            AIFunctionType.INFORMATION,
            GSON.toJsonTree(response)
        );
    }
}
