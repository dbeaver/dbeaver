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
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreExecutionPlanCommentTest extends DBeaverUnitTest {

    @Test
    public void trailingCommentAfterDelimiter_yieldsSingleStatement() {
        //given
        String selection = "SELECT 1; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void trailingBlockCommentAfterDelimiter_yieldsSingleStatement() {
        //given
        String selection = "SELECT 1; /* context */"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 /* context */", planSql); //$NON-NLS-1$
    }

    @Test
    public void plainQuery_unchanged() {
        //given
        String selection = "SELECT 1"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1", planSql); //$NON-NLS-1$
    }

    @Test
    public void leadingComment_preservedAsSingleStatement() {
        //given
        String selection = "-- context\nSELECT 1"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertFalse(planSql.contains(";"), //$NON-NLS-1$
            "Explain input must be a single statement, got: " + planSql);
    }

    @Test
    public void blockComment_preservedAsSingleStatement() {
        //given
        String selection = "/* context */ SELECT 1"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertFalse(planSql.contains(";"), //$NON-NLS-1$
            "Explain input must be a single statement, got: " + planSql);
    }

    @Test
    public void commentLikeTextInStringLiteral_untouched() {
        //given
        String selection = "SELECT '-- not a comment' AS value"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertTrue(planSql.contains("'-- not a comment'"), //$NON-NLS-1$
            "String literal must stay intact, got: " + planSql);
    }

    @Test
    public void twoRealStatements_notSilentlyMerged() {
        //given
        String selection = "SELECT 1; SELECT 2;"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertTrue(planSql.contains("SELECT 1") && planSql.contains("SELECT 2"), //$NON-NLS-1$ //$NON-NLS-2$
            "Two statements must never be treated as one, got: " + planSql);
    }

    @Test
    public void semicolonInDollarQuote_untouched() {
        //given
        String selection = "SELECT $tag$a;b$tag$; -- comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT $tag$a;b$tag$ -- comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void escapedQuoteInEscapeString_untouched() {
        //given
        String selection = "SELECT E'a\\'; -- c'"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT E'a\\'; -- c'", planSql); //$NON-NLS-1$
    }

    private static String explainXml(String query) {
        return new PostgreExecutionPlan(false, false, query, new DBCQueryPlannerConfiguration()).getPlanQueryString();
    }
}
