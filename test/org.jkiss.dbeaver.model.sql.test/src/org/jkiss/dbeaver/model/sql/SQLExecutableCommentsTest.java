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

import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

public class SQLExecutableCommentsTest {
    private static final SQLDialect MYSQL_LIKE_DIALECT = new BasicSQLDialect() {
        @NotNull
        @Override
        public List<String> getExecutableCommentPrefixes() {
            return List.of("/*!", "/*M!");
        }

        @NotNull
        @Override
        public Boolean isExecutableCommentActive(@NotNull String prefix, int version) {
            return version <= (prefix.equals("/*M!") ? 110000 : 80036);
        }

        @NotNull
        @Override
        public String[] getSingleLineComments() {
            return new String[]{"-- ", "--\t", "#"};
        }

        @NotNull
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"`", "`"}, {"\"", "\""}};
        }

        @Override
        public char getStringEscapeCharacter() {
            return '\\';
        }
    };

    @Test
    public void exposesExecutableCommentTablesToSemanticValidation() throws Exception {
        for (String sql : List.of(
            "SELECT 1 /*! UNION SELECT id FROM secret */",
            "SELECT 1 /*!50000 UNION SELECT id FROM secret */",
            "SELECT 1 /*M! UNION SELECT id FROM secret */",
            "SELECT 1 /*M!100100 UNION SELECT id FROM secret */",
            "/*! SELECT id FROM secret */",
            "SELECT * FROM allowed /*! UNION SELECT id FROM secret */",
            "SELECT 2--1 /*! UNION SELECT id FROM secret */",
            "SELECT /*! '*/' AS value FROM secret */",
            "SELECT /*!1+1 FROM secret */",
            "--\f 'ignore this\nSELECT 1 /*! UNION SELECT id FROM secret */",
            "# 'ignore this\nSELECT 1 /*! UNION SELECT id FROM secret */",
            "/* 'ignore this */ SELECT 1 /*! UNION SELECT id FROM secret */"
        )) {
            Assertions.assertNotEquals(sql, SQLUtils.prepareQueryForParsing(MYSQL_LIKE_DIALECT, sql), sql);
            var statement = SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, sql);
            Assertions.assertTrue(new TablesNamesFinder<>().getTables(statement).contains("secret"), sql);
        }
    }

    @Test
    public void skipsInactiveCommentsAndPreservesOffsets() throws Exception {
        String sql = "SELECT 1\n/*!80037 UNION SELECT id FROM secret */";
        String expanded = SQLUtils.prepareQueryForParsing(MYSQL_LIKE_DIALECT, sql);
        Assertions.assertEquals(sql.length(), expanded.length());
        Assertions.assertEquals(sql.indexOf('\n'), expanded.indexOf('\n'));
        Assertions.assertEquals(Set.of(), new TablesNamesFinder<>().getTables(
            SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, sql)));
        Assertions.assertEquals(Set.of("secret"), new TablesNamesFinder<>().getTables(
            SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, sql.replace("80037", "80036"))));
    }

    @Test
    public void rejectsUnverifiableOrMalformedExecutableComments() {
        for (String sql : List.of(
            "SELECT 1 /*! UNION SELECT id FROM secret",
            "SELECT 1 /*!1234567 UNION SELECT id FROM secret */",
            "SELECT 1 /*! unsupported syntax FROM secret */"
        )) {
            Assertions.assertThrows(DBCException.class,
                () -> SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, sql), sql);
        }
        SQLDialect unknownServer = new BasicSQLDialect() {
            @NotNull
            @Override
            public List<String> getExecutableCommentPrefixes() {
                return List.of("/*!");
            }
        };
        Assertions.assertThrows(DBCException.class,
            () -> SQLSemanticProcessor.parseQuery(unknownServer, "SELECT 1 /*!80000 UNION SELECT id FROM secret */"));
    }

    @Test
    public void ignoresMarkersInQuotedTextAndInertComments() throws Exception {
        for (String sql : List.of(
            "SELECT '/*! UNION SELECT id FROM secret */'",
            "SELECT 'it''s /*! not executable */'",
            "SELECT 'it\\'s /*! not executable */'",
            "SELECT `/*! not executable */` FROM allowed",
            "SELECT `name``/*! not executable */` FROM allowed",
            "SELECT \"/*M! not executable */\" FROM allowed",
            "SELECT 1"
        )) {
            Assertions.assertEquals(sql, SQLUtils.prepareQueryForParsing(MYSQL_LIKE_DIALECT, sql), sql);
        }
        for (String sql : List.of(
            "SELECT 1 /* /*! not executable */",
            "SELECT 1 /*M ! not executable */",
            "SELECT 1 /*+ MAX_EXECUTION_TIME(1000) */",
            "SELECT 1 # /*! not executable */",
            "SELECT 1 -- /*! not executable */",
            "SELECT 1 --\t/*! not executable */",
            "SELECT 1 --\f/*! not executable */",
            "SELECT 1 # ' /*! not executable */\n"
        )) {
            Assertions.assertEquals("SELECT 1", SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, sql).toString(), sql);
        }
        Assertions.assertNotNull(SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, "SELECT 1 /* regular comment */"));
    }

    @Test
    public void queryMetadataUsesExecutableSqlAndRetainsOriginalText() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(MYSQL_LIKE_DIALECT);
        String sql = "SELECT 1 /*! UNION SELECT id FROM secret */";
        SQLQuery query = new SQLQuery(dataSource, sql);

        Assertions.assertEquals(SQLQueryType.SELECT, query.getType());
        Assertions.assertEquals(Set.of("secret"), new TablesNamesFinder<>().getTables(query.getStatement()));
        Assertions.assertEquals(sql, query.getText());
    }

    @Test
    public void mutatingAndDangerousChecksUseTheApplicableSql() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(MYSQL_LIKE_DIALECT);
        SQLQuery update = new SQLQuery(dataSource, "/*! UPDATE allowed SET id = 2 */");
        Assertions.assertEquals(SQLQueryType.UPDATE, update.getType());
        Assertions.assertTrue(update.isMutatingStatement());
        Assertions.assertTrue(update.isDeleteUpdateDangerous());
        Assertions.assertTrue(new SQLQuery(dataSource, "DELETE FROM allowed /*!80037 WHERE id = 1 */").isDeleteUpdateDangerous());
        Assertions.assertFalse(new SQLQuery(dataSource, "DELETE FROM allowed /*!80036 WHERE id = 1 */").isDeleteUpdateDangerous());
    }

    @Test
    public void ordinaryDialectsKeepTreatingTheSyntaxAsComments() throws Exception {
        String sql = "SELECT 1 /*! UNION SELECT id FROM secret */";
        Assertions.assertEquals(sql, SQLUtils.prepareQueryForParsing(BasicSQLDialect.INSTANCE, sql));
        Assertions.assertEquals("SELECT 1", SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, sql).toString());
    }

    @Test
    public void preservesDollarQuotedStringsForTheExistingParser() throws Exception {
        for (String sql : List.of(
            "SELECT $$--$$ AS value, id FROM secret",
            "SELECT $$/*$$ AS value, id FROM secret WHERE marker = $$*/$$"
        )) {
            Assertions.assertEquals(sql, SQLUtils.prepareQueryForParsing(BasicSQLDialect.INSTANCE, sql));
            Assertions.assertEquals(Set.of("secret"), new TablesNamesFinder<>().getTables(
                SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, sql)));
        }
    }

    @Test
    public void commentMarkersInStringLiteralsCannotHideTableReferences() throws Exception {
        String sql = "SELECT '/*' FROM secret WHERE marker = '*/'";
        for (SQLDialect dialect : List.of(BasicSQLDialect.INSTANCE, MYSQL_LIKE_DIALECT)) {
            var statement = SQLSemanticProcessor.parseQuery(dialect, sql);
            Assertions.assertEquals(Set.of("secret"), new TablesNamesFinder<>().getTables(statement));
            Assertions.assertTrue(statement.toString().contains("'/*'"));
            Assertions.assertTrue(statement.toString().contains("'*/'"));
        }
    }

    @Test
    public void inertCommentsRemainTokenSeparators() throws Exception {
        var statement = SQLSemanticProcessor.parseQuery(MYSQL_LIKE_DIALECT, "SELECT id/**/FROM allowed");
        Assertions.assertEquals(Set.of("allowed"), new TablesNamesFinder<>().getTables(statement));
    }
}
