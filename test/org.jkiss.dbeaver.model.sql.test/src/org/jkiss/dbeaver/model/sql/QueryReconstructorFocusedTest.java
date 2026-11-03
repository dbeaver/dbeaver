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
import org.jkiss.dbeaver.model.sql.parser.QueryReconstructor;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * A small, explicit set of token-diff and comment-recovery scenarios.
 */
public class QueryReconstructorFocusedTest extends DBeaverUnitTest {

    @Test
    public void unchangedQueryRetainsInternalComments() throws Exception {
        String sql = "SELECT\n-- column\nid\nFROM\n-- source\norders";

        Assertions.assertEquals(sql, reconstruct(sql, sql));
    }

    @Test
    public void queryWithoutCommentsUsesNewText() throws Exception {
        String newSql = "SELECT id, name FROM orders";

        Assertions.assertEquals(newSql, reconstruct("SELECT id FROM orders", newSql));
    }

    @Test
    public void addingColumnKeepsCommentOnExistingColumn() throws Exception {
        assertReconstruction(
            "SELECT\n-- id comment\nid\nFROM orders",
            "SELECT\nid,\nname\nFROM orders",
            "-- id comment"
        );
    }

    @Test
    public void removingColumnKeepsCommentOnRemainingColumn() throws Exception {
        assertReconstruction(
            "SELECT\n-- id comment\nid,\nold_col\nFROM orders",
            "SELECT\nid\nFROM orders",
            "-- id comment"
        );
    }

    @Test
    public void replacingColumnKeepsCommentOnRemainingColumn() throws Exception {
        assertReconstruction(
            "SELECT\n-- id comment\nid,\nold_col\nFROM orders",
            "SELECT\nid,\nnew_col\nFROM orders",
            "-- id comment"
        );
    }

    @Test
    public void addingJoinKeepsCommentOnExistingJoin() throws Exception {
        assertReconstruction(
            "SELECT id FROM orders\n-- customer join\nLEFT JOIN customers ON orders.customer_id = customers.id",
            "SELECT id FROM orders\nLEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "INNER JOIN invoices ON orders.id = invoices.order_id",
            "-- customer join"
        );
    }

    @Test
    public void removingJoinKeepsCommentOnRemainingJoin() throws Exception {
        assertReconstruction(
            "SELECT id FROM orders\n-- customer join\nLEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "RIGHT JOIN obsolete ON orders.id = obsolete.order_id",
            "SELECT id FROM orders\nLEFT JOIN customers ON orders.customer_id = customers.id",
            "-- customer join"
        );
    }

    @Test
    public void combinedColumnAndJoinChangesKeepCommentsOnUnchangedFragments() throws Exception {
        assertReconstruction(
            "SELECT\n-- id comment\nid, old_col\nFROM orders\n-- customer join\n" +
                "LEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "RIGHT JOIN obsolete ON orders.id = obsolete.order_id",
            "SELECT id, new_col FROM orders\nLEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "INNER JOIN invoices ON orders.id = invoices.order_id",
            "-- id comment", "-- customer join"
        );
    }

    @Test
    public void aliasedQualifiedReferencesKeepTheirComments() throws Exception {
        assertReconstruction(
            "SELECT\n-- customer name\nc.name\nFROM sales.orders AS o\n" +
                "LEFT JOIN sales.customers AS c ON o.customer_id = c.id",
            "SELECT c.name, c.city FROM sales.orders AS o\n" +
                "LEFT JOIN sales.customers AS c ON o.customer_id = c.id",
            "-- customer name"
        );
    }

    @Test
    public void insertingFirstColumnRetainsInternalComment() throws Exception {
        assertReconstruction(
            "SELECT\n-- name comment\nname, id\nFROM orders",
            "SELECT new_col, name, id FROM orders",
            "-- name comment"
        );
    }

    @Test
    public void insertingColumnBetweenTwoCommentedColumnsKeepsBothComments() throws Exception {
        String actual = assertReconstruction(
            "SELECT\n-- first column\nid,\n-- last column\nname\nFROM orders",
            "SELECT id, new_col, name FROM orders",
            "-- first column", "-- last column"
        );
        assertInOrder(actual, "-- first column", "id,", "-- last column", "name");
    }

    @Test
    public void removingMiddleColumnKeepsCommentsOnBothNeighbors() throws Exception {
        assertReconstruction(
            "SELECT\n-- first column\nid, old_col,\n-- last column\nname\nFROM orders",
            "SELECT id, name FROM orders",
            "-- first column", "-- last column"
        );
    }

    @Test
    public void insertingJoinBetweenTwoCommentedJoinsKeepsBothComments() throws Exception {
        String actual = assertReconstruction(
            "SELECT orders.id FROM orders\n-- customer join\n" +
                "LEFT JOIN customers ON orders.customer_id = customers.id\n-- payment join\n" +
                "LEFT JOIN payments ON orders.id = payments.order_id",
            "SELECT orders.id FROM orders\nLEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "INNER JOIN invoices ON orders.id = invoices.order_id\n" +
                "LEFT JOIN payments ON orders.id = payments.order_id",
            "-- customer join", "-- payment join"
        );
        assertInOrder(actual, "-- customer join", "LEFT JOIN customers", "-- payment join", "LEFT JOIN payments");
    }

