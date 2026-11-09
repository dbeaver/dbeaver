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
package org.jkiss.dbeaver.model.ai.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseCapabilities;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.ai.functions.ListSchemaNamesFunction;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionInternalRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AIConnectionCapabilitiesTest extends DBeaverUnitTest {
    private final DBPProject project = Mockito.mock(DBPProject.class);
    private final Fixture sqlite = fixture("sqlite", false);
    private final Fixture postgres = fixture("postgres", true);

    @Test
    public void reportsNamespacesForEachConnectionInBothPrimaryOrders() throws DBException {
        for (Fixture primary : List.of(sqlite, postgres)) {
            AIFunctionContext context = context(primary, primary == sqlite ? postgres : sqlite);
            String prompt = AIEngineRequestFactory.describeConnections(context);
            JsonObject sqliteInfo = connection(prompt, "sqlite");
            JsonObject postgresInfo = connection(prompt, "postgres");
            Assertions.assertFalse(sqliteInfo.get("supportsSchemas").getAsBoolean());
            Assertions.assertFalse(sqliteInfo.get("supportsCatalogs").getAsBoolean());
            Assertions.assertTrue(postgresInfo.get("supportsSchemas").getAsBoolean());
            Assertions.assertTrue(postgresInfo.get("supportsCatalogs").getAsBoolean());
            Assertions.assertEquals("sqlite", sqliteInfo.get("dialect").getAsString());
            Assertions.assertEquals("postgres", postgresInfo.get("dialect").getAsString());
            Assertions.assertEquals("database", postgresInfo.get("defaultCatalog").getAsString());
            Assertions.assertEquals("public", postgresInfo.get("defaultSchema").getAsString());
            Assertions.assertFalse(sqliteInfo.has("defaultCatalog"));
            Assertions.assertFalse(sqliteInfo.has("defaultSchema"));
            Assertions.assertEquals(primary == sqlite, sqliteInfo.get("primary").getAsBoolean());
            Assertions.assertEquals(primary == postgres, postgresInfo.get("primary").getAsBoolean());
        }
    }

    @Test
    public void schemaToolRemainsAvailableWhenOnlyAdditionalDatabaseSupportsIt() throws DBException {
        AIFunctionContext context = context(sqlite, postgres);
        AIFunctionDescriptor function = schemaFunction();
        Assertions.assertEquals(AIFunctionVerifier.FunctionState.NOT_APPLICABLE, function.getFunctionState(context));
        Assertions.assertEquals(AIFunctionVerifier.FunctionState.APPLICABLE,
            AIEngineRequestFactory.getRequestFunctionState(function, context));
        Assertions.assertEquals(AIFunctionVerifier.FunctionState.NOT_APPLICABLE,
            function.getFunctionState(context.withConnection("sqlite")));
        Assertions.assertEquals(AIFunctionVerifier.FunctionState.APPLICABLE,
            function.getFunctionState(context.withConnection("postgres")));
    }

    @Test
    public void schemaToolIsOmittedWhenNoAttachedDatabaseSupportsSchemas() throws DBException {
        AIFunctionContext context = context(sqlite, fixture("second-sqlite", false));
        Assertions.assertEquals(AIFunctionVerifier.FunctionState.NOT_APPLICABLE,
            AIEngineRequestFactory.getRequestFunctionState(schemaFunction(), context));
    }

    @Test
    public void endpointToolsRemainAvailableWhenOnlyAdditionalDatabaseSupportsThem() throws DBException {
        AIFunctionContext context = context(sqlite, postgres);
        for (String endpoint : List.of(AIFunctionParams.SOURCE_CONNECTION_ID_PARAM, AIFunctionParams.TARGET_CONNECTION_ID_PARAM)) {
            AIFunctionDescriptor function = Mockito.mock(AIFunctionDescriptor.class);
            Mockito.when(function.getParameter(endpoint)).thenReturn(Mockito.mock(AIFunctionParameter.class));
            Mockito.when(function.getFunctionState(Mockito.any())).thenAnswer(invocation ->
                ((AIFunctionContext) invocation.getArgument(0)).getContext() == postgres.database()
                    ? AIFunctionVerifier.FunctionState.APPLICABLE : AIFunctionVerifier.FunctionState.NOT_APPLICABLE);
            Assertions.assertEquals(AIFunctionVerifier.FunctionState.APPLICABLE,
                AIEngineRequestFactory.getRequestFunctionState(function, context));
            Mockito.when(postgres.container().isConnected()).thenReturn(false);
            Assertions.assertEquals(AIFunctionVerifier.FunctionState.NOT_APPLICABLE,
                AIEngineRequestFactory.getRequestFunctionState(function, context));
            Mockito.when(postgres.container().isConnected()).thenReturn(true);
        }
    }

    @Test
    public void rejectsSchemaToolForSqliteBeforeReadingItsChildren() throws DBException {
        AIFunctionContext context = context(postgres, sqlite);
        AIFunctionInternalRegistry registry = Mockito.mock(AIFunctionInternalRegistry.class, Mockito.CALLS_REAL_METHODS);
        DBException error = Assertions.assertThrows(DBException.class,
            () -> registry.callFunction(context, schemaFunction(), Map.of(AIFunctionParams.CONNECTION_ID_PARAM, "sqlite")));
        Assertions.assertTrue(error.getMessage().contains("sqlite"));
        Assertions.assertTrue(error.getMessage().contains("not supported"));
        Mockito.verify((DBSObjectContainer) sqlite.dataSource(), Mockito.never()).getChildren(Mockito.any());
    }

    @Test
    public void disconnectedConnectionRemainsUnknownWithoutOpeningIt() throws DBException {
        Mockito.when(postgres.container().isConnected()).thenReturn(false);
        JsonObject info = connection(AIEngineRequestFactory.describeConnections(context(sqlite, postgres)), "postgres");
        Assertions.assertEquals("unknown", info.get("supportsSchemas").getAsString());
        Assertions.assertFalse(info.get("metadataAvailable").getAsBoolean());
        Mockito.verifyNoInteractions(postgres.execution());
    }

    @Test
    public void deniedContextDoesNotExposeItsDialectOrDefaults() throws DBException {
        AIFunctionContext original = context(sqlite, postgres);
        AIFunctionContext denied = new AIFunctionContext(new VoidProgressMonitor(), sqlite.database(), original.getPrompt(),
            original.getConversation(), id -> {
                if ("postgres".equals(id)) {
                    throw new DBException("Access denied");
                }
                return sqlite.database();
            });
        JsonObject info = connection(AIEngineRequestFactory.describeConnections(denied), "postgres");
        Assertions.assertFalse(info.get("metadataAvailable").getAsBoolean());
        Assertions.assertFalse(info.has("dialect"));
        Assertions.assertFalse(info.has("defaultCatalog"));
        Assertions.assertFalse(info.has("defaultSchema"));
        Assertions.assertEquals(AIFunctionVerifier.FunctionState.NOT_APPLICABLE,
            AIEngineRequestFactory.getRequestFunctionState(schemaFunction(), denied));
    }

    @Test
    public void doesNotInventSchemaSupportWhenNoCatalogIsSelected() throws DBException {
        Mockito.when(postgres.execution().getContextDefaults()).thenReturn(null);
        AIDatabaseCapabilities capabilities = AIDatabaseCapabilities.read(context(postgres, sqlite));
        Assertions.assertEquals(Boolean.TRUE, capabilities.catalogs());
        Assertions.assertNull(capabilities.schemas());
    }

    @Test
    public void schemaOnlyDatabaseDoesNotAdvertiseCatalogs() throws DBException {
        Mockito.doReturn(DBSSchema.class).when((DBSObjectContainer) sqlite.dataSource()).getPrimaryChildType(Mockito.any());
        AIDatabaseCapabilities capabilities = AIDatabaseCapabilities.read(context(sqlite, postgres));
        Assertions.assertEquals(Boolean.FALSE, capabilities.catalogs());
        Assertions.assertEquals(Boolean.TRUE, capabilities.schemas());
    }

    @Test
    public void catalogOnlyDatabaseDoesNotAdvertiseSchemas() throws DBException {
        var defaults = postgres.execution().getContextDefaults();
        DBSCatalog catalog = defaults.getDefaultCatalog();
        Mockito.doReturn(DBSTable.class).when(catalog).getPrimaryChildType(Mockito.any());
        AIDatabaseCapabilities capabilities = AIDatabaseCapabilities.read(context(postgres, sqlite));
        Assertions.assertEquals(Boolean.TRUE, capabilities.catalogs());
        Assertions.assertEquals(Boolean.FALSE, capabilities.schemas());
    }

    @Test
    public void defaultsOutsideTheAllowedScopeAreOmitted() throws DBException {
        Mockito.when(postgres.database().isObjectAllowed(Mockito.any())).thenReturn(false);
        JsonObject info = connection(AIEngineRequestFactory.describeConnections(context(sqlite, postgres)), "postgres");
        Assertions.assertFalse(info.has("defaultCatalog"));
        Assertions.assertFalse(info.has("defaultSchema"));
    }

    @NotNull
    private AIFunctionContext context(@NotNull Fixture primary, @NotNull Fixture additional) throws DBException {
        Mockito.when(project.getId()).thenReturn("project");
        AIPromptGenerator prompt = Mockito.mock(AIPromptGenerator.class);
        AIChatConversation conversation = new AIChatConversation(UUID.randomUUID(), "Chat", prompt,
            List.of(), primary.container(), 0, null);
        conversation.addDataSource(additional.container());
        return new AIFunctionContext(new VoidProgressMonitor(), primary.database(), prompt, conversation,
            id -> id == null || id.equals(primary.container().getId()) ? primary.database() : additional.database());
    }

    @NotNull
    private static AIFunctionDescriptor schemaFunction() {
        AIFunctionDescriptor descriptor = Mockito.mock(AIFunctionDescriptor.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(descriptor.getId()).thenReturn("listSchemaNames");
        Mockito.when(descriptor.getParameter(AIFunctionParams.CONNECTION_ID_PARAM)).thenReturn(Mockito.mock(AIFunctionParameter.class));
        Mockito.when(descriptor.getInstance()).thenReturn(new ListSchemaNamesFunction());
        return descriptor;
    }

    @NotNull
    private Fixture fixture(@NotNull String id, boolean catalogs) {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getId()).thenReturn(id);
        Mockito.when(container.getName()).thenReturn(id);
        Mockito.when(container.getProject()).thenReturn(project);
        Mockito.when(container.isConnected()).thenReturn(true);
        DBPDataSource source = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dialect.getDialectName()).thenReturn(id);
        Mockito.when(source.getSQLDialect()).thenReturn(dialect);
        Mockito.when(container.getDataSource()).thenReturn(source);
        DBCExecutionContext execution = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(execution.getDataSource()).thenReturn(source);
        try {
            Mockito.doReturn(catalogs ? DBSCatalog.class : DBSTable.class)
                .when((DBSObjectContainer) source).getPrimaryChildType(Mockito.any());
            if (catalogs) {
                DBSCatalog catalog = Mockito.mock(DBSCatalog.class);
                Mockito.when(catalog.getName()).thenReturn("database");
                Mockito.doReturn(DBSSchema.class).when(catalog).getPrimaryChildType(Mockito.any());
                DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
                Mockito.doReturn(catalog).when(defaults).getDefaultCatalog();
                DBSSchema schema = Mockito.mock(DBSSchema.class);
                Mockito.when(schema.getName()).thenReturn("public");
                Mockito.doReturn(schema).when(defaults).getDefaultSchema();
                Mockito.doReturn(defaults).when(execution).getContextDefaults();
            }
        } catch (DBException exception) {
            throw new AssertionError(exception);
        }
        AIDatabaseContext database = Mockito.mock(AIDatabaseContext.class);
        Mockito.when(database.getExecutionContext()).thenReturn(execution);
        Mockito.when(database.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        Mockito.when(database.isObjectAllowed(Mockito.any())).thenReturn(true);
        DBSLogicalDataSource logical = Mockito.mock(DBSLogicalDataSource.class);
        Mockito.when(logical.getDataSourceContainer()).thenReturn(container);
        Mockito.when(database.getDataSource()).thenReturn(logical);
        return new Fixture(container, source, execution, database);
    }

    @NotNull
    private static JsonObject connection(@NotNull String prompt, @NotNull String id) {
        return prompt.lines().filter(line -> line.startsWith("{"))
            .map(line -> JsonParser.parseString(line).getAsJsonObject())
            .filter(info -> id.equals(info.get("connectionId").getAsString())).findFirst().orElseThrow();
    }

    private record Fixture(
        @NotNull DBPDataSourceContainer container,
        @NotNull DBPDataSource dataSource,
        @NotNull DBCExecutionContext execution,
        @NotNull AIDatabaseContext database
    ) {
    }
}
