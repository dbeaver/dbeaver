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

import java.util.Collections;

public class PostgreExecutionPlanCommentTest extends DBeaverUnitTest {

    @Test
    public void trailingCommentAfterDelimiterYieldsSingleStatement() {
        //given
        String selection = "SELECT 1; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void trailingBlockCommentAfterDelimiterYieldsSingleStatement() {
        //given
        String selection = "SELECT 1; /* context */"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 /* context */", planSql); //$NON-NLS-1$
    }

    @Test
    public void trailingCommentAfterDelimiterWorksForTextExplain() {
        //given
        String selection = "SELECT 1; -- trailing comment"; //$NON-NLS-1$
        PostgreExecutionPlan plan = new PostgreExecutionPlan(
            true, false, selection, new DBCQueryPlannerConfiguration());

        //when
        String planSql = plan.getPlanQueryString();

        //then
        Assertions.assertEquals("EXPLAIN SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void terminalDelimiterWithoutCommentPreserved() {
        //given
        String selection = "SELECT 1;"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1;", planSql); //$NON-NLS-1$
    }

    @Test
    public void plainQueryUnchanged() {
        //given
        String selection = "SELECT 1"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1", planSql); //$NON-NLS-1$
    }

    @Test
    public void commentLikeTextInStringLiteralUntouched() {
        //given
        String selection = "SELECT '-- ; --'; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT '-- ; --' -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void twoRealStatementsWithTrailingCommentKeepInternalDelimiter() {
        //given
        String selection = "SELECT 1; SELECT 2; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1; SELECT 2 -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void semicolonInQuotedIdentifierUntouched() {
        //given
        String selection = "SELECT \"a;b\"; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals(
            "EXPLAIN (FORMAT XML) SELECT \"a;b\" -- trailing comment", //$NON-NLS-1$
            planSql);
    }

    @Test
    public void semicolonInDollarQuoteUntouched() {
        //given
        String selection = "SELECT $tag$a;b$tag$; -- comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT $tag$a;b$tag$ -- comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void dollarQuoteLikeSuffixInIdentifierDoesNotStartDollarQuote() {
        //given
        String selection = "SELECT foo$tag$; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT foo$tag$ -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void escapedQuoteInEscapeStringUntouched() {
        //given
        String selection = "SELECT E'a\\'b'; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT E'a\\'b' -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void ambiguousBackslashQuoteFailsClosed() {
        //given
        String selection = "SELECT 'a\\'; -- c'; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 'a\\'; -- c'; -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void originalQueryPreserved() {
        //given
        String selection = "SELECT 1; -- trailing comment"; //$NON-NLS-1$

        //when
        PostgreExecutionPlan plan = explainPlan(selection);

        //then
        Assertions.assertEquals(selection, plan.getQueryString());
    }

    @Test
    public void originalQueryPreservedForExternalPlanConstructor() {
        //given
        String selection = "SELECT 1; -- trailing comment"; //$NON-NLS-1$

        //when
        PostgreExecutionPlan plan = new PostgreExecutionPlan(selection, Collections.emptyList());

        //then
        Assertions.assertEquals(selection, plan.getQueryString());
    }

    private static PostgreExecutionPlan explainPlan(String query) {
        return new PostgreExecutionPlan(false, false, query, new DBCQueryPlannerConfiguration());
    }

    private static String explainXml(String query) {
        return explainPlan(query).getPlanQueryString();
    }
}
