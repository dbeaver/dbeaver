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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.functions.heuristics.DCLScopeHeuristics;
import org.jkiss.dbeaver.model.ai.functions.heuristics.DDLScopeHeuristics;
import org.jkiss.dbeaver.model.ai.functions.heuristics.DMLScopeHeuristics;
import org.jkiss.dbeaver.model.ai.functions.heuristics.SQLScopeHeuristics;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class SQLExecutionFunctionsTest {
    @Test
    void dqlAcceptsReadOnlyQueriesAndRejectsWrites() {
        ExecuteDQLFunction function = new ExecuteDQLFunction();
        for (String sql : List.of("SELECT 1", "SELECT * FROM public.orders", "VALUES (1)",
            "WITH x AS (SELECT * FROM public.orders) SELECT * FROM x", "EXPLAIN SELECT * FROM public.orders")) {
            Assertions.assertDoesNotThrow(() -> function.validateQuery(new SQLQuery(null, sql)), sql);
        }
        for (String sql : List.of("INSERT INTO public.orders VALUES (1)", "TRUNCATE TABLE public.orders",
            "GRANT SELECT ON public.orders TO reader", "SELECT * INTO public.copy FROM public.orders",
            "WITH x AS (DELETE FROM public.orders RETURNING *) SELECT * FROM x", "EXPLAIN ANALYZE DELETE FROM public.orders",
            "SELECT * FROM public.orders FOR UPDATE")) {
            Assertions.assertThrows(DBException.class, () -> function.validateQuery(new SQLQuery(null, sql)), sql);
        }
    }

    @Test
    void toolsAcceptTheirOwnStandardStatementFamilies() {
        Map<ExecuteSQLFunction, List<String>> statements = Map.of(
            new ExecuteDMLFunction(), List.of(
                "INSERT INTO public.orders VALUES (1)", "UPDATE public.orders SET id=2", "DELETE FROM public.orders",
                "MERGE INTO public.orders t USING public.source s ON (t.id=s.id) WHEN MATCHED THEN UPDATE SET id=s.id",
                "CALL public.refresh_orders()", "REPLACE INTO public.orders VALUES (1)",
                "WITH changed AS (DELETE FROM public.orders RETURNING *) SELECT * FROM changed"),
            new ExecuteDDLFunction(), List.of(
                "CREATE TABLE public.orders (id int)", "ALTER TABLE public.orders ADD COLUMN name varchar(10)",
                "DROP VIEW public.report", "TRUNCATE TABLE public.orders", "RENAME TABLE public.orders TO public.old_orders",
                "COMMENT ON TABLE public.orders IS 'orders'", "CREATE SEQUENCE public.order_ids",
                "CREATE DOMAIN public.positive AS integer CHECK (VALUE > 0)", "CREATE ROLE reader"),
            new ExecuteDCLFunction(), List.of(
                "GRANT SELECT ON public.orders TO reader", "REVOKE SELECT ON public.orders FROM reader",
                "DENY SELECT ON public.orders TO reader", "GRANT reporting TO reader", "SET ROLE reader",
                "SET SESSION AUTHORIZATION reader", "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO reader")
        );
        statements.forEach((function, queries) -> queries.forEach(sql ->
            Assertions.assertDoesNotThrow(() -> function.validateQuery(new SQLQuery(null, sql)), sql)));
        statements.forEach((function, queries) -> statements.forEach((other, otherQueries) -> {
            if (function.getClass() != other.getClass()) {
                otherQueries.forEach(sql -> Assertions.assertThrows(DBException.class,
                    () -> function.validateQuery(new SQLQuery(null, sql)), sql));
            }
        }));
    }

    @Test
    void fallbackClassifiesCommentsAndCtesWithoutTrustingLiterals() {
        Assertions.assertEquals(SQLQueryCategory.DML,
            SQLScopeHeuristics.getQueryCategory("/* select */ WITH x AS (SELECT * FROM source) UPDATE target SET id=1"));
        Assertions.assertEquals(SQLQueryCategory.DQL,
            SQLScopeHeuristics.getQueryCategory("SELECT 'DROP TABLE private.orders' FROM public.orders"));
        Assertions.assertEquals(SQLQueryCategory.UNKNOWN, SQLScopeHeuristics.getQueryCategory("-- SELECT 1"));
        Assertions.assertEquals(SQLQueryCategory.UNKNOWN, SQLScopeHeuristics.getQueryCategory("BEGIN DELETE FROM orders; END"));
        Assertions.assertEquals(SQLQueryCategory.UNKNOWN,
            SQLScopeHeuristics.getQueryCategory("SELECT * FROM public.orders /*! UNION SELECT * INTO private.copy FROM public.orders */"));
    }

    @Test
    void heuristicStrategiesUseCategorySpecificReferenceRules() {
        Assertions.assertEquals(Set.of("public.orders", "private.source"), new DMLScopeHeuristics()
            .extractReferences("UPDATE public.orders SET id=(SELECT id FROM private.source)").stream()
            .map(SQLScopeHeuristics.ObjectReference::name).collect(Collectors.toSet()));
        Assertions.assertTrue(new DDLScopeHeuristics().extractReferences("CREATE TABLE public.copy AS SELECT * FROM public.orders")
            .contains(new SQLScopeHeuristics.ObjectReference("public.copy", "TABLE", true)));
        Assertions.assertEquals(List.of(new SQLScopeHeuristics.ObjectReference("public.orders", "OBJECT", false)),
            new DCLScopeHeuristics().extractReferences("GRANT UPDATE(id), DELETE ON public.orders TO reader"));
        Assertions.assertEquals(List.of(), new DCLScopeHeuristics().extractReferences("GRANT reporting TO reader"));
        Assertions.assertEquals(List.of(new SQLScopeHeuristics.ObjectReference("public", "SCHEMA", false)),
            new DCLScopeHeuristics().extractReferences("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO reader"));
        Assertions.assertEquals(List.of(), new DCLScopeHeuristics()
            .extractReferences("ALTER DEFAULT PRIVILEGES GRANT SELECT ON TABLES TO reader"));
    }

    @Test
    void mutationToolsDoNotConsumeResultFormattingParameters() throws DBException {
        for (ExecuteSQLFunction function : List.of(new ExecuteDMLFunction(), new ExecuteDDLFunction(), new ExecuteDCLFunction())) {
            Assertions.assertNull(function.getResultProcessor(Map.of("format", "unknown")));
            Assertions.assertEquals(0, function.getMaxRows(Map.of("maxRows", -1)));
            Assertions.assertEquals(AIFunctionType.INFORMATION, function.getResultType());
        }
        Assertions.assertEquals(42, new ExecuteDQLFunction().getMaxRows(Map.of("maxRows", 42)));
        Assertions.assertEquals(AIFunctionType.INFORMATION, new ExecuteDQLFunction().getResultType());
    }

    @Test
    void dmlReturnsNumericModifiedRowCountAndDqlReturnsResultData() {
        DBCStatistics first = new DBCStatistics();
        first.addRowsUpdated(2);
        DBCStatistics second = new DBCStatistics();
        second.addRowsUpdated(5);
        DBCStatistics total = new DBCStatistics();
        total.accumulate(first);
        total.accumulate(second);
        Assertions.assertEquals(7L, new ExecuteDMLFunction().getResultValue("", total));
        Assertions.assertEquals(-1L, new ExecuteDMLFunction().getResultValue("", new DBCStatistics()));
        Assertions.assertEquals("id\n1\n", new ExecuteDQLFunction().getResultValue("id\n1\n", total));
    }

    @Test
    void scopeVerificationRejectsStatementsFromOtherCategoriesBeforeResolvingObjects() {
        Map<ExecuteSQLFunction, String> mismatches = Map.of(
            new ExecuteDQLFunction(), "DELETE FROM public.orders",
            new ExecuteDMLFunction(), "SELECT * FROM public.orders",
            new ExecuteDDLFunction(), "UPDATE public.orders SET id=1",
            new ExecuteDCLFunction(), "DROP TABLE public.orders"
        );
        mismatches.forEach((function, sql) -> {
            DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
            AIDatabaseContext context = Mockito.mock(AIDatabaseContext.class);
            Assertions.assertThrows(DBException.class,
                () -> function.verifyObjectsInScope(new SQLQuery(null, sql), executionContext, context));
            Mockito.verifyNoInteractions(executionContext, context);
        });
    }

    @Test
    void dmlChecksTargetsAndSubquerySources() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDMLFunction function = new ExecuteDMLFunction();
        Assertions.assertNull(fixture.verify(function, "UPDATE public.orders SET id=1"));
        fixture.assertRestricted(function, "DELETE FROM private.orders");
        fixture.assertRestricted(function, "INSERT INTO public.orders SELECT * FROM private.orders");
        fixture.assertRestricted(function, "UPDATE public.orders SET id=(SELECT max(id) FROM private.orders)");
        fixture.assertRestricted(function, "WITH changed AS (DELETE FROM private.orders RETURNING *) SELECT * FROM changed");
    }

    @Test
    void ddlChecksNewObjectNamespaceAndExistingSources() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDDLFunction function = new ExecuteDDLFunction();
        Assertions.assertNull(fixture.verify(function, "CREATE TABLE public.new_orders (id int)"));
        Assertions.assertNull(fixture.verify(function, "CREATE SEQUENCE public.order_ids"));
        Assertions.assertNull(fixture.verify(function, "COMMENT ON COLUMN public.orders.id IS 'id'"));
        fixture.assertRestricted(function, "CREATE TABLE private.new_orders (id int)");
        fixture.assertRestricted(function, "CREATE TABLE public.copy AS SELECT * FROM private.orders");
        fixture.assertRestricted(function, "TRUNCATE TABLE private.orders");
        fixture.assertRestricted(function, "ALTER TABLE private.orders ADD COLUMN name text");
        fixture.assertRestricted(function, "CREATE ROLE reader");
        fixture.assertRestricted(function, "CREATE SCHEMA new_schema");
    }

    @Test
    void dclChecksObjectPrivilegesAndRequiresFullScopeForRoleGrants() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDCLFunction function = new ExecuteDCLFunction();
        Assertions.assertNull(fixture.verify(function, "GRANT SELECT ON public.orders TO reader"));
        fixture.assertRestricted(function, "REVOKE SELECT ON private.orders FROM reader");
        fixture.assertRestricted(function, "GRANT SELECT ON public.orders, private.orders TO reader");
        fixture.assertRestricted(function, "DENY SELECT ON OBJECT::private.orders TO reader");
        fixture.assertRestricted(function, "GRANT reporting TO reader");
        Mockito.when(fixture.context.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        Assertions.assertNull(fixture.verify(function, "GRANT reporting TO reader"));
    }

    private static class ScopeFixture {
        private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
            Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        private final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        private final AIDatabaseContext context = Mockito.mock(AIDatabaseContext.class);

        private ScopeFixture() throws DBException {
            Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
            Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
            Mockito.when(context.getScope()).thenReturn(AIDatabaseScope.CURRENT_SCHEMA);
            Mockito.when(context.isObjectAllowed(Mockito.any())).thenReturn(true);
            DBSSchema publicSchema = schema("public");
            DBSSchema privateSchema = schema("private");
            DBSObjectContainer root = (DBSObjectContainer) dataSource;
            Mockito.doReturn(DBSSchema.class).when(root).getPrimaryChildType(Mockito.any());
            Mockito.when(root.getChild(Mockito.any(), Mockito.anyString())).thenAnswer(invocation -> {
                String name = invocation.getArgument(1);
                return name.equalsIgnoreCase("public") ? publicSchema : name.equalsIgnoreCase("private") ? privateSchema : null;
            });
            DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
            Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
            Mockito.doReturn(publicSchema).when(defaults).getDefaultSchema();
            Mockito.when(defaults.supportsSchemaChange()).thenReturn(true);
        }

        @NotNull
        private DBSSchema schema(@NotNull String name) throws DBException {
            DBSSchema schema = Mockito.mock(DBSSchema.class);
            Mockito.when(schema.getName()).thenReturn(name);
            Mockito.when(schema.getDataSource()).thenReturn(dataSource);
            Mockito.when(schema.getParentObject()).thenReturn(dataSource);
            Mockito.doReturn(DBSEntity.class).when(schema).getPrimaryChildType(Mockito.any());
            DBSEntity table = Mockito.mock(DBSEntity.class);
            Mockito.when(table.getName()).thenReturn("orders");
            Mockito.when(table.getDataSource()).thenReturn(dataSource);
            Mockito.when(table.getParentObject()).thenReturn(schema);
            Mockito.when(schema.getChild(Mockito.any(), Mockito.anyString())).thenAnswer(invocation ->
                "orders".equalsIgnoreCase(invocation.getArgument(1)) ? table : null);
            return schema;
        }

        @Nullable
        private AIFunctionResult verify(@NotNull ExecuteSQLFunction function, @NotNull String sql) throws DBException {
            return function.verifyObjectsInScope(new SQLQuery(null, sql), executionContext, context);
        }

        private void assertRestricted(@NotNull ExecuteSQLFunction function, @NotNull String sql) throws DBException {
            Assertions.assertNotNull(verify(function, sql), sql);
        }
    }
}
