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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class AIObjectScopeTest extends DBeaverUnitTest {
    private final AIDatabaseContext databaseContext = Mockito.mock(AIDatabaseContext.class);
    private final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
    private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
        Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
    private final DBSObjectContainer root = (DBSObjectContainer) dataSource;
    private final DBSDataContainer allowed = Mockito.mock(DBSDataContainer.class);
    private final DBSDataContainer secret = Mockito.mock(DBSDataContainer.class);
    private final DBSSchema schema = Mockito.mock(DBSSchema.class);
    private final AIFunctionContext context = new AIFunctionContext(new VoidProgressMonitor(), databaseContext,
        Mockito.mock(AIPromptGenerator.class));

    @BeforeEach
    public void setUp() throws DBException {
        Mockito.when(databaseContext.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        Mockito.when(root.getDataSource()).thenReturn(dataSource);
        Mockito.doReturn(DBSTable.class).when(root).getPrimaryChildType(Mockito.any());
        Mockito.when(root.getChild(Mockito.any(), Mockito.eq("private"))).thenReturn(schema);
        Mockito.when(allowed.getParentObject()).thenReturn(root);
        Mockito.when(secret.getParentObject()).thenReturn(root);
        Mockito.when(databaseContext.isObjectAllowed(Mockito.any())).thenReturn(true);
        Mockito.when(databaseContext.isObjectAllowed(secret)).thenReturn(false);
    }

    @Test
    public void tableResolutionRejectsObjectsOutsideDataSourceScope() {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "secret", secret);
            DBException failure = Assertions.assertThrows(DBException.class, () -> AIUtils.findTableInContext(context, "secret"));
            Assertions.assertTrue(failure.getMessage().contains("outside the allowed scope"));
        }
    }

    @Test
    public void tableResolutionRejectsObjectsOutsideCustomScope() {
        Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CUSTOM);
        Mockito.when(databaseContext.getCustomEntities()).thenReturn(List.of(secret));
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "allowed", allowed);
            DBException failure = Assertions.assertThrows(DBException.class, () -> AIUtils.findTableInContext(context, "allowed"));
            Assertions.assertTrue(failure.getMessage().contains("outside the allowed scope"));
        }
    }

    @Test
    public void schemaResolutionRejectsObjectsOutsideDataSourceScope() {
        Mockito.when(databaseContext.isObjectAllowed(schema)).thenReturn(false);
        DBException failure = Assertions.assertThrows(DBException.class, () -> AIUtils.findSchemaInContext(context, "private"));
        Assertions.assertTrue(failure.getMessage().contains("outside the allowed scope"));
    }

    @Test
    public void schemaResolutionRejectsObjectsOutsideCustomScope() {
        Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CUSTOM);
        DBException failure = Assertions.assertThrows(DBException.class, () -> AIUtils.findSchemaInContext(context, "private"));
        Assertions.assertTrue(failure.getMessage().contains("outside the allowed scope"));
    }

    @Test
    public void sqlChecksTablesInJoinsAndSubqueries() throws DBException {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "allowed", allowed);
            stubTable(dbUtils, "secret", secret);
            AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SELECT * FROM allowed"));
            Assertions.assertThrows(DBException.class, () -> AIUtils.checkQueryInScope(context,
                new SQLQuery(dataSource, "SELECT * FROM allowed a JOIN secret s ON a.id=s.id")));
            Assertions.assertThrows(DBException.class, () -> AIUtils.checkQueryInScope(context,
                new SQLQuery(dataSource, "SELECT * FROM allowed WHERE EXISTS (SELECT 1 FROM secret)")));
        }
    }

    @Test
    public void sqlResolvesCteSourcesWithoutTreatingAliasesAsTables() throws DBException {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "allowed", allowed);
            stubTable(dbUtils, "secret", secret);
            AIUtils.checkQueryInScope(context,
                new SQLQuery(dataSource, "WITH recent AS (SELECT * FROM allowed) SELECT * FROM recent"));
            Assertions.assertThrows(DBException.class, () -> AIUtils.checkQueryInScope(context,
                new SQLQuery(dataSource, "WITH recent AS (SELECT * FROM secret) SELECT * FROM recent")));
        }
    }

    @Test
    public void tableResolutionRejectsServerQualifiedNames() {
        DBException failure = Assertions.assertThrows(DBException.class,
            () -> AIUtils.findTableInContext(context, "remoteServer.allowed_db.dbo.allowed"));
        Assertions.assertTrue(failure.getMessage().contains("Unsupported qualified table name"));
    }

    @Test
    public void sqlRejectsServerQualifiedNames() {
        DBException failure = Assertions.assertThrows(DBException.class, () -> AIUtils.checkQueryInScope(context,
            new SQLQuery(dataSource, "SELECT * FROM remoteServer.allowed_db.dbo.allowed")));
        Assertions.assertTrue(failure.getMessage().contains("Unsupported qualified table name"));
    }

    @Test
    public void tableResolutionAllowsQuotedNamesContainingDots() throws DBException {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "\"allowed.with.dots\"", allowed);
            Assertions.assertSame(allowed, AIUtils.findTableInContext(context, "\"allowed.with.dots\""));
        }
    }

    @Test
    public void sqlRejectsUnresolvedObjects() {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            Assertions.assertThrows(DBException.class,
                () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SELECT * FROM missing")));
        }
    }

    @Test
    public void sqlRejectsUnparseableStatements() {
        Assertions.assertThrows(DBException.class,
            () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SELECT * FROM")));
    }

    @Test
    public void sqlAllowsQueriesWithoutTableReferences() throws DBException {
        AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SELECT 1"));
    }

    @Test
    public void sqlRejectsAdditionalStatementsBeforeResolvingObjects() throws DBException {
        for (String sql : List.of(
            "SELECT 1; DELETE FROM secret",
            "SELECT 1; SELECT 2",
            "SELECT 1; /* trailing command */ SELECT * FROM secret",
            "SELECT 1; -- trailing command\nDELETE FROM secret",
            "SHOW TABLES; DELETE FROM secret",
            "SHOW COLUMNS FROM allowed; DELETE FROM secret"
        )) {
            DBException failure = Assertions.assertThrows(DBException.class,
                () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, sql)));
            Assertions.assertTrue(failure.getMessage().contains("single SQL statement"));
        }
        Mockito.verify(root, Mockito.never()).getChild(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void sqlRejectsMalformedTrailingStatements() {
        Assertions.assertThrows(DBException.class,
            () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SELECT 1; INVALID SQL")));
    }

    @Test
    public void sqlAllowsDelimitersInsideLiteralsAndComments() throws DBException {
        for (String sql : List.of(
            "SELECT '; DELETE FROM secret'",
            "SELECT 1; -- DELETE FROM secret",
            "SELECT 1 /* ; DELETE FROM secret */;",
            "SELECT ';' AS value; /* trailing comment */"
        )) {
            AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, sql));
        }
        Mockito.verify(root, Mockito.never()).getChild(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void sqlSupportsShowTablesWithoutScopeRestrictions() throws DBException {
        AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW TABLES"));
        AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW TABLES LIKE 'orders%'"));
    }

    @Test
    public void sqlAllowsDelimitersInsideQuotedIdentifiers() throws DBException {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "\"allowed;name\"", allowed);
            AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SELECT * FROM \"allowed;name\""));
        }
    }

    @Test
    public void sqlRejectsShowTablesWithScopeRestrictions() {
        for (AIDatabaseScope scope : List.of(AIDatabaseScope.CURRENT_DATABASE, AIDatabaseScope.CURRENT_SCHEMA, AIDatabaseScope.CUSTOM)) {
            Mockito.when(databaseContext.getScope()).thenReturn(scope);
            Assertions.assertThrows(DBException.class,
                () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW TABLES")));
        }
        Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        Mockito.when(databaseContext.hasDataSourceCustomScope()).thenReturn(true);
        Assertions.assertThrows(DBException.class,
            () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW TABLES")));
    }

    @Test
    public void sqlChecksObjectsReferencedByShowTablesFilters() {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "secret", secret);
            Assertions.assertThrows(DBException.class, () -> AIUtils.checkQueryInScope(context,
                new SQLQuery(dataSource, "SHOW TABLES WHERE EXISTS (SELECT 1 FROM secret)")));
        }
    }

    @Test
    public void sqlSupportsShowColumnsWithoutBypassingTableScope() throws DBException {
        try (var dbUtils = Mockito.mockStatic(DBUtils.class, Mockito.CALLS_REAL_METHODS)) {
            stubTable(dbUtils, "allowed", allowed);
            stubTable(dbUtils, "secret", secret);
            AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW COLUMNS FROM allowed"));
            Assertions.assertThrows(DBException.class,
                () -> AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW COLUMNS FROM secret")));
            Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CUSTOM);
            Mockito.when(databaseContext.getCustomEntities()).thenReturn(List.of(allowed));
            AIUtils.checkQueryInScope(context, new SQLQuery(dataSource, "SHOW COLUMNS FROM allowed"));
        }
    }

    private void stubTable(
        @NotNull org.mockito.MockedStatic<DBUtils> dbUtils,
        @NotNull String name,
        @NotNull DBSObject table
    ) {
        String normalizedName = DBUtils.getUnQuotedNormalizedIdentifier(dataSource.getSQLDialect(), name);
        dbUtils.when(() -> DBUtils.getObjectByPath(context.getMonitor(), executionContext, root, null, null, normalizedName, true))
            .thenReturn(table);
    }
}
