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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryConnectionDummyContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableClause;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryTableCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsLocalTableModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.*;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSTypeDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx2;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.util.*;

public class SQLQueryModelRecognizerTest extends DBeaverUnitTest {

    private static final String STMT_SELECT =
        "SELECT t.id, t.amount FROM test_table t JOIN parent_table p ON p.id = t.parent_id " +
            "WHERE t.amount > 0 GROUP BY t.id, t.amount HAVING COUNT(*) > 0 ORDER BY t.id";

    private static final String STMT_RECURSIVE_CTE =
        "WITH RECURSIVE hierarchy (id, parent_id, depth, total_amount) AS (" +
            "  SELECT id, parent_id, 0, amount FROM test_table WHERE parent_id IS NULL " +
            "  UNION ALL " +
            "  SELECT child.id, child.parent_id, parent.depth + 1, parent.total_amount + child.amount " +
            "  FROM test_table child JOIN hierarchy parent ON child.parent_id = parent.id " +
            "  WHERE parent.depth < 10" +
            "), filtered AS (" +
            "  SELECT id, parent_id, depth, total_amount FROM hierarchy WHERE total_amount > 100" +
            ") " +
            "SELECT f.id, f.depth, f.total_amount FROM filtered f " +
            "JOIN parent_table p ON p.id = f.parent_id WHERE f.depth > 0 ORDER BY f.depth, f.id";

    private static final String STMT_INSERT = "INSERT INTO test_table (id, amount) VALUES (1, 10), (2, 20)";

    private static final String STMT_UPDATE =
        "UPDATE test_table SET amount = amount + 1 WHERE id IN (SELECT id FROM parent_table)";

    private static final String STMT_DELETE = "DELETE FROM test_table WHERE id = 1";

    private static final String STMT_CREATE_TABLE =
        "CREATE TABLE test_table (id INTEGER PRIMARY KEY, parent_id INTEGER, " +
            "CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent_table (id), CHECK (id > 0))";

    private static final String STMT_ALTER_TABLE = "ALTER TABLE test_table ADD COLUMN amount INTEGER DEFAULT 0";

    private static final String STMT_DROP_TABLE = "DROP TABLE IF EXISTS test_table";

    private static final String STMT_CALL = "CALL test_proc(1, 'value')";

    private static final String STMT_CREATE_VIEW = "CREATE VIEW test_view AS SELECT id FROM test_table";

    private static final String STMT_CREATE_SCHEMA = "CREATE SCHEMA test_schema";

    private static final String STMT_DROP_SCHEMA = "DROP SCHEMA test_schema CASCADE";

    private static final String STMT_DECLARE_AND_SELECT = """
        DECLARE @start_date DATE = DATEADD(MONTH, -1, CURRENT_TIMESTAMP)
        DECLARE @end_date DATE = CURRENT_TIMESTAMP
        SELECT
        e.first_name AS FirstName
        , e.last_name AS LastName
        , e.hire_date AS HireDate
        FROM employee_table e
        WHERE e.hire_date BETWEEN @start_date AND @end_date
        """;

    @Test
    public void recognizesConsecutiveDeclarationsAndSelectAsOneQuery() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope(STMT_DECLARE_AND_SELECT);

