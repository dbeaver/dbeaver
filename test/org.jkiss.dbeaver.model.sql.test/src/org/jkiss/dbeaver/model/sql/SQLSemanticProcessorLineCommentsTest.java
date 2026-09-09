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

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SQLSemanticProcessorLineCommentsTest extends DBeaverUnitTest {

    private static final SQLDialect MYSQL_LIKE_DIALECT = new BasicSQLDialect() {
        @NotNull
        @Override
        public String[] getSingleLineComments() {
            return new String[]{"-- ", "--\t", "#"};
        }

        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"`", "`"}, {"\"", "\""}};
        }

        @Override
        public char getStringEscapeCharacter() {
            return '\\';
        }
    };

    private static final SQLDialect BRACKET_DIALECT = new BasicSQLDialect() {
        @NotNull
        @Override
        public String[] getSingleLineComments() {
            return new String[]{"-- ", "#"};
        }

        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"[", "]"}};
        }
    };

    @NotNull
    private PlainSelect parseSelect(@NotNull String sql) throws Exception {
        return parseSelect(MYSQL_LIKE_DIALECT, sql);
    }

    @NotNull
    private PlainSelect parseSelect(@NotNull SQLDialect dialect, @NotNull String sql) throws Exception {
        Statement statement = SQLSemanticProcessor.parseQuery(dialect, sql);
        Assertions.assertInstanceOf(PlainSelect.class, statement, "Not a plain select: " + sql);
        return (PlainSelect) statement;
    }

    @NotNull
    private Table getSourceTable(@NotNull PlainSelect select) {
        Assertions.assertInstanceOf(Table.class, select.getFromItem());
        return (Table) select.getFromItem();
    }

    @Test
    public void trailingHashComment() throws Exception {
        PlainSelect select = parseSelect("SELECT * FROM actor # comment");
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void trailingHashCommentWithoutSpaceIsNotAnAlias() throws Exception {
        PlainSelect select = parseSelect("SELECT * FROM actor #comment");
        Table table = getSourceTable(select);
        Assertions.assertEquals("actor", table.getName());
        Assertions.assertNull(table.getAlias());
    }

    @Test
    public void hashCommentInTheMiddle() throws Exception {
        PlainSelect select = parseSelect("SELECT * FROM actor\n# comment\nWHERE actor_id > 10");
        Assertions.assertEquals("actor", getSourceTable(select).getName());
        Assertions.assertNotNull(select.getWhere());
    }

    @Test
    public void leadingHashComment() throws Exception {
        PlainSelect select = parseSelect("# comment\nSELECT * FROM actor");
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void hashInsideStringLiteralIsPreserved() throws Exception {
        PlainSelect select = parseSelect("SELECT '# not a comment' AS c FROM actor # tail");
        Assertions.assertTrue(select.toString().contains("# not a comment"));
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void hashInsideQuotedIdentifierIsPreserved() throws Exception {
        PlainSelect select = parseSelect("SELECT `weird#name` FROM actor # tail");
        Assertions.assertTrue(select.toString().contains("`weird#name`"));
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void hashInsideBackslashEscapedStringIsPreserved() throws Exception {
        PlainSelect select = parseSelect("SELECT 'a\\'#b' AS c FROM actor # tail");
        Assertions.assertTrue(select.toString().contains("#b"), select.toString());
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void backslashEscapedQuoteBeforeHashComment() throws Exception {
        PlainSelect select = parseSelect("SELECT * FROM actor WHERE name = 'O\\'Brien' # tail");
        Assertions.assertEquals("actor", getSourceTable(select).getName());
        Assertions.assertNotNull(select.getWhere());
    }

    @Test
    public void hashInsideBracketQuotedIdentifierIsPreserved() throws Exception {
        PlainSelect select = parseSelect(BRACKET_DIALECT, "SELECT [weird#name] FROM actor # tail");
        Assertions.assertTrue(select.toString().contains("[weird#name]"));
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void hashAfterStringWithDoubledQuote() throws Exception {
        PlainSelect select = parseSelect("SELECT 'it''s' AS c FROM actor # tail");
        Assertions.assertEquals("actor", getSourceTable(select).getName());
    }

    @Test
    public void hashAfterStandardCommentWithUnbalancedQuote() throws Exception {
        PlainSelect select = parseSelect("SELECT * FROM actor -- don't mind me\nWHERE actor_id > 10 # tail");
        Assertions.assertEquals("actor", getSourceTable(select).getName());
        Assertions.assertNotNull(select.getWhere());
    }

    @Test
    public void standardDialectIsUnaffected() throws Exception {
        Statement statement = SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "SELECT * FROM actor -- comment");
        Assertions.assertInstanceOf(PlainSelect.class, statement);
    }
}
