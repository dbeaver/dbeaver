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

import net.sf.jsqlparser.statement.UnsupportedStatement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class SQLExecutionFunctionsTest {
    @Test
    void verifyQueryDispatchesToSemanticOrTextChecks() throws DBException {
        class RecordingFunction extends ExecuteDQLFunction {
            int semanticChecks;
            int textChecks;

            @Override
            protected void verifyStatement(
                @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull SQLQuery query
            ) {
                semanticChecks++;
            }

            @Override
            protected void verifyQueryText(
                @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String text
            ) {
                textChecks++;
            }
        }
        RecordingFunction function = new RecordingFunction();
        AIFunctionContext context = Mockito.mock(AIFunctionContext.class);
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        function.verifyQuery(context, executionContext, new SQLQuery(null, "SELECT 1"));
        SQLQuery unparsed = Mockito.mock(SQLQuery.class);
        Mockito.when(unparsed.getText()).thenReturn("SELECT unsupported syntax");
        function.verifyQuery(context, executionContext, unparsed);
        Mockito.when(unparsed.getStatement()).thenReturn(new UnsupportedStatement(List.of("SELECT", "unsupported")));
        function.verifyQuery(context, executionContext, unparsed);
        Assertions.assertEquals(1, function.semanticChecks);
        Assertions.assertEquals(2, function.textChecks);
    }

    @Test
    void dqlAcceptsOnlyReadOnlySelectStatements() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDQLFunction function = new ExecuteDQLFunction();
        Assertions.assertDoesNotThrow(() -> fixture.verify(function, "SELECT 1"));
        fixture.verify(function, "VALUES (1)");
        fixture.verify(function, "TABLE public.orders");
        fixture.verify(function, "SELECT * FROM public.orders");
        fixture.verify(function, "SELECT public.read_payroll()");
        fixture.verify(function, "WITH x AS (SELECT * FROM public.orders) SELECT * FROM x");
        // An unparsed WITH statement cannot be assumed to contain only reads.
        fixture.assertRestricted(function, "WITH x AS (TABLE public.orders) SELECT * FROM x");
        for (String sql : List.of("INSERT INTO public.orders VALUES (1)", "SELECT * INTO public.copy FROM public.orders",
            "WITH x AS (DELETE FROM public.orders RETURNING *) SELECT * FROM x", "SELECT * FROM public.orders FOR UPDATE")) {
            fixture.assertRestricted(function, sql);
        }
        fixture.assertRestricted(function, "WITH x AS (SELECT * FROM private.orders) SELECT * FROM x");
        fixture.assertRestricted(function, "WITH x AS (TABLE private.orders) SELECT * FROM x");
        fixture.assertRestricted(function, "SELECT private.read_payroll()");
        fixture.assertRestricted(function, "SELECT public.orders()");
    }

    @Test
    void semanticChecksRejectOtherStatementTypesBeforeLookingUpScope() {
        Map<ExecuteSQLFunction, String> mismatches = Map.of(
            new ExecuteDQLFunction(), "DELETE FROM public.orders",
            new ExecuteDMLFunction(), "SELECT * FROM public.orders",
            new ExecuteDDLFunction(), "UPDATE public.orders SET id=1",
            new ExecuteDCLFunction(), "DROP TABLE public.orders"
        );
        for (var entry : mismatches.entrySet()) {
            ExecuteSQLFunction function = entry.getKey();
            String sql = entry.getValue();
            AIFunctionContext context = Mockito.mock(AIFunctionContext.class);
            DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
            Assertions.assertThrows(DBException.class, () -> function.verifyQuery(context, executionContext, new SQLQuery(null, sql)));
            Mockito.verifyNoInteractions(context, executionContext);
        }
    }

    @Test
    void dmlVerifiesModificationTargetsAndReadSourcesSemantically() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDMLFunction function = new ExecuteDMLFunction();
        for (String sql : List.of("INSERT INTO public.orders VALUES (1)", "UPDATE public.orders SET id=2",
            "DELETE FROM public.orders", "REPLACE INTO public.orders VALUES (1)", "CALL public.refresh_orders()",
            "MERGE INTO public.orders t USING public.source s ON (t.id=s.id) WHEN MATCHED THEN UPDATE SET id=s.id")) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(function, sql), sql);
        }
        fixture.assertRestricted(function, "DELETE FROM private.orders");
        fixture.assertRestricted(function, "INSERT INTO public.orders SELECT * FROM private.orders");
        fixture.assertRestricted(function, "UPDATE public.orders SET id=(SELECT max(id) FROM private.orders)");
        fixture.assertRestricted(function, "CALL public.refresh_orders(private.read_payroll())");
    }

    @Test
    void ddlVerifiesDefinitionTargetsAndSemanticSources() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDDLFunction function = new ExecuteDDLFunction();
        for (String sql : List.of("CREATE TABLE public.new_orders (id int)", "CREATE SEQUENCE public.order_ids",
            "ALTER TABLE public.orders ADD COLUMN name text", "CREATE INDEX public.idx ON public.orders(id)",
            "DROP VIEW public.report", "TRUNCATE TABLE public.orders",
            "RENAME TABLE public.orders TO public.old_orders", "COMMENT ON COLUMN public.orders.id IS 'id'")) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(function, sql), sql);
        }
        for (String sql : List.of("CREATE TABLE private.new_orders (id int)",
            "CREATE TABLE public.copy AS SELECT * FROM private.orders", "CREATE INDEX public.idx ON private.orders(id)",
            "ALTER TABLE public.orders ADD FOREIGN KEY (id) REFERENCES private.orders(id)",
            "CREATE TABLE public.copy (id int REFERENCES private.orders(id))", "TRUNCATE TABLE private.orders",
            "DROP TABLE public.orders, private.orders",
            "ALTER TABLE private.orders ADD COLUMN name text", "CREATE SCHEMA new_schema", "CREATE ROLE reader")) {
            fixture.assertRestricted(function, sql);
        }
    }

    @Test
    void dclVerifiesObjectPrivilegesAndGlobalRoleOperations() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDCLFunction function = new ExecuteDCLFunction();
        Assertions.assertDoesNotThrow(() -> fixture.verify(function, "GRANT SELECT ON public.orders TO reader"));
        fixture.verify(function, "GRANT USAGE ON SCHEMA public TO reader");
        fixture.assertRestricted(function, "REVOKE SELECT ON private.orders FROM reader");
        fixture.assertRestricted(function, "GRANT USAGE ON SCHEMA private TO reader");
        fixture.assertRestricted(function, "GRANT CONNECT ON DATABASE private TO reader");
        fixture.assertRestricted(function, "GRANT SELECT ON public.orders, private.orders TO reader");
        fixture.assertRestricted(function, "DENY SELECT ON OBJECT::private.orders TO reader");
        fixture.assertRestricted(function, "GRANT reporting TO reader");
        fixture.assertRestricted(function, "SET ROLE reader");
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        fixture.verify(function, "GRANT reporting TO reader");
        fixture.verify(function, "SET ROLE reader");
        fixture.verify(function, "SET SESSION AUTHORIZATION reader");
    }

    @Test
    void textChecksApplyTheirOwnStatementWhitelistsAndScopeRules() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        Assertions.assertDoesNotThrow(() -> fixture.verifyText(new ExecuteDQLFunction(), "SELECT 'DELETE FROM private.decoy' FROM public.orders"));
        fixture.verifyText(new ExecuteDMLFunction(), "/* prefix */ UPDATE public.orders SET id=1");
        fixture.verifyText(new ExecuteDDLFunction(), "CREATE DOMAIN public.positive AS integer CHECK (VALUE > 0)");
        fixture.verifyText(new ExecuteDCLFunction(), "REVOKE SELECT ON public.orders FROM reader");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "UPDATE public.orders SET id=1");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "SELECT * FROM public.orders");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "GRANT SELECT ON public.orders TO reader");
        fixture.assertTextRestricted(new ExecuteDCLFunction(), "CREATE TABLE public.orders (id int)");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT * FROM private.orders");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "INSERT INTO public.orders SELECT * FROM private.orders");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "CALL public.refresh_orders(private.read_payroll())");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "CREATE INDEX public.idx ON private.orders(id)");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "DROP TABLE public.orders, private.orders");
        fixture.assertTextRestricted(new ExecuteDCLFunction(), "REVOKE SELECT ON public.orders, private.orders FROM reader");
    }

    @Test
    void executableCommentsAndUnterminatedStringsCannotBypassTextChecks() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        Assertions.assertThrows(DBException.class,
            () -> fixture.verify(new ExecuteDQLFunction(), "SELECT * FROM public.orders /*! UNION SELECT * FROM private.orders */"));
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT 'unterminated FROM public.orders");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT * INTO public.copy FROM public.orders");
    }

    @Test
    void dmlResultIsAnInformationValueContainingTheModifiedRowCount() {
        DBCStatistics statistics = new DBCStatistics();
        statistics.addRowsUpdated(2);
        statistics.addRowsUpdated(5);
        ExecuteDMLFunction function = new ExecuteDMLFunction();
        Assertions.assertEquals(AIFunctionType.INFORMATION, function.getResultType());
        Assertions.assertEquals(7L, function.createResult(statistics).getValue());
        Assertions.assertEquals(-1L, function.createResult(new DBCStatistics()).getValue());
    }

    @Test
    void routineDisplayNameCanBeResolvedWhenItsUniqueCacheKeyIncludesASignature() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        DBSSchema schema = (DBSSchema) ((DBSObjectContainer) fixture.dataSource).getChild(null, "public");
        DBSProcedureContainer procedures = (DBSProcedureContainer) schema;
        DBSProcedure procedure = Mockito.mock(DBSProcedure.class);
        Mockito.when(procedure.getName()).thenReturn("refresh_orders");
        Mockito.when(procedure.getDataSource()).thenReturn(fixture.dataSource);
        Mockito.when(procedure.getParentObject()).thenReturn(schema);
        Mockito.when(procedures.getProcedure(Mockito.any(), Mockito.anyString())).thenReturn(null);
        Mockito.doReturn(List.of(procedure)).when(procedures).getProcedures(Mockito.any());
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(), "SELECT public.refresh_orders()"));
    }

    @Test
    void explicitlyUnknownCatalogDoesNotSelectTheDefault() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDQLFunction function = new ExecuteDQLFunction();
        Assertions.assertSame(fixture.executionContext, function.openExecutionContext(fixture.context, null));
        Assertions.assertThrows(DBException.class, () -> function.openExecutionContext(fixture.context, "unknown"));
    }

    @Test
    void structuralCatalogUsesAnIsolatedContextAndLeavesTheOriginalUnchanged() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        DBSCatalog catalog = Mockito.mock(DBSCatalog.class);
        Mockito.when(((DBSObjectContainer) fixture.dataSource).getChild(Mockito.any(), Mockito.eq("sales"))).thenReturn(catalog);
        DBSInstance owner = Mockito.mock(DBSInstance.class);
        Mockito.when(fixture.executionContext.getOwnerInstance()).thenReturn(owner);
        DBCExecutionContext isolated = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(owner.openIsolatedContext(Mockito.any(), Mockito.anyString(), Mockito.same(fixture.executionContext))).thenReturn(isolated);
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(isolated.getContextDefaults()).thenReturn(defaults);
        Mockito.when(defaults.supportsCatalogChange()).thenReturn(true);

        Assertions.assertSame(isolated, new ExecuteDQLFunction().openExecutionContext(fixture.context, "sales"));
        Mockito.verify((DBCExecutionContextDefaults) defaults).setDefaultCatalog(Mockito.any(), Mockito.same(catalog), Mockito.isNull());
        Mockito.verify(fixture.executionContext, Mockito.never()).getContextDefaults();
    }

    @Test
    void catalogOutsideOriginalScopeIsRejectedBeforeOpeningAContext() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATABASE);
        DBSCatalog catalog = Mockito.mock(DBSCatalog.class);
        Mockito.when(((DBSObjectContainer) fixture.dataSource).getChild(Mockito.any(), Mockito.eq("other"))).thenReturn(catalog);
        Assertions.assertThrows(DBException.class, () -> new ExecuteDQLFunction().openExecutionContext(fixture.context, "other"));
        Mockito.verify(fixture.executionContext, Mockito.never()).getOwnerInstance();
    }

    private static class ScopeFixture {
        private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
            Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        private final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        private final AIDatabaseContext databaseContext = Mockito.mock(AIDatabaseContext.class);
        private final AIFunctionContext context = Mockito.mock(AIFunctionContext.class);

        private ScopeFixture() throws DBException {
            Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
            Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
            Mockito.when(context.getContext()).thenReturn(databaseContext);
            Mockito.when(databaseContext.getExecutionContext()).thenReturn(executionContext);
            Mockito.when(databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_SCHEMA);
            Mockito.when(databaseContext.isObjectAllowed(Mockito.any())).thenReturn(true);
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
            DBSSchema schema = Mockito.mock(DBSSchema.class, Mockito.withSettings().extraInterfaces(DBSProcedureContainer.class));
            Mockito.when(schema.getName()).thenReturn(name);
            Mockito.when(schema.getDataSource()).thenReturn(dataSource);
            Mockito.when(schema.getParentObject()).thenReturn(dataSource);
            Mockito.doReturn(DBSEntity.class).when(schema).getPrimaryChildType(Mockito.any());
            DBSEntity table = Mockito.mock(DBSEntity.class);
            Mockito.when(table.getName()).thenReturn("orders");
            Mockito.when(table.getDataSource()).thenReturn(dataSource);
            Mockito.when(table.getParentObject()).thenReturn(schema);
            Mockito.when(schema.getChild(Mockito.any(), Mockito.anyString())).thenAnswer(invocation -> {
                String child = invocation.getArgument(1);
                return child.equalsIgnoreCase("orders") || child.equalsIgnoreCase("source") ? table : null;
            });
            DBSProcedure procedure = Mockito.mock(DBSProcedure.class);
            Mockito.when(procedure.getName()).thenReturn("refresh_orders");
            Mockito.when(procedure.getDataSource()).thenReturn(dataSource);
            Mockito.when(procedure.getParentObject()).thenReturn(schema);
            Mockito.when(((DBSProcedureContainer) schema).getProcedure(Mockito.any(), Mockito.anyString())).thenAnswer(invocation -> {
                String child = invocation.getArgument(1);
                return child.equalsIgnoreCase("refresh_orders") || child.equalsIgnoreCase("read_payroll") ? procedure : null;
            });
            return schema;
        }

        private void verify(@NotNull ExecuteSQLFunction function, @NotNull String sql) throws DBException {
            function.verifyQuery(context, executionContext, new SQLQuery(null, sql));
        }

        private void verifyText(@NotNull ExecuteSQLFunction function, @NotNull String sql) throws DBException {
            SQLQuery query = Mockito.mock(SQLQuery.class);
            Mockito.when(query.getText()).thenReturn(sql);
            function.verifyQuery(context, executionContext, query);
        }

        private void assertRestricted(@NotNull ExecuteSQLFunction function, @NotNull String sql) {
            Assertions.assertThrows(DBException.class, () -> verify(function, sql), sql);
        }

        private void assertTextRestricted(@NotNull ExecuteSQLFunction function, @NotNull String sql) {
            Assertions.assertThrows(DBException.class, () -> verifyText(function, sql), sql);
        }
    }
}
