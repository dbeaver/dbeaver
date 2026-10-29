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

import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractSQLDialectTest extends DBeaverUnitTest {

    private static final SQLDialect dialect = BasicSQLDialect.INSTANCE;

    @Test
    public void getQuotedStringPlainValueTest() {
        Assertions.assertEquals("'hello'", dialect.getQuotedString("hello"));
    }

    @Test
    public void getQuotedStringEmbeddedQuotesTest() {
        Assertions.assertEquals("'it''s'", dialect.getQuotedString("it's"));
    }

    @Test
    public void getQuotedStringMultipleQuotedFragmentsTest() {
        Assertions.assertEquals(
            "'''03'',''04'',''05'',''10'',''11'''",
            dialect.getQuotedString("'03','04','05','10','11'")
        );
    }

    @Test
    public void getQuotedStringValueLooksLikeQuotedLiteralTest() {
        Assertions.assertEquals(
            "'''kkk''''pp'''",
            dialect.getQuotedString("'kkk''pp'")
        );
    }

    @Test
    public void getQuotedStringConsecutiveQuotesTest() {
        Assertions.assertEquals("'kkk''''''pp'", dialect.getQuotedString("kkk'''pp"));
        Assertions.assertEquals("'kkk''''pp'", dialect.getQuotedString("kkk''pp"));
    }

    @Test
    public void getQuotedStringNestedQuotedLiteralLookingValueTest() {
        Assertions.assertEquals(
            "'''kkk''''''''pp'''",
            dialect.getQuotedString("'kkk''''pp'")
        );
    }

    @Test
    public void isTransactionModifyingSelectTest() {
        Assertions.assertFalse(dialect.isTransactionModifyingQuery("SELECT * FROM Test"));
    }

    @Test
    public void isTransactionModifyingSelectWithCteTest() {
        Assertions.assertFalse(dialect.isTransactionModifyingQuery("WITH cte AS (SELECT 1) SELECT * FROM cte"));
        Assertions.assertFalse(dialect.isTransactionModifyingQuery("with cte as (select 1) select * from cte"));
        Assertions.assertFalse(dialect.isTransactionModifyingQuery(
            "WITH cte AS (SELECT 1) SELECT * FROM cte;"));
    }

    @Test
    public void isTransactionModifyingSelectWithSeveralCtesTest() {
        Assertions.assertFalse(dialect.isTransactionModifyingQuery(
            "WITH cte1 AS (SELECT 1), cte2 AS (SELECT 2) SELECT * FROM cte1, cte2"));
        Assertions.assertFalse(dialect.isTransactionModifyingQuery(
            "WITH RECURSIVE cte(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM cte) SELECT * FROM cte"));
    }

    @Test
    public void isTransactionModifyingSelectWithCteWithCommentsTest() {
        Assertions.assertFalse(dialect.isTransactionModifyingQuery(
            "-- comment\nWITH cte AS (SELECT 1) SELECT * FROM cte"));
        Assertions.assertFalse(dialect.isTransactionModifyingQuery(
            "/* comment */ WITH cte AS (SELECT 1) SELECT * FROM cte"));
    }

    @Test
    public void isTransactionModifyingSelectWithCteIgnoresCteKeywordsTest() {
        // Keywords of the CTE queries themselves must not be taken into account
        Assertions.assertFalse(dialect.isTransactionModifyingQuery(
            "WITH cte AS (SELECT 1 AS alias FROM (SELECT 1) AS src) SELECT alias AS select FROM cte"));
    }

    @Test
    public void isTransactionModifyingInsertWithCteTest() {
        Assertions.assertTrue(dialect.isTransactionModifyingQuery(
            "WITH cte AS (SELECT 1) INSERT INTO Test SELECT * FROM cte"));
    }

    @Test
    public void isTransactionModifyingUpdateWithCteTest() {
        Assertions.assertTrue(dialect.isTransactionModifyingQuery(
            "WITH cte AS (SELECT 1) UPDATE Test SET id = 1"));
    }

    @Test
    public void isTransactionModifyingDeleteWithCteTest() {
        Assertions.assertTrue(dialect.isTransactionModifyingQuery(
            "WITH cte AS (SELECT 1) DELETE FROM Test"));
    }

    @Test
    public void isTransactionModifyingInsertWithSeveralCtesTest() {
        Assertions.assertTrue(dialect.isTransactionModifyingQuery(
            "WITH cte1 AS (SELECT 1), cte2 AS (SELECT 2) UPDATE Test SET id = cte2.id"));
    }

    @Test
    public void isTransactionModifyingUnrecognizedCteTest() {
        // If the WITH clause cannot be recognized the query is treated as transaction modifying
        Assertions.assertTrue(dialect.isTransactionModifyingQuery("WITH"));
        Assertions.assertTrue(dialect.isTransactionModifyingQuery("WITH cte"));
    }
}
