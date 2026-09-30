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
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
