/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai.functions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

class GetDatasourceInfoFunctionTest {
    private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
        Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
    private final DBPDataSourceContainer dataSourceContainer = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPDataSourceInfo info = Mockito.mock(DBPDataSourceInfo.class);
    private final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
    private final AIFunctionContext context = Mockito.mock(AIFunctionContext.class);
    private final GetDatasourceInfoFunction function = new GetDatasourceInfoFunction();

    @BeforeEach
    void setUp() throws DBException {
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSourceContainer.getId()).thenReturn("sample-id");
        Mockito.when(dataSourceContainer.getName()).thenReturn("Sample connection");
        Mockito.when(dataSourceContainer.isConnected()).thenReturn(true);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:sample://localhost/example");
        Mockito.when(dataSourceContainer.getConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(dataSource.getInfo()).thenReturn(info);
        Mockito.when(info.getDriverName()).thenReturn("Sample driver");
        Mockito.when(info.getDriverVersion()).thenReturn("1.2");
        Mockito.when(info.getDatabaseProductName()).thenReturn("Sample server");
        Mockito.when(info.getDatabaseProductVersion()).thenReturn("3.4");
        var dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.getDialectName()).thenReturn("Sample SQL");
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        AIDatabaseContext databaseContext = Mockito.mock(AIDatabaseContext.class);
        Mockito.when(databaseContext.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(context.getContext()).thenReturn(databaseContext);
        Mockito.doReturn(DBSEntity.class).when((DBSObjectContainer) dataSource).getPrimaryChildType(Mockito.any());
    }

    @Test
    void returnsStructuredConnectionAndServerMetadata() throws Exception {
        var result = result();
        Assertions.assertEquals(JsonParser.parseString("""
            {"dataSourceId":"sample-id","dataSourceName":"Sample connection","connectionURL":"jdbc:sample://localhost/example",
             "driverName":"Sample driver","driverVersion":"1.2",
             "databaseServerName":"Sample server","databaseServerVersion":"3.4",
             "sqlDialectName":"Sample SQL","supportsCatalogs":false,"supportsSchemas":false,"isConnected":true}
            """), result);
        Assertions.assertEquals("dataSourceId", result.keySet().iterator().next());
    }

    @Test
    void omitsCredentialBearingConnectionURLs() throws Exception {
        var configuration = dataSourceContainer.getConnectionConfiguration();
        for (String url : new String[] {
            "jdbc:sample://user:private-value@localhost/example",
            "jdbc:sample://localhost/example?password=private-value",
            "jdbc:sqlserver://localhost;password={private;value}",
            "jdbc:oracle:thin:user/private-value@localhost:1521/example",
            "jdbc:sample://localhost/example?accessToken=private-value",
            "jdbc:sample://localhost/example?pass%77ord=private-value",
            "jdbc:sample://localhost/example?password%3Dprivate-value",
            "jdbc:sample://localhost/example?api_key=private-value"
        }) {
            configuration.setUrl(url);
            Assertions.assertTrue(result().get("connectionURL").isJsonNull());
        }
    }

    @Test
    void disconnectedDatasourceDoesNotExposeCachedServerInfoOrReadStructure() throws Exception {
        Mockito.when(dataSourceContainer.isConnected()).thenReturn(false);

        var result = result();

        Assertions.assertFalse(result.get("isConnected").getAsBoolean());
        Assertions.assertTrue(result.get("databaseServerName").isJsonNull());
        Assertions.assertTrue(result.get("databaseServerVersion").isJsonNull());
        Assertions.assertEquals("Sample driver", result.get("driverName").getAsString());
        Assertions.assertEquals("jdbc:sample://localhost/example", result.get("connectionURL").getAsString());
        Mockito.verify(info, Mockito.never()).getDatabaseProductName();
        Mockito.verify(info, Mockito.never()).getDatabaseProductVersion();
        Mockito.verify(executionContext, Mockito.never()).getContextDefaults();
        Mockito.verify((DBSObjectContainer) dataSource, Mockito.never()).getPrimaryChildType(Mockito.any());
    }

    @Test
    void reportsStructureRatherThanGenericCatalogAndSchemaTerms() throws Exception {
        Mockito.when(info.getCatalogTerm()).thenReturn("Catalog");
        Mockito.when(info.getSchemaTerm()).thenReturn("Schema");
        DBSObjectContainer container = (DBSObjectContainer) dataSource;

        Mockito.doReturn(DBSEntity.class).when(container).getPrimaryChildType(Mockito.any());
        var tableOnly = result();
        Assertions.assertFalse(tableOnly.get("supportsCatalogs").getAsBoolean());
        Assertions.assertFalse(tableOnly.get("supportsSchemas").getAsBoolean());

        Mockito.doReturn(DBSSchema.class).when(container).getPrimaryChildType(Mockito.any());
        Assertions.assertTrue(result().get("supportsSchemas").getAsBoolean());

        Mockito.doReturn(DBSCatalog.class).when(container).getPrimaryChildType(Mockito.any());
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
        Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        var catalogsAndSchemas = result();
        Assertions.assertTrue(catalogsAndSchemas.get("supportsCatalogs").getAsBoolean());
        Assertions.assertTrue(catalogsAndSchemas.get("supportsSchemas").getAsBoolean());
    }

    @NotNull
    private JsonObject result() throws DBException {
        return Assertions.assertInstanceOf(JsonObject.class, function.callFunction(context, Map.of()).getValue());
    }
}