        Assertions.assertEquals(2, scope.getVariableClauses().size());
        Assertions.assertEquals(
            List.of("@start_date", "@end_date"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
        Assertions.assertEquals(
            2, scope.getVariableClauses().stream()
                .map(SQLQueryVariableClause::getValueExpression).filter(value -> value != null).count()
        );
        Assertions.assertNotNull(scope.getBody());
        Assertions.assertNotNull(scope.getResultingVariables());
    }

    @Test
    public void recognizesStandaloneVariableStatements() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope(
            "DECLARE @first INT = 1, @second VARCHAR(20) DEFAULT 'x'"
        );

        Assertions.assertNull(scope.getBody());
        Assertions.assertEquals(
            List.of("@first", "@second"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
        Assertions.assertEquals(
            List.of(
                SQLQueryVariableInfo.OperationKind.DECLARATION,
                SQLQueryVariableInfo.OperationKind.DECLARATION
            ),
            scope.getVariableClauses().stream().map(SQLQueryVariableClause::getKind).toList()
        );
        Assertions.assertEquals(
            2, scope.getVariableClauses().stream()
                .map(SQLQueryVariableClause::getValueExpression).filter(value -> value != null).count()
        );

        SQLQueryVariableStatementModel assignment = recognizeVariableScope("SET @first = 2");
        Assertions.assertEquals(
            List.of(SQLQueryVariableInfo.OperationKind.ASSIGNMENT),
            assignment.getVariableClauses().stream().map(SQLQueryVariableClause::getKind).toList()
        );
    }

    @Test
    public void variableAssignmentsDoNotStealExistingSetStatements() {
        for (String sql : List.of("SET TRANSACTION READ ONLY", "SET SCHEMA test_schema")) {
            SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
            Assertions.assertNotNull(model);
            STMTreeNode queryNode = model.getSyntaxNode().findFirstNonErrorChild();
            Assertions.assertNotNull(queryNode);
            Assertions.assertEquals(STMKnownRuleNames.sqlQueryBody, queryNode.getNodeName());
        }
    }

    @Test
    public void variableStatementRecognitionSkipsOtherStatements() {
        for (String sql : List.of(
            "SET TRANSACTION READ ONLY",
            "SET SCHEMA test_schema",
            "UPDATE test_table SET amount = 1"
        )) {
            Assertions.assertNull(SQLQueryModelRecognizer.recognizeVariableStatement(
                createContext(new VoidProgressMonitor()), sql, SQLQueryVariablesSubset.EMPTY));
        }

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeVariableStatement(
            createContext(new VoidProgressMonitor()), "SET @value = 1", SQLQueryVariablesSubset.EMPTY);
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryVariableStatementModel.class, model.getQueryModel());
    }

    @Test
    public void variableStatementRecognitionSkipsRegularQueryBody() {
        SQLQueryVariableStatementModel regularModel = recognizeVariableScope(STMT_DECLARE_AND_SELECT);
        SQLQueryModel trackingModel = SQLQueryModelRecognizer.recognizeVariableStatement(
            createContext(new VoidProgressMonitor()),
            STMT_DECLARE_AND_SELECT,
            SQLQueryVariablesSubset.EMPTY
        );
        Assertions.assertNotNull(trackingModel);
        SQLQueryVariableStatementModel trackingStatement = Assertions.assertInstanceOf(
            SQLQueryVariableStatementModel.class,
            trackingModel.getQueryModel()
        );

        Assertions.assertNotNull(regularModel.getBody());
        Assertions.assertNull(trackingStatement.getBody());
        SQLQueryVariablesSubset regularVariables = Objects.requireNonNull(regularModel.getResultingVariables());
        SQLQueryVariablesSubset trackingVariables = Objects.requireNonNull(trackingStatement.getResultingVariables());
        Assertions.assertEquals(
            regularVariables.getVariables().stream().map(SQLQueryVariableInfo::rawName).toList(),
            trackingVariables.getVariables().stream().map(SQLQueryVariableInfo::rawName).toList()
        );
        Assertions.assertEquals(
            regularVariables.getVariables().stream().map(variable -> variable.type().getDisplayName()).toList(),
            trackingVariables.getVariables().stream().map(variable -> variable.type().getDisplayName()).toList()
        );
    }

    @Test
    public void resolvesVisibleVariableReferences() {
        SQLQueryVariableInfo definition = new SQLQueryVariableInfo(
            "@value",
            "value",
            SQLQueryExprType.forExplicitTypeRef("INT"),
            SQLScriptVariableScope.BATCH,
            SQLQueryVariableInfo.OperationKind.DECLARATION,
            0
        );

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "SELECT @VALUE",
            SQLQueryVariablesSubset.makeSnapshot(
                SQLScriptVariableScope.BATCH,
                Map.of(definition.canonicalName(), definition)
            )
        );

        Assertions.assertNotNull(model);
        SQLQuerySymbolEntry variable = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("@VALUE"))
            .findFirst()
            .orElseThrow();
        Assertions.assertSame(definition, variable.getDefinition());
        Assertions.assertInstanceOf(SQLQuerySymbolOrigin.ScriptVariableRef.class, variable.getOrigin());
    }

    @Test
    public void keepsClientVariableOriginsClientSpecific() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "SELECT ${value}, :parameter"
        );

        Assertions.assertNotNull(model);
        List<SQLQuerySymbolEntry> clientVariables = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("${value}") || symbol.getRawName().equals(":parameter"))
            .toList();
        Assertions.assertEquals(2, clientVariables.size());
        Assertions.assertTrue(clientVariables.stream()
            .noneMatch(symbol -> symbol.getOrigin() instanceof SQLQuerySymbolOrigin.ScriptVariableRef));
    }

    @Test
    public void resolvesVisibleUnprefixedVariableReferences() {
        SQLQueryVariableInfo definition = new SQLQueryVariableInfo(
            "current_value",
            "current_value",
            SQLQueryExprType.forExplicitTypeRef("INT"),
            SQLScriptVariableScope.SESSION,
            SQLQueryVariableInfo.OperationKind.DECLARATION,
            0
        );
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context,
            "SELECT current_value",
            SQLQueryVariablesSubset.makeSnapshot(
                SQLScriptVariableScope.SESSION,
                Map.of(definition.canonicalName(), definition)
            )
        );

        Assertions.assertNotNull(model);
        Assertions.assertTrue(context.getProblems().isEmpty());
        SQLQuerySymbolEntry variable = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("current_value"))
            .findFirst()
            .orElseThrow();
        Assertions.assertSame(definition, variable.getDefinition());
        Assertions.assertFalse(variable.getOrigin() instanceof SQLQuerySymbolOrigin.ScriptVariableRef);
    }

    @Test
    public void recognizesUnprefixedSessionVariables() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope(
            "DECLARE OR REPLACE VARIABLE current_value INT DEFAULT 1");

        Assertions.assertEquals(
            List.of("current_value"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
        Assertions.assertEquals(1, scope.getVariableClauses().size());

        SQLQueryVariableStatementModel assignment = recognizeVariableScope("SET VARIABLE current_value = 2");
        Assertions.assertEquals(
            List.of("current_value"),
            assignment.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
    }

    @Test
    public void recognizesDatabricksVarAssignment() {
        SQLQueryVariableStatementModel scope = recognizeVariableScope("SET VAR current_value = 2");

        Assertions.assertEquals(
            List.of("current_value"),
            scope.getVariableClauses().stream().map(c -> c.getVariableName().getRawName()).toList()
        );
    }

    @Test
    public void incompleteVariableDeclarationDoesNotFailRecognition() {
        Assertions.assertDoesNotThrow(() ->
            SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), "DECLARE \"")
        );
    }

    @Test
    public void selectCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_SELECT);
    }

    @Test
    public void recursiveCteCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_RECURSIVE_CTE);
    }

    @Test
    public void insertCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_INSERT);
    }

    @Test
    public void updateCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_UPDATE);
    }

    @Test
    public void deleteCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_DELETE);
    }

    @Test
    public void createTableCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_CREATE_TABLE);
    }

    @Test
    public void alterTableCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_ALTER_TABLE);
    }

    @Test
    public void dropTableCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_DROP_TABLE);
    }

    @Test
    public void callCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint(STMT_CALL);
    }

    @Test
    public void unsupportedCreateViewCancellationDoesNotReturnPartialModel() {
        assertUnsupportedStatementCancellationAtEveryCheckpoint(STMT_CREATE_VIEW);
    }

    @Test
    public void unsupportedCreateSchemaCancellationDoesNotReturnPartialModel() {
        assertUnsupportedStatementCancellationAtEveryCheckpoint(STMT_CREATE_SCHEMA);
    }

    @Test
    public void unsupportedDropSchemaCancellationDoesNotReturnPartialModel() {
        assertUnsupportedStatementCancellationAtEveryCheckpoint(STMT_DROP_SCHEMA);
    }

    @Test
    public void resolvesSqlWithinProceduralBlock() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN SELECT id FROM tab; END"
        );
        Assertions.assertNotNull(model);
        SQLQueryRoutineStatementModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("tab")));
    }

    @Test
    public void declaredLocalTableSuppliesColumnsToNestedSql() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN DECLARE @items TABLE (id int); SELECT id FROM @items; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(context.getProblems().stream().noneMatch(p -> p.getMessage().contains("@items")));
    }

    @Test
    public void functionParameterIsVisibleInItsSqlBody() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE FUNCTION f(x int) RETURNS int AS $body$ BEGIN SELECT x; END; $body$ LANGUAGE sql"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(model.getAllSymbols().stream().filter(s -> s.getRawName().equals("x"))
            .anyMatch(s -> s.getDefinition() != null));
    }

    @Test
    public void parenthesizedTableFunctionBodyRetainsNestedSqlAndParameterContext() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "CREATE TABLE FUNCTION dataset.qa_table_fn(x INT64) AS (SELECT x FROM rects3rects)"
        );
        Assertions.assertNotNull(model);
        SQLQueryRoutineCreateModel routine = Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, model.getQueryModel());
        SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        SQLQueryEmbeddedStatement nestedSql = Assertions.assertInstanceOf(
            SQLQueryEmbeddedStatement.class, body.getStatements().getFirst()
        );
        Assertions.assertNotNull(nestedSql.getSql());
        Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("rects3rects")));
        List<SQLQuerySymbolEntry> parameter = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(2, parameter.size());
        Assertions.assertSame(parameter.getFirst(), parameter.getLast().getDefinition());
    }

    @Test
    public void proceduralCancellationDoesNotReturnPartialModel() {
        assertCancellationAtEveryCheckpoint("CREATE PROCEDURE p AS BEGIN DECLARE v int; SELECT v; END");
    }

    @Test
    public void triggerTransitionRowsAreVisibleInsideBody() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE TRIGGER tr AFTER UPDATE ON tab REFERENCING NEW AS n FOR EACH ROW " +
                "BEGIN SELECT n.id; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryTriggerCreateModel.class, model.getQueryModel());
        Assertions.assertTrue(model.getAllSymbols().stream().anyMatch(s -> s.getRawName().equals("n")));
    }

    @Test
    public void declaredCursorIsLinkedFromProceduralStatements() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS DECLARE c CURSOR FOR SELECT id FROM tab; " +
                "BEGIN OPEN c; FETCH NEXT FROM c INTO x; CLOSE c; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(model.getAllSymbols().stream().filter(s -> s.getRawName().equals("c"))
            .anyMatch(s -> s.getDefinition() != null));
    }

    @Test
    public void temporaryTableColumnsResolveWithinProcedure() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE scratch (id int); " +
                "INSERT INTO scratch (id) VALUES (1); SELECT id FROM scratch; DROP TABLE scratch; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(context.getProblems().stream().noneMatch(p -> p.getMessage().contains("scratch")));
    }

    @Test
    public void prefixedTemporaryTableColumnsResolveWithinProcedure() {
        SQLQueryRecognitionContext context = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            context, "CREATE PROCEDURE p AS BEGIN CREATE TABLE #scratch (id int); SELECT id FROM #scratch; END"
        );
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertTrue(context.getProblems().stream().noneMatch(p -> p.getMessage().contains("#scratch")));
    }

    @Test
    public void droppingMultipleTemporaryRelationsPreservesOtherRelations() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "SELECT 1"
        );
        Assertions.assertNotNull(model);
        STMTreeNode node = model.getSyntaxNode();
        SQLQueryRowsSourceContext original = new SQLQueryRowsSourceContext(new SQLQueryConnectionDummyContext(
            BasicSQLDialect.INSTANCE, SQLQueryVariablesSubset.EMPTY, Set.of(), Set.of()
        ));
        Assertions.assertSame(original, original.withoutTemporaryRelations(List.of()));
        Assertions.assertSame(original, original.withoutTemporaryRelations(List.of("missing")));

        SQLQueryRowsSourceContext current = original;
        for (String name : List.of("first", "second", "remaining")) {
            SQLQuerySymbolEntry declaration = new SQLQuerySymbolEntry(node, name, name, null);
            SQLQueryRowsLocalTableModel rows = new SQLQueryRowsLocalTableModel(node, declaration, List.of());
            current = current.withTemporaryRelation(new SQLQueryLocalVariableInfo(
                declaration, SQLDialect.LocalVariableKind.ROWSET, SQLQueryExprType.UNKNOWN, rows, false));
        }
        SQLQueryRowsSourceContext afterDrop = current.withoutTemporaryRelations(List.of("first", "missing", "first", "second"));
        Assertions.assertSame(afterDrop, afterDrop.withoutTemporaryRelations(List.of("missing", "second")));
        for (String name : List.of("first", "second")) {
            Assertions.assertNull(afterDrop.findDynamicRowsSource(new SQLQuerySymbolEntry(node, name, name, null)));
        }
        Assertions.assertNotNull(afterDrop.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "remaining", "remaining", null)));
        SQLQueryRowsSourceContext outer = current.withTemporaryRelationsFrom(afterDrop);
        Assertions.assertNull(outer.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "first", "first", null)));
        Assertions.assertNull(outer.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "second", "second", null)));
        Assertions.assertNotNull(outer.findDynamicRowsSource(new SQLQuerySymbolEntry(node, "remaining", "remaining", null)));
    }

    @Test
    public void dropTableUsesCollectedUnqualifiedNamesForTemporaryRelations() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TEMP TABLE first (id int); " +
            "CREATE TEMP TABLE second (id int); CREATE TEMP TABLE remaining (id int); " +
            "DROP TABLE first, second, missing, schema.remaining; " +
            "SELECT id FROM first; SELECT id FROM second; SELECT id FROM remaining; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        List<SQLQuerySymbolEntry> remaining = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("remaining"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(3, remaining.size());
        Assertions.assertSame(remaining.getFirst(), remaining.getLast().getDefinition());
        for (String name : List.of("first", "second")) {
            List<SQLQuerySymbolEntry> symbols = model.getAllSymbols().stream()
                .filter(s -> s.getRawName().equals(name))
                .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
            Assertions.assertEquals(3, symbols.size());
            Assertions.assertNotSame(symbols.getFirst(), symbols.getLast().getDefinition());
        }
    }

    @Test
    public void nestedBlockRetainsItsFinalTemporaryRelationSnapshot() {
        String sql = "CREATE PROCEDURE p AS BEGIN " +
            "CREATE TEMP TABLE removed (id int); CREATE TEMP TABLE replaced (id int); " +
            "CREATE TEMP TABLE retained (id int); " +
            "BEGIN DROP TABLE removed, replaced; CREATE TEMP TABLE replaced (id text); END; " +
            "SELECT id FROM removed; SELECT id FROM replaced; SELECT id FROM retained; END";
        SQLQueryRecognitionContext recognitionContext = createContext(new VoidProgressMonitor());
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(recognitionContext, sql);
        Assertions.assertNotNull(model);
        Assertions.assertTrue(recognitionContext.getProblems().stream().noneMatch(p ->
            p.getMessage().contains("replaced") || p.getMessage().contains("retained")), recognitionContext.getProblems().toString());
        int trailingSelect = sql.lastIndexOf("SELECT id FROM replaced");
        Assertions.assertTrue(recognitionContext.getProblems().stream().noneMatch(p ->
            p.getInterval().a >= trailingSelect), recognitionContext.getProblems().toString());

        List<SQLQuerySymbolEntry> removed = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("removed"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(3, removed.size());
        Assertions.assertNotSame(removed.getFirst(), removed.getLast().getDefinition());

        List<SQLQuerySymbolEntry> replaced = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("replaced"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(4, replaced.size());
        Assertions.assertSame(replaced.get(2), replaced.getLast().getDefinition());

        List<SQLQuerySymbolEntry> retained = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("retained"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(2, retained.size());
        Assertions.assertSame(retained.getFirst(), retained.getLast().getDefinition());
    }

    @Test
    public void nestedBlockVariableShadowsOuterDeclarationOnlyInsideItsBlock() {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()),
            "CREATE PROCEDURE p AS DECLARE x int; BEGIN " +
                "DECLARE x text; BEGIN SELECT x; END; SELECT x; END"
        );
        Assertions.assertNotNull(model);
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(4, names.size());
        Assertions.assertSame(names.get(1), names.get(2).getDefinition());
        Assertions.assertSame(names.get(0), names.get(3).getDefinition());
    }

    @Test
    public void programmaticalDefinitionsDistinguishBodyCapabilityFromBodyPresence() {
        SQLQueryModel incomplete = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "CREATE PROCEDURE p"
        );
        Assertions.assertNotNull(incomplete);
        SQLQueryProgrammaticalBodyStatementModel routine = Assertions.assertInstanceOf(
            SQLQueryProgrammaticalBodyStatementModel.class, incomplete.getQueryModel()
        );
        Assertions.assertNull(routine.getBody());
        Assertions.assertInstanceOf(SQLQueryRoutineCreateModel.class, routine);

        SQLQueryModel drop = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "DROP PROCEDURE p"
        );
        Assertions.assertNotNull(drop);
        Assertions.assertInstanceOf(SQLQueryRoutineDropModel.class, drop.getQueryModel());

        SQLQueryModel alter = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "ALTER FUNCTION p"
        );
        Assertions.assertNotNull(alter);
        Assertions.assertInstanceOf(SQLQueryRoutineAlterModel.class, alter.getQueryModel());
    }

    @Test
    public void assignmentInsideRoutineHasStatementModelAndResolvesLocalName() {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE x int; SET x = 1; SELECT x; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        Assertions.assertInstanceOf(SQLQueryAssignmentStatement.class, model.findNodeContaining(sql.indexOf("SET x") + 1));
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(3, names.size());
        Assertions.assertSame(names.get(0), names.get(1).getDefinition());
        Assertions.assertSame(names.get(0), names.get(2).getDefinition());
    }

    @Test
    public void anonymousBlockAndBodyFreeTriggerHaveDistinctModelFamilies() {
        SQLQueryModel block = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "BEGIN SELECT 1; END"
        );
        Assertions.assertNotNull(block);
        SQLQueryAnonymousBlockModel anonymous = Assertions.assertInstanceOf(SQLQueryAnonymousBlockModel.class, block.getQueryModel());
        Assertions.assertNotNull(anonymous.getBody());

        SQLQueryModel drop = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "DROP TRIGGER t ON tab"
        );
        Assertions.assertNotNull(drop);
        Assertions.assertInstanceOf(SQLQueryTriggerDropModel.class, drop.getQueryModel());

        SQLQueryModel alter = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "ALTER TRIGGER t"
        );
        Assertions.assertNotNull(alter);
        Assertions.assertInstanceOf(SQLQueryTriggerAlterModel.class, alter.getQueryModel());

        SQLQueryModel cursor = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new VoidProgressMonitor()), "OPEN c"
        );
        Assertions.assertNotNull(cursor);
        Assertions.assertInstanceOf(SQLQueryCursorOperationStatement.class, cursor.getQueryModel());
    }

    @Test
    public void branchesHaveIndependentLocalVariables() {
        String sql = "CREATE PROCEDURE p AS BEGIN IF 1 = 1 THEN DECLARE x int; SELECT x; " +
            "ELSE DECLARE x text; SELECT x; END IF; SELECT x; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        SQLQueryConditionalStatement branch = Assertions.assertInstanceOf(
            SQLQueryConditionalStatement.class, model.findNodeContaining(sql.indexOf("IF 1") + 1)
        );
        Assertions.assertEquals(2, branch.getBranches().size());
        List<SQLQuerySymbolEntry> names = model.getAllSymbols().stream()
            .filter(s -> s.getRawName().equals("x"))
            .sorted(Comparator.comparingInt(s -> s.getInterval().a)).toList();
        Assertions.assertEquals(5, names.size());
        Assertions.assertSame(names.get(0), names.get(1).getDefinition());
        Assertions.assertSame(names.get(2), names.get(3).getDefinition());
        Assertions.assertNotSame(names.get(0), names.get(4).getDefinition());
        Assertions.assertNotSame(names.get(2), names.get(4).getDefinition());
    }

    @Test
    public void databaseCompositeTypeResolvesForLocalVariableAndCast() throws Exception {
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        BasicSQLDialect dialect = BasicSQLDialect.INSTANCE;
        DBSTypeDescriptor fieldType = Mockito.mock(DBSTypeDescriptor.class);
        Mockito.when(fieldType.getKind()).thenReturn(DBSTypeDescriptor.Kind.SIMPLE);
        Mockito.when(fieldType.getTypeName()).thenReturn("integer");
        DBSTypeDescriptor compositeType = Mockito.mock(DBSTypeDescriptor.class);
        Mockito.when(compositeType.getKind()).thenReturn(DBSTypeDescriptor.Kind.COMPOSITE);
        Mockito.when(compositeType.getTypeName()).thenReturn("app.custom_type");
        Mockito.when(compositeType.findCompositeMember(monitor, "field")).thenReturn(fieldType);
        Mockito.when(compositeType.findCompositeMember(monitor, "FIELD")).thenReturn(fieldType);
        DBSDataType dataType = Mockito.mock(DBSDataType.class, Mockito.withSettings().extraInterfaces(DBSTypedObjectEx2.class));
        Mockito.when(dataType.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        Mockito.when(((DBSTypedObjectEx2) dataType).getTypeDescriptor(monitor)).thenReturn(compositeType);

        DBPDataSource source = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        DBPDataSourceContainer sourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(source.getSQLDialect()).thenReturn(dialect);
        Mockito.when(source.getContainer()).thenReturn(sourceContainer);
        Mockito.when(sourceContainer.isExtraMetadataReadEnabled()).thenReturn(true);
        Mockito.when(((DBSObjectContainer) source).getDataSource()).thenReturn(source);
        DBSCatalog catalog = Mockito.mock(DBSCatalog.class);
        DBSObjectContainer schema = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(catalog.getChild(monitor, "app")).thenReturn(schema);
        Mockito.when(schema.getChild(monitor, "custom_type")).thenReturn(dataType);
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(defaults.getDefaultCatalog()).thenReturn(catalog);
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(executionContext.getDataSource()).thenReturn(source);
        Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        syntax.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());

        SQLQueryRecognitionContext declarationContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        String declarationSql = "BEGIN DECLARE v app.custom_type; SELECT v.field; END";
        SQLQueryModel declarationModel = SQLQueryModelRecognizer.recognizeQuery(declarationContext, declarationSql);
        Assertions.assertNotNull(declarationModel);
        SQLQueryLocalVariableInfo local = declarationModel.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("v") && symbol.getDefinition() instanceof SQLQueryLocalVariableInfo)
            .map(symbol -> (SQLQueryLocalVariableInfo) symbol.getDefinition())
            .findFirst().orElseThrow();
        SQLQueryVariableDeclarationStatement declaration = Assertions.assertInstanceOf(
            SQLQueryVariableDeclarationStatement.class, declarationModel.findNodeContaining(declarationSql.indexOf("DECLARE") + 1));
        Assertions.assertSame(local.type(), declaration.getDeclarations().getFirst().getType());
        Assertions.assertEquals(DBPDataKind.STRUCT, local.type().getDataKind());
        Assertions.assertEquals("integer", local.type().findNamedMemberType(monitor, "field").getDisplayName());
        Assertions.assertTrue(declarationContext.getProblems().isEmpty(), declarationContext.getProblems().toString());

        SQLQueryRecognitionContext castContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel castModel = SQLQueryModelRecognizer.recognizeQuery(castContext, "SELECT 1::app.custom_type AS result");
        Assertions.assertNotNull(castModel);
        SQLQueryRowsSourceModel result = Assertions.assertInstanceOf(SQLQueryRowsSourceModel.class, castModel.getQueryModel());
        SQLQueryExprType castType = result.getRowsDataContext().getColumnsList().getFirst().type;
        Assertions.assertEquals(DBPDataKind.STRUCT, castType.getDataKind());
        Assertions.assertEquals("integer", castType.findNamedMemberType(monitor, "field").getDisplayName());
        Assertions.assertTrue(castContext.getProblems().isEmpty(), castContext.getProblems().toString());

        SQLQueryRecognitionContext parameterContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel parameterModel = SQLQueryModelRecognizer.recognizeQuery(parameterContext,
            "CREATE FUNCTION f(x app.custom_type) RETURNS int AS $body$ BEGIN SELECT x.field; END; $body$ LANGUAGE sql");
        Assertions.assertNotNull(parameterModel);
        SQLQueryLocalVariableInfo parameter = parameterModel.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equals("x") && symbol.getDefinition() instanceof SQLQueryLocalVariableInfo)
            .map(symbol -> (SQLQueryLocalVariableInfo) symbol.getDefinition())
            .findFirst().orElseThrow();
        Assertions.assertEquals(DBPDataKind.STRUCT, parameter.type().getDataKind());
        Assertions.assertTrue(parameterContext.getProblems().isEmpty(), parameterContext.getProblems().toString());

        SQLQueryRecognitionContext fallbackContext = new SQLQueryRecognitionContext(
            monitor, executionContext, true, false, syntax, dialect);
        SQLQueryModel fallbackModel = SQLQueryModelRecognizer.recognizeQuery(fallbackContext, "SELECT 1::missing_type AS result");
        Assertions.assertNotNull(fallbackModel);
        SQLQueryRowsSourceModel fallbackResult = Assertions.assertInstanceOf(SQLQueryRowsSourceModel.class, fallbackModel.getQueryModel());
        SQLQueryExprType fallback = fallbackResult.getRowsDataContext().getColumnsList().getFirst().type;
        Assertions.assertEquals(DBPDataKind.UNKNOWN, fallback.getDataKind());
        Assertions.assertEquals("missing_type", fallback.getDisplayName());
    }

    @Test
    public void regularAndTemporaryTableCreationHaveDifferentStatementModels() {
        String sql = "CREATE PROCEDURE p AS BEGIN CREATE TABLE permanent (id int); " +
            "CREATE TEMP TABLE scratch (id int); END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        SQLQueryRoutineStatementModel routine = Assertions.assertInstanceOf(SQLQueryRoutineStatementModel.class, model.getQueryModel());
        SQLQueryStatementSequence body = Assertions.assertInstanceOf(SQLQueryStatementSequence.class, routine.getBody());
        SQLQueryStatementBlock block = Assertions.assertInstanceOf(SQLQueryStatementBlock.class, body.getStatements().getFirst());
        SQLQueryStatementSequence statements = Assertions.assertInstanceOf(
            SQLQueryStatementSequence.class, block.getSections().getFirst()
        );
        SQLQueryEmbeddedStatement create = Assertions.assertInstanceOf(
            SQLQueryEmbeddedStatement.class, statements.getStatements().getFirst()
        );
        Assertions.assertInstanceOf(SQLQueryTableCreateModel.class, create.getSql());
        Assertions.assertInstanceOf(SQLQueryRelationDeclarationStatement.class, statements.getStatements().get(1));
    }

    @Test
    public void visitorDistinguishesProgrammaticalAndBodyStatementTypes() {
        String sql = "CREATE PROCEDURE p AS BEGIN DECLARE x int; SET x = 1; END";
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);

        @SuppressWarnings("unchecked")
        SQLQueryNodeModelVisitor<String, String> visitor = (SQLQueryNodeModelVisitor<String, String>) Proxy.newProxyInstance(
            SQLQueryNodeModelVisitor.class.getClassLoader(), new Class<?>[] { SQLQueryNodeModelVisitor.class },
            (proxy, method, args) -> method.getName()
        );
        Assertions.assertEquals("visitRoutineCreate", model.getQueryModel().apply(visitor, ""));
        Assertions.assertEquals("visitAssignmentStatement", model.findNodeContaining(sql.indexOf("SET x") + 1).apply(visitor, ""));
    }

    private static void assertCancellationAtEveryCheckpoint(@NotNull String sql) {
        CountingProgressMonitor completedMonitor = new CountingProgressMonitor(Integer.MAX_VALUE);
        SQLQueryModel completedModel = SQLQueryModelRecognizer.recognizeQuery(createContext(completedMonitor), sql);
        Assertions.assertTrue(completedModel != null && completedModel.getQueryModel() != null, sql);

        int cancellationPoint = 0;
        while (cancellationPoint < completedMonitor.getCheckCount()) {
            Assertions.assertNull(
                SQLQueryModelRecognizer.recognizeQuery(createContext(new CountingProgressMonitor(cancellationPoint)), sql),
                sql + " returned a model when cancelled at checkpoint " + cancellationPoint + " but should have been cancelled"
            );
            cancellationPoint++;
        }

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new CountingProgressMonitor(cancellationPoint)),
            sql
        );
        Assertions.assertTrue(
            model != null && model.getQueryModel() != null,
            sql + " didn't return a model when cancelled at checkpoint " + cancellationPoint + " but should have"
        );
    }

    private static void assertUnsupportedStatementCancellationAtEveryCheckpoint(@NotNull String sql) {
        CountingProgressMonitor completedMonitor = new CountingProgressMonitor(Integer.MAX_VALUE);
        SQLQueryModel completedModel = SQLQueryModelRecognizer.recognizeQuery(createContext(completedMonitor), sql);
        Assertions.assertTrue(completedModel != null && completedModel.getQueryModel() == null, sql);

        int cancellationPoint = 0;
        while (cancellationPoint < completedMonitor.getCheckCount()) {
            Assertions.assertNull(
                SQLQueryModelRecognizer.recognizeQuery(createContext(new CountingProgressMonitor(cancellationPoint)), sql),
                sql + " returned a model when cancelled at checkpoint " + cancellationPoint + " but should have been cancelled"
            );
            cancellationPoint++;
        }

        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(
            createContext(new CountingProgressMonitor(cancellationPoint)),
            sql
        );
        Assertions.assertTrue(
            model != null && model.getQueryModel() == null,
            sql + " didn't return a fallback model at checkpoint " + cancellationPoint + " but should have"
        );
    }

    @NotNull
    private static SQLQueryVariableStatementModel recognizeVariableScope(@NotNull String sql) {
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(createContext(new VoidProgressMonitor()), sql);
        Assertions.assertNotNull(model);
        return Assertions.assertInstanceOf(SQLQueryVariableStatementModel.class, model.getQueryModel());
    }

    @NotNull
    private static SQLQueryRecognitionContext createContext(@NotNull VoidProgressMonitor monitor) {
        SQLDialect dialect = BasicSQLDialect.INSTANCE;
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        return new SQLQueryRecognitionContext(monitor, null, false, false, syntaxManager, dialect);
    }

    private static class CountingProgressMonitor extends VoidProgressMonitor {
        private final int cancellationPoint;
        private int checkCount;

        private CountingProgressMonitor(int cancellationPoint) {
            this.cancellationPoint = cancellationPoint;
        }

        @Override
        public boolean isCanceled() {
            return this.checkCount++ >= this.cancellationPoint;
        }

        private int getCheckCount() {
            return this.checkCount;
        }
    }
}
