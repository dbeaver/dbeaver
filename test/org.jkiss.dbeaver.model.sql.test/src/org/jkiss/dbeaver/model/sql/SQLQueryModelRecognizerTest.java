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
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolOrigin;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableClause;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