    @Test
    public void removingMiddleJoinKeepsCommentOnFollowingJoin() throws Exception {
        String actual = assertReconstruction(
            "SELECT orders.id FROM orders\nLEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "INNER JOIN obsolete ON orders.id = obsolete.order_id\n-- payment join\n" +
                "LEFT JOIN payments ON orders.id = payments.order_id",
            "SELECT orders.id FROM orders\nLEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "LEFT JOIN payments ON orders.id = payments.order_id",
            "-- payment join"
        );
        assertInOrder(actual, "LEFT JOIN customers", "-- payment join", "LEFT JOIN payments");
    }

    @Test
    public void replacingColumnNextToInlineCommentPreservesNextLine() throws Exception {
        String actual = assertReconstruction(
            "SELECT id, old_col\nFROM orders -- source comment\nWHERE id > 0",
            "SELECT id, new_col\nFROM orders\nWHERE id > 0",
            "-- source comment"
        );
        assertInOrder(actual, "FROM orders", "-- source comment", "WHERE id > 0");
    }

    @Test
    public void combinedEditsPreserveInternalBlockComments() throws Exception {
        assertReconstruction(
            "SELECT /* column comment */ id, old_col FROM orders\n" +
                "LEFT JOIN /* join comment */ customers ON orders.customer_id = customers.id",
            "SELECT id, new_col FROM orders\n" +
                "LEFT JOIN customers ON orders.customer_id = customers.id\n" +
                "INNER JOIN invoices ON orders.id = invoices.order_id",
            "/* column comment */", "/* join comment */"
        );
    }

    @Test
    public void adjacentCommentsAreBothPreservedAcrossAnInsertion() throws Exception {
        String actual = assertReconstruction(
            "SELECT id, /* first */ /* second */ name FROM orders",
            "SELECT id, name, new_col FROM orders",
            "/* first */", "/* second */"
        );
        assertInOrder(actual, "/* first */", "/* second */", "name");
    }

    @Test
    public void repeatedIdentifiersStillProduceTheRequestedSql() throws Exception {
        assertReconstruction(
            "SELECT id, /* existing column */ name FROM orders JOIN customers ON orders.id = customers.id",
            "SELECT id, name, city FROM orders JOIN customers ON orders.id = customers.id",
            "/* existing column */"
        );
    }

    @Test
    public void formattingOnlyChangeKeepsOriginalInternalComment() throws Exception {
        assertReconstruction(
            "SELECT id, /* keep */ name FROM orders",
            "SELECT\n  id,\n  name\nFROM orders",
            "/* keep */"
        );
    }

    @Test
    public void leadingOnlyCommentSurvivesSqlChange() throws Exception {
        Assertions.assertEquals(
            "/* leading */\nSELECT id, name FROM orders",
            reconstruct("/* leading */\nSELECT id FROM orders", "SELECT id, name FROM orders")
        );
    }

    @Test
    public void reorderingColumnsKeepsCommentOnUnchangedClause() throws Exception {
        assertReconstruction(
            "SELECT id, name FROM /* source comment */ orders",
            "SELECT name, id FROM orders",
            "/* source comment */"
        );
    }

    @NotNull
    private static String assertReconstruction(
        @NotNull String originalSql,
        @NotNull String newSql,
        @NotNull String... retainedComments
    ) throws Exception {
        String actual = reconstruct(originalSql, newSql);

        // Compare SQL tokens rather than whitespace: reconstruction may retain original formatting.
        Assertions.assertArrayEquals(
            tokenImages(newSql),
            tokenImages(SQLUtils.collectComments(
                actual,
                BasicSQLDialect.INSTANCE.getMultiLineComments(),
                BasicSQLDialect.INSTANCE.getSingleLineComments()
            ).cleanSqlText()),
            actual
        );
        for (String comment : retainedComments) {
            Assertions.assertEquals(1, actual.split(java.util.regex.Pattern.quote(comment), -1).length - 1, actual);
        }
        return actual;
    }

    @NotNull
    private static String reconstruct(@NotNull String originalSql, @NotNull String newSql) throws Exception {
        return new QueryReconstructor(BasicSQLDialect.INSTANCE, originalSql).reconstructFromOriginalFragments(newSql);
    }

    @NotNull
    private static String[] tokenImages(@NotNull String sql) throws Exception {
        return Arrays.stream(SQLSemanticProcessor.parseSqlTextForTokens(BasicSQLDialect.INSTANCE, sql))
            .map(token -> token.image)
            .toArray(String[]::new);
    }

    private static void assertInOrder(@NotNull String actual, @NotNull String... fragments) {
        int offset = 0;
        for (String fragment : fragments) {
            int index = actual.indexOf(fragment, offset);
            Assertions.assertTrue(index >= 0, "Expected " + fragment + " after offset " + offset + " in " + actual);
            offset = index + fragment.length();
        }
    }
}
