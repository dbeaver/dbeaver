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

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.postgresql.TestPreferenceStore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PostgreExecutionPlanCommentTest extends DBeaverUnitTest {

    @Test
    public void trailingCommentAfterDelimiterYieldsSingleStatement() {
        //given
        String selection = "SELECT 1; -- trailing comment"; //$NON-NLS-1$

        //when
        PostgreExecutionPlan plan = explainPlan(selection);
        String planSql = plan.getPlanQueryString();

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
        Assertions.assertEquals(selection, plan.getQueryString());
    }

    @Test
    public void leadingLineCommentPrecedesExplainPrefix() {
        //given
        String selection = "-- leading comment\nSELECT 1; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals(
            "-- leading comment\nEXPLAIN (FORMAT XML) SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void leadingLineCommentPrecedesTextExplainPrefix() {
        //given
        String selection = "-- leading comment\nSELECT 1; -- trailing comment"; //$NON-NLS-1$
        PostgreExecutionPlan plan = new PostgreExecutionPlan(
            true, false, selection, new DBCQueryPlannerConfiguration());

        //when
        String planSql = plan.getPlanQueryString();

        //then
        Assertions.assertEquals(
            "-- leading comment\nEXPLAIN SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
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
    public void nestedBlockCommentAfterDelimiterYieldsSingleStatement() {
        //given
        String selection = "SELECT 1; /* outer /* nested */ tail */"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 /* outer /* nested */ tail */", planSql); //$NON-NLS-1$
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
    public void repeatedTrailingDelimitersBeforeCommentAreRemoved() {
        //given
        String selection = "SELECT 1;; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void delimiterAfterCommentBeforeTrailingCommentIsRemoved() {
        //given
        String selection = "SELECT 1; /* comment */; -- trailing"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1 /* comment */ -- trailing", planSql); //$NON-NLS-1$
    }

    @Test
    public void multipleStatementsWithCommentKeepInternalDelimiter() {
        //given
        String selection = "SELECT 1; /* between */ SELECT 2; -- trailing"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals(
            "EXPLAIN (FORMAT XML) SELECT 1; /* between */ SELECT 2 -- trailing", planSql); //$NON-NLS-1$
    }

    @Test
    public void connectionDelimiterPreferencesDoNotTrimIdentifierAlias() {
        //given
        String selection = "SELECT 1 AS go -- trailing"; //$NON-NLS-1$
        TestPreferenceStore connectionStore = new TestPreferenceStore() {
            @Override
            public String getString(String name) {
                return ModelPreferences.SCRIPT_STATEMENT_DELIMITER.equals(name) ? ";" : ""; //$NON-NLS-1$ //$NON-NLS-2$
            }
        };
        DBPPreferenceStore platformStore = DBWorkbench.getPlatform().getPreferenceStore();
        boolean platformDelimiterWasDefault = platformStore.isDefault(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
        String platformDelimiter = platformStore.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
        platformStore.setValue(ModelPreferences.SCRIPT_STATEMENT_DELIMITER, "GO"); //$NON-NLS-1$

        try {
            //when
            String connectionPlanSql = explainPlan(selection)
                .getPlanQueryString(new PostgreDialect(), connectionStore);
            String platformPlanSql = explainPlan(selection)
                .getPlanQueryString(new PostgreDialect(), platformStore);

            //then
            Assertions.assertEquals(
                "EXPLAIN (FORMAT XML) SELECT 1 AS go -- trailing", connectionPlanSql); //$NON-NLS-1$
            Assertions.assertEquals(
                "EXPLAIN (FORMAT XML) SELECT 1 AS  -- trailing", platformPlanSql); //$NON-NLS-1$
        } finally {
            if (platformDelimiterWasDefault || platformDelimiter == null) {
                platformStore.setToDefault(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
            } else {
                platformStore.setValue(ModelPreferences.SCRIPT_STATEMENT_DELIMITER, platformDelimiter);
            }
        }
    }

    @Test
    public void customDelimiterEndingWithNativeDelimiterIsRemovedCompletely() {
        //given
        String selection = "SELECT 1 GO; -- trailing"; //$NON-NLS-1$
        TestPreferenceStore connectionStore = new TestPreferenceStore() {
            @Override
            public String getString(String name) {
                return ModelPreferences.SCRIPT_STATEMENT_DELIMITER.equals(name) ? "GO;" : ""; //$NON-NLS-1$ //$NON-NLS-2$
            }
        };

        //when
        String planSql = explainPlan(selection).getPlanQueryString(new PostgreDialect(), connectionStore);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1  -- trailing", planSql); //$NON-NLS-1$
    }

    @Test
    public void delimiterSuffixAfterDollarIdentifierUntouched() {
        //given
        String selection = "SELECT foo$GO -- trailing"; //$NON-NLS-1$
        TestPreferenceStore connectionStore = new TestPreferenceStore() {
            @Override
            public String getString(String name) {
                return ModelPreferences.SCRIPT_STATEMENT_DELIMITER.equals(name) ? "GO" : ""; //$NON-NLS-1$
            }
        };

        //when
        String planSql = explainPlan(selection).getPlanQueryString(new PostgreDialect(), connectionStore);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT foo$GO -- trailing", planSql); //$NON-NLS-1$
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
    public void dollarQuoteBodyWithLineCommentUntouched() {
        //given
        String selection = "SELECT $$a; -- text$$; -- trailing"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT $$a; -- text$$ -- trailing", planSql); //$NON-NLS-1$
    }

    @Test
    public void namedDollarQuoteBodyWithLineCommentUntouched() {
        //given
        String selection = "SELECT $tag$a; -- text$tag$; -- trailing"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT $tag$a; -- text$tag$ -- trailing", planSql); //$NON-NLS-1$
    }

    @Test
    public void escapedQuoteInOrdinaryStringUsesDialectEscapeCharacter() {
        //given
        String selection = "SELECT 'a\\'; -- body'; -- trailing"; //$NON-NLS-1$
        PostgreDialect escapeDialect = new PostgreDialect() {
            @Override
            public char getStringEscapeCharacter() {
                return '\\';
            }
        };

        //when
        String planSql = explainXml(selection, escapeDialect);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 'a\\'; -- body' -- trailing", planSql); //$NON-NLS-1$
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
    public void escapedQuoteInEscapeStringYieldsSingleStatement() {
        //given
        String selection = "SELECT E'a\\'b'; -- trailing comment"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT E'a\\'b' -- trailing comment", planSql); //$NON-NLS-1$
    }

    @Test
    public void codeAfterTrailingCommentPreventsTrim() {
        //given
        String selection = "SELECT 1; -- trailing comment\nSELECT 2"; //$NON-NLS-1$

        //when
        String planSql = explainXml(selection);

        //then
        Assertions.assertEquals("EXPLAIN (FORMAT XML) SELECT 1; -- trailing comment\nSELECT 2", planSql); //$NON-NLS-1$
    }

    private static PostgreExecutionPlan explainPlan(String query) {
        return new PostgreExecutionPlan(false, false, query, new DBCQueryPlannerConfiguration());
    }

    private static String explainXml(String query) {
        return explainPlan(query).getPlanQueryString();
    }

    private static String explainXml(String query, SQLDialect dialect) {
        return explainPlan(query).getPlanQueryString(
            dialect, DBWorkbench.getPlatform().getPreferenceStore());
    }
}
