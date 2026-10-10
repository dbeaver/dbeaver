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
package org.jkiss.dbeaver.model.sql.analyzer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRoutineCompletion;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLPartitionScanner;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionProposal;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PostgreRoutineCompletionTest extends DBeaverUnitTest {

    @Test
    public void completeKeywordsInQuotedBodies() throws Exception {
        for (String delimiter : List.of("$$", "$function$")) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function(delimiter, "sel|"), false);
            assertFalse(usesEditorContext(request));
            assertEquals(IDocument.DEFAULT_CONTENT_TYPE, request.getContentType());
            assertProposal(request, "SELECT");
        }
    }

    @Test
    public void completeTablesInQuotedBody() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$function$", "SELECT * FROM test_t|"), false);
        assertProposal(request, "test_table");
    }

    @Test
    public void completeColumnsInQuotedBody() throws Exception {
        SQLCompletionRequest request = prepare(
            new PostgreDialect(), function("$$", "SELECT t.test_c| FROM test_table t;"), false
        );
        assertProposal(request, "test_column");
    }

    @Test
    public void completeColumnsInCurrentStatement() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
            "SELECT * FROM unrelated t;\nSELECT t.test_c| FROM test_table t;"), false);
        assertEquals("SELECT t.test_c FROM test_table t", request.getActiveQuery().getText());
        assertProposal(request, "test_column");
    }

    @Test
    public void ignoreStatementDelimitersInLiterals() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
            "SELECT '; SELECT dummy', t.test_c| FROM test_table t;"), false);
        assertTrue(request.getActiveQuery().getText().startsWith("SELECT '; SELECT dummy',"));
        assertProposal(request, "test_column");
    }

    @Test
    public void preserveNestedDollarLiteralsInQuery() throws Exception {
        String sql = "SELECT $text$; SELECT dummy$text$, t.test_c| FROM test_table t;";
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", sql), false);
        assertEquals(sql.replace("|", "").replaceAll(";$", ""), request.getActiveQuery().getText());
        assertEquals(proposals(prepare(new PostgreDialect(), sql, false)), proposals(request));
    }

    @Test
    public void preserveCaseExpressionsAndSubqueries() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
            "SELECT CASE WHEN EXISTS (SELECT 1) THEN t.test_c| ELSE 0 END FROM test_table t;"), false);
        assertTrue(request.getActiveQuery().getText().startsWith("SELECT CASE"));
        assertProposal(request, "test_column");
    }

    @Test
    public void preserveCommentsInsideBody() throws Exception {
        assertContent("-- sel|", SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT);
        assertContent("/* sel| */", SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT);
        assertContent("/* outer /* inner */ sel| */", SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT);
    }

    @Test
    public void preserveLiteralsInsideBody() throws Exception {
        for (String body : List.of("SELECT 'sel|'", "SELECT E'sel|'", "SELECT $text$sel|$text$")) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", body), false);
            assertFalse(usesEditorContext(request));
            assertEquals(SQLParserPartitions.CONTENT_TYPE_SQL_STRING, request.getContentType());
            assertFalse(proposals(request).stream().anyMatch("SELECT"::equalsIgnoreCase));
        }
    }

    @Test
    public void preserveQuotedIdentifiersInsideBody() throws Exception {
        assertContent("SELECT \"test_c|\"", SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED);
    }

    @Test
    public void completeAfterInnerCommentAndLiteral() throws Exception {
        for (String body : List.of("-- comment\nsel|", "/* comment */ sel|", "SELECT 'value';\nsel|")) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", body), false);
            assertEquals(IDocument.DEFAULT_CONTENT_TYPE, request.getContentType());
            assertProposal(request, "SELECT");
        }
    }

    @Test
    public void bodyCompletionIsIndependentOfScriptStringSettings() throws Exception {
        for (String delimiter : List.of("$$", "$function$")) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function(delimiter, "sel|"), true);
            assertFalse(usesEditorContext(request));
            assertEquals(IDocument.DEFAULT_CONTENT_TYPE, request.getContentType());
            assertProposal(request, "SELECT");
        }
    }

    @Test
    public void preserveOrdinaryStrings() throws Exception {
        for (SQLDialect dialect : List.of(new PostgreDialect(), BasicSQLDialect.INSTANCE)) {
            SQLCompletionRequest request = prepare(dialect, "SELECT '$$ sel| $$'", false);
            assertTrue(usesEditorContext(request));
            assertEquals(SQLParserPartitions.CONTENT_TYPE_SQL_STRING, request.getContentType());
            assertFalse(proposals(request).stream().anyMatch("SELECT"::equalsIgnoreCase));
        }
    }

    @Test
    public void dollarSignsDoNotEnableBlocksInOtherDialects() throws Exception {
        SQLCompletionRequest request = prepare(BasicSQLDialect.INSTANCE, function("$$", "sel|"), false);
        assertTrue(usesEditorContext(request));
    }

    @Test
    public void completeWithoutHeader() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), "BEGIN\nsel|\nEND;", false);
        assertFalse(usesEditorContext(request));
        assertProposal(request, "SELECT");
    }

    @Test
    public void respectBodyBoundaries() throws Exception {
        SQLCompletionRequest empty = prepare(new PostgreDialect(), "CREATE FUNCTION f() RETURNS void AS $$|$$ LANGUAGE sql;", false);
        assertFalse(usesEditorContext(empty));
        assertEquals(IDocument.DEFAULT_CONTENT_TYPE, empty.getContentType());
        for (String sql : List.of(
            "CREATE FUNCTION f() RETURNS void AS $fun|ction$sel$function$;",
            "CREATE FUNCTION f() RETURNS void AS $$sel$|$;",
            "CREATE FUNCTION f() RETURNS void AS $$sel$$|;"
        )) {
            assertTrue(usesEditorContext(prepare(new PostgreDialect(), sql, false)));
        }
    }

    @Test
    public void completeWithinIfAndFor() throws Exception {
        for (String block : List.of(
            "IF EXISTS (SELECT 1 FROM test_table) THEN\nSELECT t.test_c| FROM test_table t;\nEND IF;",
            "FOR r IN SELECT * FROM test_table LOOP\nSELECT t.test_c| FROM test_table t;\nEND LOOP;"
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", block), false);
            assertEquals("SELECT t.test_c FROM test_table t", request.getActiveQuery().getText());
            assertProposal(request, "test_column");
        }
    }

    @Test
    public void completeImmediatelyAfterOpeningQuote() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), "CREATE FUNCTION f() RETURNS int LANGUAGE sql AS $$sel|$$;", false);
        assertEquals("sel", request.getWordPart());
        assertProposal(request, "SELECT");
    }

    @Test
    public void leaveDollarLiteralsOutsideBodyAlone() throws Exception {
        for (String sql : List.of(
            "SELECT $$ sel| $$;",
            "CREATE FUNCTION f(x text DEFAULT $$ sel| $$) RETURNS text AS $$BEGIN RETURN x; END;$$ LANGUAGE plpgsql;"
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), sql, false);
            assertTrue(usesEditorContext(request));
            assertEquals(SQLParserPartitions.CONTENT_TYPE_SQL_STRING, request.getContentType());
            assertFalse(proposals(request).stream().anyMatch("SELECT"::equalsIgnoreCase));
        }
    }

    @Test
    public void reuseSnapshotUntilSourceChanges() throws Exception {
        SQLCompletionContext context = prepare(new PostgreDialect(), "sel|", false).getContext();
        Document document = new Document("BEGIN\nsel\nEND;");
        PostgreRoutineCompletion completion = new PostgreRoutineCompletion();
        SQLCompletionRequest first = completion.createRequest(context, document, 9, true, true);
        SQLCompletionRequest second = completion.createRequest(context, document, 8, true, true);
        assertNotNull(first);
        assertNotNull(second);
        assertSame(first.getDocument(), second.getDocument());
        document.replace(6, 3, "SELECT");
        SQLCompletionRequest changed = completion.createRequest(context, document, 12, true, true);
        assertNotNull(changed);
        assertNotSame(first.getDocument(), changed.getDocument());
        assertTrue(first.getDocument().get().startsWith("BEGIN\nsel"));
        assertTrue(changed.getDocument().get().startsWith("BEGIN\nSELECT"));
        assertNull(completion.createRequest(context, document, 12, true, false));
    }

    @Test
    public void sqlIdentifierMustNotSplitQuery() throws Exception {
        String sql = "SELECT t.test_c|, 1 AS loop FROM test_table t;";
        assertProposal(prepare(new PostgreDialect(), sql, false), "test_column");
        assertProposal(prepare(new PostgreDialect(), function("$$", sql), false), "test_column");
    }

    @Test
    public void returnQueryWithNewline() throws Exception {
        assertProposal(prepare(new PostgreDialect(), function("$$", "RETURN\nQUERY SELECT t.test_c| FROM test_table t;"), false), "test_column");
    }

    @Test
    public void returnQueryWithSpace() throws Exception {
        assertProposal(prepare(new PostgreDialect(), function("$$", "RETURN QUERY SELECT t.test_c| FROM test_table t;"), false), "test_column");
    }

    @Test
    public void proceduralCaseMustSplitStatements() throws Exception {
        assertProposal(prepare(new PostgreDialect(), function("$$", "CASE WHEN true THEN\nSELECT t.test_c| FROM test_table t;\nELSE NULL; END CASE;"), false), "test_column");
    }

    @Test
    public void completeInForSelect() throws Exception {
        assertProposal(prepare(new PostgreDialect(), function("$$", "FOR r IN SELECT t.test_c| FROM test_table t LOOP\nNULL; END LOOP;"), false), "test_column");
    }

    @Test
    public void preserveBodyContextWhenTypingContinues() throws Exception {
        SQLCompletionRequest seed = prepare(new PostgreDialect(), function("$$", "se|"), false);
        String text = function("$$", "se|");
        int offset = text.indexOf('|');
        Document document = partition(seed.getContext(), text.replace("|", ""));
        PostgreRoutineCompletion completion = new PostgreRoutineCompletion();
        SQLCompletionRequest request = completion.createRequest(seed.getContext(), document, offset, true, false);
        assertNotNull(request);
        document.replace(offset, 0, "l");
        assertEquals("sel", request.createWordDetector(document, offset + 1).getWordPart());
    }

    @Test
    public void leaveLargeTailOutOfCompletionSnapshotAfterEdits() throws Exception {
        SQLCompletionRequest seed = prepare(new PostgreDialect(), "sel|", false);
        String text = function("$$", "sel|;\n" + "PERFORM 1;\n".repeat(100_000));
        int offset = text.indexOf('|');
        Document document = partition(seed.getContext(), text.replace("|", ""));
        PostgreRoutineCompletion completion = new PostgreRoutineCompletion();
        for (String letter : List.of("L", "l")) {
            document.replace(offset - 1, 1, letter);
            SQLCompletionRequest request = completion.createRequest(seed.getContext(), document, offset, true, false);
            assertNotNull(request);
            assertTrue(request.getDocument().getLength() <= offset + 2);
            assertProposal(request, "SELECT");
        }
    }

    @Test
    public void preserveNestedSelectAndWithQueries() throws Exception {
        for (String sql : List.of(
            "SELECT t.test_c| FROM test_table t WHERE EXISTS (SELECT 1);",
            "IF EXISTS (SELECT t.test_c| FROM test_table t) THEN NULL; END IF;",
            "WITH c AS (SELECT 1) SELECT t.test_c| FROM test_table t;"
        )) {
            assertProposal(prepare(new PostgreDialect(), function("$$", sql), false), "test_column");
        }
    }

    @Test
    public void dropTableKeepsMetadataCompletion() throws Exception {
        String sql = "DROP TABLE test_t|;";
        assertProposal(prepare(new PostgreDialect(), sql, false), "test_table");
        assertProposal(prepare(new PostgreDialect(), function("$$", sql), false), "test_table");
    }

    @Test
    public void alterTableKeepsMetadataCompletion() throws Exception {
        String sql = "ALTER TABLE test_t| ADD COLUMN c integer;";
        assertProposal(prepare(new PostgreDialect(), sql, false), "test_table");
        assertProposal(prepare(new PostgreDialect(), function("$$", sql), false), "test_table");
    }

    @Test
    public void lockTableKeepsMetadataCompletion() throws Exception {
        for (String sql : List.of("LOCK TABLE test_t| IN ACCESS SHARE MODE;", "lock table test_t| in access share mode;")) {
            assertProposal(prepare(new PostgreDialect(), sql, false), "test_table");
            for (String source : List.of(function("$$", sql), "BEGIN\nSELECT 1;\n" + sql + "\nEND;")) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), source, false);
                assertEquals(sql.replace("|", "").replaceAll(";$", ""), request.getActiveQuery().getText());
                assertProposal(request, "test_table");
            }
        }
    }

    @Test
    public void tableStatementKeepsMetadataCompletion() throws Exception {
        for (String sql : List.of("TABLE test_t|;", "table test_t|;")) {
            SQLCompletionRequest baseline = prepare(new PostgreDialect(), sql, false);
            assertProposal(baseline, "test_table");
            SQLCompletionRequest withHeader = prepare(new PostgreDialect(),
                "CREATE FUNCTION f() RETURNS SETOF test_table AS $$" + sql + "$$ LANGUAGE sql;", false);
            Document document = partition(baseline.getContext(), sql.replace("|", ""));
            SQLCompletionRequest bodyOnly = new PostgreRoutineCompletion().createRequest(
                baseline.getContext(), document, sql.indexOf('|'), true, true);
            assertNotNull(bodyOnly);
            for (SQLCompletionRequest request : List.of(withHeader, bodyOnly)) {
                assertEquals(sql.replace("|", "").replaceAll(";$", ""), request.getActiveQuery().getText());
                assertProposal(request, "test_table");
            }
        }
    }

    @Test
    public void tableCommandsKeepPlainSqlProposals() throws Exception {
        for (String sql : List.of(
            "SELECT * FROM test_t|;", "INSERT INTO test_t| DEFAULT VALUES;", "UPDATE test_t| SET c=1;",
            "DELETE FROM test_t|;", "TRUNCATE test_t|;", "ALTER TABLE test_t| ADD COLUMN c integer;",
            "DROP TABLE test_t|;", "LOCK TABLE test_t| IN ACCESS SHARE MODE;", "TABLE test_t|;",
            "REFRESH MATERIALIZED VIEW test_t|;", "COMMENT ON VIEW test_t| IS 'text';",
            "COMMENT ON TABLE test_t| IS 'text';", "GRANT SELECT ON TABLE test_t| TO PUBLIC;",
            "REVOKE SELECT ON TABLE test_t| FROM PUBLIC;", "CREATE INDEX idx ON test_t| (c);",
            "ANALYZE test_t|;", "REINDEX TABLE test_t|;", "COPY test_t| TO STDOUT;"
        )) {
            List<String> expected = proposals(prepare(new PostgreDialect(), sql, false));
            for (String source : List.of(function("$$", sql), "BEGIN\n" + sql + "\nEND;")) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), source, false);
                assertEquals(expected, proposals(request), source);
            }
        }
    }

    @Test
    public void unrecognizedCommandsKeepTheirEntireStatement() throws Exception {
        for (String sql : List.of(
            "REFRESH MATERIALIZED VIEW test_t| WITH NO DATA;",
            "COMMENT ON VIEW test_t| IS 'THEN; SELECT';",
            "refresh materialized view test_t|;"
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", "SELECT 1;\n" + sql), false);
            assertEquals(sql.replace("|", "").replaceAll(";$", ""), request.getActiveQuery().getText());
            assertProposal(request, "test_table");
        }
    }

    @Test
    public void fallbackObjectPrefixesAreNotStatementKeywords() throws Exception {
        for (String keyword : List.of("loop", "lock", "begin", "for", "then", "else", "open", "cursor", "select", "update")) {
            String tableName = keyword + "_suffix";
            for (String prefix : List.of(keyword.substring(0, keyword.length() - 1), keyword)) {
                for (String sql : List.of("COMMENT ON VIEW " + prefix + "| IS 'text';", "refresh materialized view " + prefix + "|;")) {
                    SQLCompletionRequest baseline = prepare(new PostgreDialect(), sql, false, tableName);
                    assertProposal(baseline, tableName);
                    for (String source : List.of(function("$$", sql), "BEGIN\n" + sql + "\nEND;")) {
                        SQLCompletionRequest request = prepare(new PostgreDialect(), source, false, tableName);
                        assertEquals(sql.replace("|", "").replaceAll(";$", ""), request.getActiveQuery().getText());
                        assertEquals(proposals(baseline), proposals(request), source);
                    }
                }
            }
        }
    }

    @Test
    public void entityKeywordsInProceduralExpressionsDoNotStartQueries() throws Exception {
        for (String prefix : List.of(
            "IF view THEN ", "WHILE view LOOP ", "CASE view WHEN true THEN ",
            "IF r.view THEN ", "r.view := (", "result := (", "<<view>> BEGIN "
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", prefix
                + "SELECT t.test_c| FROM test_table t;"), false);
            assertEquals("SELECT t.test_c FROM test_table t", request.getActiveQuery().getText());
            assertProposal(request, "test_column");
        }
    }

    @Test
    public void qualifiedFieldsDoNotStartStatements() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t";
        assertProposal(prepare(new PostgreDialect(), query, false), "test_column");
        for (String field : List.of("lock", "select", "update", "insert", "table", "create", "call", "loop", "open", "cursor")) {
            for (String reference : List.of("r." + field, "r . /* field */ " + field)) {
                for (String statement : List.of(
                    "IF " + reference + " > 0 THEN RETURN QUERY " + query + "; END IF;",
                    reference + " := (" + query + " LIMIT 1); RETURN QUERY SELECT " + reference + ";"
                )) {
                    String source = "CREATE FUNCTION f() RETURNS SETOF integer AS $$\nDECLARE r record;\nBEGIN\n"
                        + "SELECT 1 AS \"" + field + "\" INTO r;\n" + statement + "\nEND;\n$$ LANGUAGE plpgsql;";
                    SQLCompletionRequest request = prepare(new PostgreDialect(), source, false);
                    assertTrue(request.getActiveQuery().getText().startsWith(query.replace("|", "")), source);
                    assertProposal(request, "test_column");
                }
            }
        }
    }

    @Test
    public void qualifiedLoopFieldDoesNotEndForQuery() throws Exception {
        for (String field : List.of("t.loop", "t . /* field */ loop", "t.\"loop\"")) {
            String query = "SELECT t.test_c| FROM test_table t WHERE " + field;
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
                "FOR r IN " + query + " LOOP NULL; END LOOP;"), false);
            assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing());
            assertProposal(request, "test_column");
        }
    }

    @Test
    public void localVariableNamesDoNotStartQueriesInConditions() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t";
        for (String variable : List.of("lock", "view", "flag")) {
            for (String body : List.of(
                "BEGIN\nIF " + variable + " THEN RETURN QUERY " + query + "; END IF;\nEND;",
                "BEGIN\nWHILE (" + variable + ") LOOP RETURN QUERY " + query + "; " + variable + " := false; END LOOP;\nEND;"
            )) {
                String source = "CREATE FUNCTION f(\"" + variable + "\" boolean) RETURNS SETOF integer AS $$"
                    + body + "$$ LANGUAGE plpgsql;";
                for (String text : List.of(source, body)) {
                    SQLCompletionRequest request = prepare(new PostgreDialect(), text, false);
                    assertEquals(query.replace("|", ""), request.getActiveQuery().getText());
                    assertProposal(request, "test_column");
                }
            }
        }
    }

    @Test
    public void keywordVariablesInForAndCursorPrefixesDoNotStartQueries() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t";
        for (String body : List.of(
            "FOR lock IN " + query + " LOOP NULL; END LOOP;",
            "FOR lock IN SELECT 1 LOOP RETURN QUERY " + query + "; END LOOP;",
            "FOR i IN lock .. 10 LOOP RETURN QUERY " + query + "; END LOOP;",
            "OPEN lock FOR " + query + ";"
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", body), false);
            assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing());
            assertProposal(request, "test_column");
        }
        SQLCompletionRequest cursor = prepare(new PostgreDialect(),
            "CREATE FUNCTION f() RETURNS void AS $$ DECLARE dummy integer; lock CURSOR FOR "
                + query + "; BEGIN NULL; END; $$ LANGUAGE plpgsql;", false);
        assertEquals(query.replace("|", ""), cursor.getActiveQuery().getText());
        assertProposal(cursor, "test_column");
    }

    @Test
    public void keywordAssignmentTargetsKeepSubqueryCompletion() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t LIMIT 1";
        for (String target : List.of("lock", "lock.value", "lock[1]")) {
            for (String operator : List.of(":=", "=")) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
                    target + " " + operator + " (" + query + ");"), false);
                assertEquals(query.replace("|", ""), request.getActiveQuery().getText());
                assertProposal(request, "test_column");
            }
        }
    }

    @Test
    public void conditionsKeepTheirContextAfterSubqueries() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t";
        for (String body : List.of(
            "IF EXISTS (SELECT 1) AND lock THEN RETURN QUERY " + query + "; END IF;",
            "IF lock AND EXISTS (" + query + ") THEN NULL; END IF;",
            "IF EXISTS (SELECT 1) AND EXISTS (" + query + ") THEN NULL; END IF;",
            "WHILE EXISTS (SELECT 1) AND lock LOOP RETURN QUERY " + query + "; END LOOP;",
            "CASE lock WHEN true THEN RETURN QUERY " + query + "; END CASE;"
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", body), false);
            assertEquals(query.replace("|", ""), request.getActiveQuery().getText());
            assertProposal(request, "test_column");
        }
    }

    @Test
    public void fallbackRespectsProceduralBoundaries() throws Exception {
        for (String prefix : List.of(
            "IF true THEN ", "IF EXISTS (SELECT 1) THEN ", "WHILE true LOOP ",
            "FOR r IN SELECT 1 LOOP ", "CASE WHEN true THEN ", "IF false THEN NULL; ELSE "
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", prefix
                + "REFRESH MATERIALIZED VIEW test_t|;"), false);
            assertEquals("REFRESH MATERIALIZED VIEW test_t", request.getActiveQuery().getText());
            assertProposal(request, "test_table");
        }
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
            "IF test| THEN SELECT 1; END IF;"), false);
        assertEquals("IF test", request.getActiveQuery().getText().stripTrailing());
    }

    @Test
    public void cursorForIsNotForLoop() throws Exception {
        String sql = "SELECT t.test_c|, 1 AS loop FROM test_table t;";
        assertProposal(prepare(new PostgreDialect(), sql, false), "test_column");
        assertProposal(prepare(new PostgreDialect(), function("$$", "OPEN cur FOR " + sql), false), "test_column");
    }

    @Test
    public void callKeepsExecContext() throws Exception {
        SQLCompletionRequest baseline = prepare(new PostgreDialect(), "CALL p|;", false);
        proposals(baseline);
        assertEquals(SQLCompletionRequest.QueryType.EXEC, baseline.getQueryType());
        SQLCompletionRequest body = prepare(new PostgreDialect(), function("$$", "CALL p|;"), false);
        proposals(body);
        assertEquals(SQLCompletionRequest.QueryType.EXEC, body.getQueryType());
    }

    @Test
    public void cursorDeclarationIsNotForLoop() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(),
            "CREATE FUNCTION f() RETURNS void AS $$\nDECLARE cur CURSOR FOR "
                + "SELECT t.test_c|, 1 AS loop FROM test_table t;\nBEGIN NULL; END;$$ LANGUAGE plpgsql;", false);
        assertProposal(request, "test_column");
    }

    @Test
    public void forLoopAfterCursorStatementKeepsItsBoundary() throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
            "OPEN cur FOR SELECT 1;\nFOR r IN SELECT t.test_c| FROM test_table t LOOP NULL; END LOOP;"), false);
        assertEquals("SELECT t.test_c FROM test_table t", request.getActiveQuery().getText().stripTrailing());
        assertProposal(request, "test_column");
    }

    @Test
    public void proposalValidationDoesNotRescanUnchangedSource() throws Exception {
        SQLCompletionContext context = prepare(new PostgreDialect(), "sel|", false).getContext();
        int[] reads = {0};
        Document document = new Document("BEGIN\n" + "PERFORM 1;\n".repeat(1000) + "sel;\nEND;") {
            @Override
            public char getChar(int offset) throws BadLocationException {
                reads[0]++;
                return super.getChar(offset);
            }
        };
        int offset = document.get().indexOf("sel") + 3;
        PostgreRoutineCompletion completion = new PostgreRoutineCompletion();
        SQLCompletionRequest request = completion.createRequest(context, document, offset, true, true);
        assertNotNull(request);
        document.replace(offset - 1, 1, "L");
        assertEquals("seL", request.createWordDetector(document, offset).getWordPart());
        reads[0] = 0;
        for (int i = 0; i < 100; i++) {
            assertEquals("seL", request.createWordDetector(document, offset).getWordPart());
        }
        assertEquals(0, reads[0], "Filtering proposals must not rescan the source for each candidate");
    }

    @Test
    public void refreshPreparedQueryAfterMovingOrEditing() throws Exception {
        SQLCompletionContext context = prepare(new PostgreDialect(), "sel|", false).getContext();
        Document document = new Document("BEGIN\nSELECT 1;\nSELECT 2;\nEND;");
        PostgreRoutineCompletion completion = new PostgreRoutineCompletion();
        int firstOffset = document.get().indexOf('1') + 1;
        int secondOffset = document.get().indexOf('2') + 1;
        SQLCompletionRequest first = completion.createRequest(context, document, firstOffset, true, true);
        SQLCompletionRequest second = completion.createRequest(context, document, secondOffset, true, true);
        assertEquals("SELECT 1", first.getActiveQuery().getText());
        assertEquals("SELECT 2", second.getActiveQuery().getText());
        document.replace(secondOffset - 1, 1, "3");
        SQLCompletionRequest edited = completion.createRequest(context, document, secondOffset, true, true);
        assertEquals("SELECT 3", edited.getActiveQuery().getText());
        assertEquals("SELECT 2", second.getActiveQuery().getText());
        Document replacement = new Document("BEGIN\nSELECT 1;\nSELECT 4;\nEND;");
        assertEquals("SELECT 4", completion.createRequest(context, replacement, secondOffset, true, true).getActiveQuery().getText());
    }

    @Test
    public void integerForBoundsKeepNestedQueryCompletion() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t LIMIT 1";
        for (String variable : List.of("update", "insert", "delete", "values", "lock")) {
            for (String bounds : List.of(
                variable + " + (" + query + ") .. 45",
                "(" + variable + " + (" + query + "))..45",
                "1..(" + variable + " + (" + query + "))",
                "REVERSE 45 .. " + variable + " + (" + query + ") BY 1"
            )) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
                    "FOR i IN " + bounds + " LOOP NULL; END LOOP;"), false);
                assertEquals(query.replace("|", ""), request.getActiveQuery().getText(), bounds);
                assertProposal(request, "test_column");
            }
        }
    }

    @Test
    public void integerForRangeDoesNotHideCaseUpperBound() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t LIMIT 1";
        for (String separator : List.of("..", " .. ", " .. /* upper bound */ ")) {
            for (String variable : List.of("lock", "update", "view")) {
                for (String expression : List.of(
                    "CASE WHEN false THEN " + variable + " ELSE (" + query + ") END",
                    "CASE WHEN false THEN " + variable + " ELSE CASE WHEN true THEN (" + query + ") ELSE 0 END END"
                )) {
                    for (String bound : List.of(expression, "(" + expression + ")")) {
                        String body = "BEGIN FOR i IN 1" + separator + bound + " LOOP NULL; END LOOP; END;";
                        for (String sql : List.of(body, "CREATE FUNCTION f() RETURNS void AS $$" + body + "$$ LANGUAGE plpgsql;")) {
                            SQLCompletionRequest request = prepare(new PostgreDialect(), sql, false);
                            assertEquals(query.replace("|", ""), request.getActiveQuery().getText(), sql);
                            assertProposal(request, "test_column");
                            assertTrue(semanticProposals(request).stream()
                                .anyMatch(p -> p.getDisplayString().equals("test_column")), sql);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void loopPrefixUnderCaretRemainsInForQuery() throws Exception {
        for (String query : List.of("SELECT * FROM loop|", "SELECT loop| FROM test_table")) {
            String expected = query.contains("*") ? "loop_table" : "loop_column";
            for (String sql : List.of(function("$$", "FOR r IN " + query + " LOOP NULL; END LOOP;"),
                "BEGIN FOR r IN " + query + " LOOP NULL; END LOOP; END;")) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), sql, false, "loop_table");
                assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing());
                assertProposal(request, expected);
            }
        }
    }

    @Test
    public void forRangeIgnoresNestedExpressionsAndLiterals() throws Exception {
        for (String query : List.of(
            "SELECT t.test_c| FROM test_table t WHERE '..' = '..'",
            "SELECT t.test_c| FROM test_table t /* .. */",
            "UPDATE test_table t SET test_column = 42 RETURNING t.test_c|"
        )) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$",
                "FOR r IN " + query + " LOOP NULL; END LOOP;"), false);
            assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing());
            assertProposal(request, "test_column");
        }
    }

    @Test
    public void arrayExpressionsDoNotEndForQueries() throws Exception {
        for (String query : List.of(
            "SELECT ARRAY[loop], t.test_c| FROM test_table t",
            "SELECT (ARRAY[1])[loop], t.test_c| FROM test_table t",
            "SELECT ARRAY[ARRAY[loop]], t.test_c| FROM test_table t",
            "SELECT t.test_c| FROM test_table t WHERE ARRAY[loop] = ARRAY[1]"
        )) {
            String body = "BEGIN FOR r IN " + query + " LOOP NULL; END LOOP; END;";
            for (String sql : List.of(body, "CREATE FUNCTION f() RETURNS void AS $$" + body + "$$ LANGUAGE plpgsql;")) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), sql, false);
                assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing());
                assertProposal(request, "test_column");
            }
        }
    }

    @Test
    public void expressionCaseDoesNotSplitProceduralStatements() throws Exception {
        String query = "SELECT t.test_c| FROM test_table t LIMIT 1";
        for (String statement : List.of(
            "WHILE CASE WHEN true THEN lock ELSE false END LOOP RETURN QUERY " + query + "; EXIT; END LOOP;",
            "RETURN CASE WHEN false THEN lock ELSE (" + query + ") END;",
            "result := CASE WHEN false THEN lock ELSE (" + query + ") END;",
            "RETURN CASE WHEN false THEN CASE WHEN true THEN lock ELSE 0 END ELSE (" + query + ") END;",
            "RETURN CASE WHEN EXISTS (SELECT 1) THEN CASE WHEN false THEN lock ELSE (" + query + ") END ELSE 0 END;",
            "FOR i IN CASE WHEN true THEN lock ELSE 1 END .. (" + query + ") LOOP NULL; END LOOP;",
            "IF (CASE WHEN true THEN lock ELSE false END) THEN RETURN QUERY " + query + "; END IF;",
            "CASE (CASE WHEN true THEN lock ELSE false END) WHEN true THEN RETURN QUERY " + query + "; END CASE;",
            "CASE WHEN true THEN CASE WHEN false THEN NULL; ELSE RETURN QUERY " + query + "; END CASE; END CASE;",
            "result := CASE WHEN true THEN 1 ELSE 0 END; RETURN QUERY " + query + ";"
        )) {
            for (String sql : List.of(function("$$", statement), "BEGIN " + statement + " END;")) {
                SQLCompletionRequest request = prepare(new PostgreDialect(), sql, false);
                assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing(), statement);
                assertProposal(request, "test_column");
                assertTrue(semanticProposals(request).stream().anyMatch(p -> p.getDisplayString().equals("test_column")), statement);
            }
        }
    }

    @Test
    public void semanticCompletionUsesRoutineOffsetsAndSupportsInto() throws Exception {
        for (String separator : List.of("\n", "\r\n", "\r")) {
            for (String clause : List.of("", "INTO result ", "INTO STRICT result ")) {
                for (boolean bodyOnly : List.of(false, true)) {
                    String body = "BEGIN\nSELECT 1;\nSELECT\n/* comment */\nt.test_c|\n" + clause + "FROM test_table t;\nEND;";
                    String sql = (bodyOnly ? body : "CREATE FUNCTION f() RETURNS void AS $$" + body + "$$ LANGUAGE plpgsql;")
                        .replace("\n", separator);
                    SQLCompletionRequest request = prepare(new PostgreDialect(), sql, false);
                    var proposal = semanticProposals(request).stream()
                        .filter(p -> p.getDisplayString().equals("test_column")).findFirst().orElseThrow();
                    Document document = new Document(sql.replace("|", ""));
                    proposal.apply(document);
                    assertEquals(sql.replace("test_c|", "test_column"), document.get());
                    Document liveDocument = new Document(sql.replace("|", ""));
                    int offset = sql.indexOf('|');
                    liveDocument.replace(offset, 0, "o");
                    assertTrue(proposal.validate(liveDocument, offset + 1, new DocumentEvent(liveDocument, offset, 0, "o")));
                }
            }
        }
    }

    @Test
    public void valuesQueriesKeepTheirRows() throws Exception {
        String query = "VALUES ((SELECT t.test_c| FROM test_table t LIMIT 1))";
        for (String body : List.of("RETURN QUERY " + query + ";", "FOR r IN " + query + " LOOP NULL; END LOOP;")) {
            SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", body), false);
            assertEquals(query.replace("|", ""), request.getActiveQuery().getText().stripTrailing());
            assertEquals(proposals(prepare(new PostgreDialect(), query, false)), proposals(request));
        }
    }

    @Test
    public void semanticRequestsKeepTheirOwnSnapshot() throws Exception {
        SQLCompletionContext context = prepare(new PostgreDialect(), "sel|", false).getContext();
        Document document = new Document("BEGIN SELECT t.test_c FROM test_table t; END;");
        int offset = document.get().indexOf("test_c") + "test_c".length();
        PostgreRoutineCompletion helper = new PostgreRoutineCompletion();
        SQLCompletionRequest first = helper.createRequest(context, document, offset, true, true);
        document.replace(offset - "test_c".length(), "test_c".length(), "loop_c");
        SQLCompletionRequest second = helper.createRequest(context, document, offset, true, true);
        assertTrue(semanticProposals(first).stream().anyMatch(p -> p.getDisplayString().equals("test_column")));
        assertTrue(semanticProposals(second).stream().anyMatch(p -> p.getDisplayString().equals("loop_column")));
    }

    private boolean usesEditorContext(@NotNull SQLCompletionRequest request) {
        boolean[] delegated = {false};
        request.obtainCompletionContext(new VoidProgressMonitor(), () -> {
            delegated[0] = true;
            return null;
        });
        return delegated[0];
    }

    @NotNull
    private List<? extends SQLQueryCompletionProposal> semanticProposals(@NotNull SQLCompletionRequest request) throws Exception {
        SQLQueryCompletionAnalyzer analyzer = new SQLQueryCompletionAnalyzer(
            monitor -> request.obtainCompletionContext(monitor, () -> {
                fail("Routine must use its own semantic context");
                return null;
            }),
            request, request::getDocumentOffset
        );
        analyzer.run(new VoidProgressMonitor());
        return analyzer.getResult();
    }

    private void assertContent(@NotNull String body, @NotNull String type) throws Exception {
        SQLCompletionRequest request = prepare(new PostgreDialect(), function("$$", body), false);
        assertFalse(usesEditorContext(request));
        String actual = SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(type)
            ? TextUtilities.getContentType(request.getDocument(), SQLParserPartitions.SQL_PARTITIONING, request.getDocumentOffset() - 1, true)
            : request.getContentType();
        assertEquals(type, actual);
    }

    @NotNull
    private static String function(@NotNull String delimiter, @NotNull String body) {
        return "CREATE FUNCTION f() RETURNS void AS " + delimiter + "\nBEGIN\n" + body + "\nEND;\n" + delimiter + " LANGUAGE plpgsql;";
    }

    @NotNull
    private SQLCompletionRequest prepare(
        @NotNull SQLDialect dialect, @NotNull String sql, boolean dollarStrings, @NotNull String... extraTables
    ) throws Exception {
        RequestBuilder builder = RequestBuilder.tables(t -> {
            t.table("test_table", c -> {
                c.attribute("test_column");
                c.attribute("loop_column");
            });
            for (String tableName : extraTables) {
                t.table(tableName, c -> c.attribute("test_column"));
            }
        });
        builder.prepare();
        DBPDataSource dataSource = builder.getObject().getDataSource();
        when(dataSource.getSQLDialect()).thenReturn(dialect);
        var configuration = dataSource.getContainer().getActualConnectionConfiguration();
        configuration.setProviderProperty(PostgreConstants.PROP_DD_TAG_STRING, Boolean.toString(dollarStrings));
        configuration.setProviderProperty(PostgreConstants.PROP_DD_PLAIN_STRING, Boolean.toString(dollarStrings));
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, dataSource.getContainer().getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        SQLCompletionContext context = mock(SQLCompletionContext.class);
        when(context.getDataSource()).thenReturn(dataSource);
        DBCExecutionContext executionContext = mock(DBCExecutionContext.class);
        when(executionContext.getDataSource()).thenReturn(dataSource);
        when(context.getExecutionContext()).thenReturn(executionContext);
        when(context.getSyntaxManager()).thenReturn(syntaxManager);
        when(context.getRuleManager()).thenReturn(ruleManager);
        when(context.createProposal(any(), anyString(), anyString(), anyInt(), any(), any(), any(), any(), any())).thenAnswer(a ->
            new SQLCompletionProposalBase(
                a.getArgument(0), a.getArgument(1), a.getArgument(2), a.getArgument(3), a.getArgument(4),
                a.getArgument(5), a.getArgument(6), a.getArgument(7), a.getArgument(8)
            )
        );
        int offset = sql.indexOf('|');
        String text = sql.substring(0, offset) + sql.substring(offset + 1);
        Document document = partition(context, text);
        SQLCompletionRequest request = new PostgreRoutineCompletion().createRequest(context, document, offset, true, sql.startsWith("BEGIN"));
        if (request == null) {
            request = new SQLCompletionRequest(context, document, offset, new SQLQuery(dataSource, text), true);
        }
        request.setContentType(TextUtilities.getContentType(request.getDocument(), SQLParserPartitions.SQL_PARTITIONING, offset, true));
        return request;
    }

    @NotNull
    private Document partition(@NotNull SQLCompletionContext context, @NotNull String text) {
        Document document = new Document(text);
        FastPartitioner partitioner = new FastPartitioner(
            new SQLPartitionScanner(context.getDataSource(), context.getSyntaxManager().getDialect(), context.getRuleManager()),
            SQLParserPartitions.SQL_CONTENT_TYPES
        );
        document.setDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING, partitioner);
        partitioner.connect(document);
        return document;
    }

    private void assertProposal(@NotNull SQLCompletionRequest request, @NotNull String expected) throws Exception {
        List<String> values = proposals(request);
        assertTrue(
            values.stream().anyMatch(v -> v.equalsIgnoreCase(expected) || v.regionMatches(true, 0, expected + " ", 0, expected.length() + 1)),
            () -> "Missing " + expected + " in " + values + "; query=" + (request.getActiveQuery() == null ? "<none>" : request.getActiveQuery().getText())
        );
    }

    @NotNull
    private List<String> proposals(@NotNull SQLCompletionRequest request) throws Exception {
        SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
        analyzer.setCheckNavigatorNodes(false);
        analyzer.runAnalyzer(new VoidProgressMonitor());
        return analyzer.getProposals().stream().map(SQLCompletionProposalBase::getReplacementString).toList();
    }
}
