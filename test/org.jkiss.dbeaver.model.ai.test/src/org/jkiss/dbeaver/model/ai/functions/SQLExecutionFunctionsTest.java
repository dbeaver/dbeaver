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
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSEntityTypeMapping;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        function.verifyQuery(context, executionContext, new SQLQuery(null, "SELECT 1"));
        SQLQuery unparsed = Mockito.mock(SQLQuery.class);
        Mockito.when(unparsed.getText()).thenReturn("SELECT unsupported syntax");
        function.verifyQuery(context, executionContext, unparsed);
        Mockito.when(unparsed.getStatement()).thenReturn(new UnsupportedStatement(List.of("SELECT", "unsupported")));
        function.verifyQuery(context, executionContext, unparsed);
        Mockito.when(unparsed.getStatement()).thenReturn(new SQLQuery(null, "SELECT 1").getStatement());
        Mockito.when(unparsed.getParseError()).thenReturn(new DBException("Incomplete semantic analysis"));
        function.verifyQuery(context, executionContext, unparsed);
        Assertions.assertEquals(1, function.semanticChecks);
        Assertions.assertEquals(3, function.textChecks);
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
    void inlineExportsAllowAppendableFormatsAndRejectNonAppendableDocuments() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        DataTransferProcessorDescriptor processor = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(processor.getAIFormatId()).thenReturn("csv");
        Mockito.when(processor.isAppendable()).thenReturn(true);
        List<SQLQuery> queries = List.of(new SQLQuery(null, "SELECT 1"), new SQLQuery(null, "SELECT 2"));
        ExecuteDQLFunction function = new ExecuteDQLFunction();
        Assertions.assertDoesNotThrow(() -> function.verifyQueries(fixture.context, fixture.executionContext, queries, processor));
        Mockito.when(processor.getAIFormatId()).thenReturn("json");
        Mockito.when(processor.isAppendable()).thenReturn(false);
        DBException error = Assertions.assertThrows(DBException.class,
            () -> function.verifyQueries(fixture.context, fixture.executionContext, queries, processor));
        Assertions.assertEquals("Format 'json' supports only one statement per call", error.getMessage());
        Mockito.verify(processor, Mockito.never()).getInstance();
        Assertions.assertDoesNotThrow(() -> new ExecuteDMLFunction().verifyQueries(fixture.context, fixture.executionContext,
            List.of(new SQLQuery(null, "UPDATE public.orders SET id=1"), new SQLQuery(null, "DELETE FROM public.orders")), null));
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
    void dmlResolvesCteSourcesWithoutTreatingAliasesAsPhysicalTables() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDMLFunction function = new ExecuteDMLFunction();
        Assertions.assertDoesNotThrow(() -> fixture.verify(function,
            "WITH recent AS (SELECT * FROM public.source) UPDATE public.orders SET id=recent.id FROM recent"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(function,
            "WITH initial_rows AS (SELECT * FROM public.source), recent AS (SELECT * FROM initial_rows) "
                + "UPDATE public.orders SET id=recent.id FROM recent"));
        fixture.assertRestricted(function,
            "WITH recent AS (SELECT * FROM private.orders) UPDATE public.orders SET id=recent.id FROM recent");
        fixture.assertRestricted(function,
            "WITH recent AS (SELECT * FROM public.source) UPDATE private.orders SET id=recent.id FROM recent");
    }

    @Test
    void ddlVerifiesDefinitionTargetsAndSemanticSources() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDDLFunction function = new ExecuteDDLFunction();
        for (String sql : List.of("CREATE TABLE public.new_orders (id int)", "CREATE SEQUENCE public.order_ids",
            "ALTER TABLE public.orders ADD COLUMN name text", "CREATE INDEX public.idx ON public.orders(id)",
            "ALTER VIEW public.report AS SELECT * FROM public.orders",
            "CREATE TABLE public.cache (value text DEFAULT public.read_payroll())",
            "CREATE SYNONYM public.orders_alias FOR public.orders", "DROP VIEW public.report", "TRUNCATE TABLE public.orders",
            "RENAME TABLE public.orders TO public.old_orders", "COMMENT ON COLUMN public.orders.id IS 'id'")) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(function, sql), sql);
        }
        for (String sql : List.of("CREATE TABLE private.new_orders (id int)",
            "CREATE TABLE public.copy AS SELECT * FROM private.orders", "CREATE INDEX public.idx ON private.orders(id)",
            "ALTER TABLE public.orders ADD FOREIGN KEY (id) REFERENCES private.orders(id)",
            "CREATE TABLE public.copy (id int REFERENCES private.orders(id))", "TRUNCATE TABLE private.orders",
            "DROP TABLE public.orders, private.orders",
            "CREATE SYNONYM public.orders_alias FOR private.orders", "CREATE PUBLIC SYNONYM orders_alias FOR public.orders",
            "ALTER VIEW public.report AS SELECT * FROM private.orders", "ALTER TABLE public.orders SET SCHEMA private",
            "CREATE TABLE public.cache (value text DEFAULT private.read_payroll())",
            "ALTER TABLE public.orders ADD COLUMN value text DEFAULT private.read_payroll()",
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
        fixture.verifyText(new ExecuteDDLFunction(), "CREATE SYNONYM public.orders_alias FOR public.orders");
        fixture.verifyText(new ExecuteDCLFunction(), "REVOKE SELECT ON public.orders FROM reader");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "UPDATE public.orders SET id=1");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "SELECT * FROM public.orders");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "GRANT SELECT ON public.orders TO reader");
        fixture.assertTextRestricted(new ExecuteDCLFunction(), "CREATE TABLE public.orders (id int)");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT * FROM private.orders");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "INSERT INTO public.orders SELECT * FROM private.orders");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "CALL public.refresh_orders(private.read_payroll())");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "COPY public.orders TO PROGRAM 'command'");
        fixture.assertTextRestricted(new ExecuteDMLFunction(), "LOAD DATA LOCAL INFILE 'file' INTO TABLE public.orders");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "CREATE INDEX public.idx ON private.orders(id)");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "CREATE SYNONYM public.orders_alias FOR private.orders");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "ALTER TABLE public.orders SET SCHEMA private");
        fixture.assertTextRestricted(new ExecuteDDLFunction(), "DROP TABLE public.orders, private.orders");
        fixture.assertTextRestricted(new ExecuteDCLFunction(), "REVOKE SELECT ON public.orders, private.orders FROM reader");
        fixture.assertTextRestricted(new ExecuteDCLFunction(), "GRANT SELECT ON private.* TO reader");
        Assertions.assertDoesNotThrow(() -> fixture.verifyText(new ExecuteDCLFunction(), "GRANT SELECT ON public.* TO reader"));
    }

    @Test
    void partitionMovesVerifyTheReferencedTable() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDDLFunction function = new ExecuteDDLFunction();
        Assertions.assertDoesNotThrow(() -> fixture.verify(function,
            "ALTER TABLE public.orders ATTACH PARTITION public.source FOR VALUES IN (1)"));
        fixture.assertRestricted(function, "ALTER TABLE public.orders ATTACH PARTITION private.orders FOR VALUES IN (1)");
        fixture.assertTextRestricted(function, "ALTER TABLE public.orders EXCHANGE PARTITION p WITH TABLE private.orders");
        fixture.assertTextRestricted(function, "ALTER TABLE public.orders SWITCH PARTITION 1 TO private.orders");
    }

    @Test
    void cascadingDdlRequiresDatasourceWideScope() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDDLFunction function = new ExecuteDDLFunction();
        List<String> statements = List.of("TRUNCATE TABLE public.orders CASCADE", "DROP TABLE public.orders CASCADE",
            "ALTER TABLE public.orders DROP COLUMN id CASCADE");
        for (String sql : statements) {
            Assertions.assertThrows(DBException.class, () -> fixture.verify(function, sql), sql);
            fixture.assertTextRestricted(function, sql);
        }
        Assertions.assertDoesNotThrow(() -> fixture.verify(function, "ALTER TABLE public.orders DROP COLUMN \"CASCADE\""));
        Assertions.assertDoesNotThrow(() -> fixture.verifyText(function, "COMMENT ON TABLE public.orders IS 'CASCADE'"));
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        for (String sql : statements) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(function, sql), sql);
            Assertions.assertDoesNotThrow(() -> fixture.verifyText(function, sql), sql);
        }
        Mockito.when(fixture.databaseContext.hasDataSourceCustomScope()).thenReturn(true);
        for (String sql : statements) {
            Assertions.assertThrows(DBException.class, () -> fixture.verify(function, sql), sql);
            fixture.assertTextRestricted(function, sql);
        }
    }

    @Test
    void opaqueRoutineDefinitionsRequireDatasourceWideScope() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDDLFunction function = new ExecuteDDLFunction();
        String definition = "CREATE FUNCTION public.allowed() RETURNS integer AS $$ SELECT id FROM private.orders $$ LANGUAGE SQL";
        Assertions.assertThrows(DBException.class, () -> fixture.verify(function, definition));
        fixture.assertTextRestricted(function, definition);
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CURRENT_DATASOURCE);
        Assertions.assertDoesNotThrow(() -> fixture.verify(function, definition));
        Mockito.when(fixture.databaseContext.hasDataSourceCustomScope()).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> fixture.verify(function, definition));
    }

    @Test
    void customTableScopeDoesNotAuthorizeRoutinesOrOtherObjectsInItsSchema() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        DBSSchema schema = (DBSSchema) ((DBSObjectContainer) fixture.dataSource).getChild(null, "public");
        DBSEntity orders = (DBSEntity) schema.getChild(null, "orders");
        DBSSequence sequence = Mockito.mock(DBSSequence.class);
        Mockito.when(sequence.getName()).thenReturn("payroll_seq");
        Mockito.when(sequence.getDataSource()).thenReturn(fixture.dataSource);
        Mockito.when(sequence.getParentObject()).thenReturn(schema);
        DBSEntityContainer container = (DBSEntityContainer) schema;
        Mockito.when(container.getNestedEntityTypes()).thenReturn(new DBSEntityTypeMapping[] {
            new DBSEntityTypeMapping(DBSEntityType.SEQUENCE, DBSSequence.class)
        });
        Mockito.doReturn(List.of(sequence)).when(container).getEntities(Mockito.any(), Mockito.eq(DBSEntityType.SEQUENCE));
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CUSTOM);
        Mockito.when(fixture.databaseContext.getCustomEntities()).thenReturn(List.of(orders));
        Mockito.when(fixture.databaseContext.getCustomSchemas()).thenReturn(Set.of(schema));

        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(), "SELECT count(*) FROM public.orders"));
        fixture.assertRestricted(new ExecuteDQLFunction(), "SELECT read_payroll()");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT read_payroll() FROM public.orders");
        fixture.assertRestricted(new ExecuteDDLFunction(), "DROP SEQUENCE public.payroll_seq");
        fixture.assertRestricted(new ExecuteDDLFunction(), "CREATE TABLE public.extra (id int)");
        fixture.assertRestricted(new ExecuteDDLFunction(), "DROP SCHEMA public");
        fixture.assertRestricted(new ExecuteDCLFunction(), "GRANT USAGE ON SCHEMA public TO reader");
        Mockito.when(fixture.databaseContext.getCustomEntities()).thenReturn(List.of(sequence));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDDLFunction(), "DROP SEQUENCE public.payroll_seq"));
        Mockito.when(fixture.databaseContext.getCustomEntities()).thenReturn(List.of(schema));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDDLFunction(), "DROP SEQUENCE public.payroll_seq"));
    }

    @Test
    void existingIndexesAreCheckedAgainstTheirActualOwningEntity() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        DBSSchema schema = (DBSSchema) ((DBSObjectContainer) fixture.dataSource).getChild(null, "public");
        DBSEntity orders = (DBSEntity) schema.getChild(null, "orders");
        DBSEntity source = (DBSEntity) schema.getChild(null, "source");
        DBSTableIndex ownIndex = Mockito.mock(DBSTableIndex.class);
        Mockito.when(ownIndex.getName()).thenReturn("orders_idx");
        Mockito.when(ownIndex.getDataSource()).thenReturn(fixture.dataSource);
        Mockito.when(ownIndex.getParentObject()).thenReturn(orders);
        DBSTableIndex otherIndex = Mockito.mock(DBSTableIndex.class);
        Mockito.when(otherIndex.getName()).thenReturn("source_idx");
        Mockito.when(otherIndex.getDataSource()).thenReturn(fixture.dataSource);
        Mockito.when(otherIndex.getParentObject()).thenReturn(source);
        DBSEntityType indexType = new DBSEntityType("index", "Index", null, true);
        DBSEntityContainer container = (DBSEntityContainer) schema;
        Mockito.when(container.getNestedEntityTypes()).thenReturn(new DBSEntityTypeMapping[] {
            new DBSEntityTypeMapping(indexType, DBSTableIndex.class)
        });
        Mockito.doReturn(List.of(ownIndex, otherIndex)).when(container).getEntities(Mockito.any(), Mockito.same(indexType));
        Mockito.when(fixture.databaseContext.getScope()).thenReturn(AIDatabaseScope.CUSTOM);
        Mockito.when(fixture.databaseContext.getCustomEntities()).thenReturn(List.of(orders));
        Mockito.when(fixture.databaseContext.getCustomSchemas()).thenReturn(Set.of(schema));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDDLFunction(), "DROP INDEX public.orders_idx"));
        fixture.assertRestricted(new ExecuteDDLFunction(), "DROP INDEX public.source_idx");
    }

    @Test
    void executableCommentsUseSemanticScopeAndStatementTypeChecks() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        fixture.enableExecutableComments();
        ExecuteDQLFunction dql = new ExecuteDQLFunction();
        for (String sql : List.of(
            "SELECT 1 /*! UNION SELECT id FROM public.orders */",
            "SELECT 1 /*!50000 UNION SELECT id FROM public.orders */",
            "SELECT 1 /*M!100100 UNION SELECT id FROM public.orders */",
            "/*! SELECT id FROM public.orders */",
            "SELECT /*!1+1*/",
            "SELECT /*! '*/' AS value FROM public.orders */",
            "SELECT '/*! UNION SELECT id FROM private.orders */'"
        )) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(dql, sql), sql);
        }
        for (String sql : List.of(
            "SELECT 1 /*! UNION SELECT id FROM private.orders */",
            "SELECT 1 /*!50000 UNION SELECT id FROM private.orders */",
            "SELECT 1 /*M!100100 UNION SELECT id FROM private.orders */",
            "SELECT /*!1+1 FROM private.orders */",
            "SELECT /*! '*/' AS value FROM private.orders */",
            "SELECT id FROM public.orders /*! INTO public.copy */"
        )) {
            fixture.assertRestricted(dql, sql);
        }
        Assertions.assertDoesNotThrow(() -> fixture.verify(dql, "SELECT 1 /*!999999 UNION SELECT id FROM private.orders */"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDMLFunction(), "/*! UPDATE public.orders SET id=2 */"));
        fixture.assertRestricted(new ExecuteDMLFunction(), "/*! UPDATE private.orders SET id=2 */");
    }

    @Test
    void dollarQuotedCommentMarkersCannotHideRestrictedTables() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        ExecuteDQLFunction function = new ExecuteDQLFunction();
        for (String literal : List.of("$$--$$", "$$/* hidden */$$")) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(function,
                "SELECT " + literal + " AS value, id FROM public.orders"));
            fixture.assertRestricted(function, "SELECT " + literal + " AS value, id FROM private.orders");
            fixture.assertTextRestricted(function, "SELECT " + literal + " AS value, id FROM private.orders");
        }
    }

    @Test
    void textTokenizationSeesExecutableReferencesAndDialectLineComments() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        fixture.enableExecutableComments();
        List<String> tokens = ExecuteSQLFunction.tokenize(fixture.dataSource.getSQLDialect(),
            "SELECT * FROM public.orders /*! JOIN private.orders ON true */ # ignored FROM other.orders");
        Assertions.assertEquals(Set.of("public.orders", "private.orders"),
            ExecuteSQLFunction.findTextObjects(tokens, Set.of("FROM", "JOIN")));
    }

    @Test
    void executableCommentsAndUnterminatedStringsCannotBypassTextChecks() throws Exception {
        ScopeFixture fixture = new ScopeFixture();
        fixture.enableExecutableComments();
        fixture.setSqlMode("");
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT 'it\\'s' FROM public.orders"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT 'it\\\"s' FROM public.orders"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT \"it\\\"s\" FROM public.orders"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT \"it\\\"s /*! UNION SELECT id FROM private.orders */\" FROM public.orders"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT \"it\\\"s /*! nested /*! hidden */ */\" FROM public.orders"));
        fixture.assertRestricted(new ExecuteDQLFunction(),
            "SELECT \"it\\\"s\" FROM private.orders");
        fixture.assertRestricted(new ExecuteDQLFunction(),
            "SELECT \"it\\\"s\" FROM public.orders /*! UNION SELECT id FROM private.orders */");
        fixture.setSqlMode("NO_BACKSLASH_ESCAPES");
        DBException modeError = Assertions.assertThrows(DBException.class, () -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT 'x\\' /*! UNION SELECT id FROM private.orders */ -- '"));
        Assertions.assertTrue(modeError.getMessage().contains("SQL mode"));
        fixture.setSqlMode("ANSI_QUOTES");
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT 'it\\\"s' FROM public.orders"));
        Assertions.assertDoesNotThrow(() -> fixture.verify(new ExecuteDQLFunction(),
            "SELECT 1 /* \\\" */ FROM public.orders"));
        modeError = Assertions.assertThrows(DBException.class,
            () -> fixture.verify(new ExecuteDQLFunction(), "SELECT \"x\\\" FROM public.orders"));
        Assertions.assertTrue(modeError.getMessage().contains("escaped double quote"));
        fixture.setSqlMode("");
        DBException scriptError = Assertions.assertThrows(DBException.class, () -> new ExecuteDQLFunction().verifyQueries(
            fixture.context, fixture.executionContext,
            List.of(new SQLQuery(fixture.dataSource, "SET SESSION sql_mode='NO_BACKSLASH_ESCAPES'"),
                new SQLQuery(fixture.dataSource, "SELECT 'x\\' FROM public.orders")), null));
        Assertions.assertTrue(scriptError.getMessage().contains("multiple statements"));
        Assertions.assertThrows(DBException.class,
            () -> fixture.verify(new ExecuteDQLFunction(), "SELECT * FROM public.orders /*! UNION SELECT * FROM private.orders */"));
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT 'unterminated FROM public.orders");
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT * INTO public.copy FROM public.orders");
    }

    @Test
    void inertExecutableMarkersDoNotBlockTextFallback() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        fixture.enableExecutableComments();
        ExecuteDQLFunction function = new ExecuteDQLFunction();
        for (String sql : List.of(
            "SELECT '/*!' FROM public.orders WHERE id SOUNDS LIKE 'x'",
            "SELECT id FROM public.orders WHERE id SOUNDS LIKE 'x' /* ordinary /*! */",
            "SELECT id FROM public.orders WHERE id SOUNDS LIKE 'x' /*!999999 ignored */"
        )) {
            Assertions.assertDoesNotThrow(() -> fixture.verify(function, sql), sql);
            fixture.assertRestricted(function, sql.replace("public.orders", "private.orders"));
        }
        fixture.assertRestricted(function,
            "SELECT id FROM public.orders WHERE id SOUNDS LIKE 'x' /*! AND id > 0 */");
        fixture.assertRestricted(function,
            "SELECT id FROM public.orders WHERE id SOUNDS LIKE 'x' /*M! AND id > 0 */");
    }

    @Test
    void textTokenizationRespectsDialectBracketsAndStringEscapes() throws DBException {
        ScopeFixture fixture = new ScopeFixture();
        Assertions.assertDoesNotThrow(() -> fixture.verifyText(new ExecuteDQLFunction(), "SELECT ARRAY[1,2] FROM public.orders"));
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT ARRAY[private.read_payroll()] FROM public.orders");
        Mockito.when(fixture.dataSource.getSQLDialect()).thenReturn(new BasicSQLDialect() {
            @Override
            public char getStringEscapeCharacter() {
                return '\\';
            }
        });
        Assertions.assertDoesNotThrow(() -> fixture.verifyText(new ExecuteDQLFunction(), "SELECT 'it\\'s a string' FROM public.orders"));
        Mockito.when(fixture.dataSource.getSQLDialect()).thenReturn(new BasicSQLDialect() {
            @NotNull
            @Override
            public String[][] getIdentifierQuoteStrings() {
                return new String[][] {{"[", "]"}};
            }
        });
        Assertions.assertDoesNotThrow(() -> fixture.verifyText(new ExecuteDQLFunction(), "SELECT [private.read_payroll()] FROM [public].[orders]"));
        fixture.assertTextRestricted(new ExecuteDQLFunction(), "SELECT * FROM [private].[orders]");
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
            Mockito.when(dataSource.getSQLDialect()).thenReturn(new BasicSQLDialect() {
                @Override
                public DBPIdentifierCase storesUnquotedCase() {
                    return DBPIdentifierCase.LOWER;
                }
            });
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
            DBSSchema schema = Mockito.mock(DBSSchema.class,
                Mockito.withSettings().extraInterfaces(DBSProcedureContainer.class, DBSEntityContainer.class));
            Mockito.when(schema.getName()).thenReturn(name);
            Mockito.when(schema.getDataSource()).thenReturn(dataSource);
            Mockito.when(schema.getParentObject()).thenReturn(dataSource);
            Mockito.doReturn(DBSEntity.class).when(schema).getPrimaryChildType(Mockito.any());
            Map<String, DBSEntity> entities = Map.of(
                "orders", entity(schema, "orders"), "source", entity(schema, "source"), "report", entity(schema, "report"));
            Mockito.when(schema.getChild(Mockito.any(), Mockito.anyString())).thenAnswer(invocation -> {
                String child = invocation.getArgument(1);
                return entities.get(child.toLowerCase(Locale.ROOT));
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

        @NotNull
        private DBSEntity entity(@NotNull DBSSchema schema, @NotNull String name) {
            DBSEntity entity = Mockito.mock(DBSEntity.class);
            Mockito.when(entity.getName()).thenReturn(name);
            Mockito.when(entity.getDataSource()).thenReturn(dataSource);
            Mockito.when(entity.getParentObject()).thenReturn(schema);
            return entity;
        }

        private void enableExecutableComments() {
            Mockito.when(dataSource.getSQLDialect()).thenReturn(new BasicSQLDialect() {
                @Override
                public char getStringEscapeCharacter() {
                    return '\\';
                }

                @NotNull
                @Override
                public DBPIdentifierCase storesUnquotedCase() {
                    return DBPIdentifierCase.LOWER;
                }

                @NotNull
                @Override
                public List<String> getExecutableCommentPrefixes() {
                    return List.of("/*!", "/*M!");
                }

                @NotNull
                @Override
                public Boolean isExecutableCommentActive(@NotNull String prefix, int version) {
                    return version <= 100100;
                }

                @NotNull
                @Override
                public String[] getSingleLineComments() {
                    return new String[]{"-- ", "--\t", "#"};
                }
            });
        }

        private void setSqlMode(@NotNull String mode) throws DBException {
            DBCSession session = Mockito.mock(DBCSession.class);
            DBCStatement statement = Mockito.mock(DBCStatement.class);
            DBCResultSet resultSet = Mockito.mock(DBCResultSet.class);
            Mockito.when(context.getMonitor()).thenReturn(Mockito.mock(DBRProgressMonitor.class));
            Mockito.when(executionContext.openSession(Mockito.any(), Mockito.eq(DBCExecutionPurpose.UTIL), Mockito.anyString()))
                .thenReturn(session);
            Mockito.when(session.prepareStatement(Mockito.eq(DBCStatementType.QUERY), Mockito.anyString(),
                Mockito.eq(false), Mockito.eq(false), Mockito.eq(false))).thenReturn(statement);
            Mockito.when(statement.executeStatement()).thenReturn(true);
            Mockito.when(statement.openResultSet()).thenReturn(resultSet);
            Mockito.when(resultSet.nextRow()).thenReturn(true);
            Mockito.when(resultSet.getAttributeValue(0)).thenReturn(mode);
        }

        private void verify(@NotNull ExecuteSQLFunction function, @NotNull String sql) throws DBException {
            function.verifyQuery(context, executionContext, new SQLQuery(dataSource, sql));
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
