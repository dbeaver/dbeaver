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

    private static void assertReconstruction(
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
}
