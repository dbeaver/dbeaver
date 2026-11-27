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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SQLQueryNonStandardOperatorsTest extends DBeaverUnitTest {

    private static final SQLDialect MYSQL_LIKE_DIALECT = new BasicSQLDialect() {
        @NotNull
        @Override
        public String[] getSingleLineComments() {
            return new String[]{"-- ", "#"};
        }

        @Nullable
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"`", "`"}, {"\"", "\""}};
        }

        @Override
        public char getStringEscapeCharacter() {
            return '\\';
        }
    };

    private static final SQLDialect SQL_SERVER_LIKE_DIALECT = new BasicSQLDialect() {
        @Nullable
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"[", "]"}, {"\"", "\""}};
        }
    };

    @NotNull
    private static String mask(@NotNull String sql) {
        return SQLUtils.maskNonStandardOperators(BasicSQLDialect.INSTANCE, sql);
    }

    @Test
    public void prefixOperatorShouldNotHideSourceTable() {
        // dbeaver/dbeaver#43110
        SQLQuery query = new SQLQuery(
            null,
            "SELECT rowid, transactiondate, amount::MONEY FROM accounts.transactions " +
                "WHERE description ~* 'fee' AND @(amount::NUMERIC)=95.00 ORDER BY transactiondate DESC"
        );

        DBCEntityMetaData entityMeta = query.getEntityMetadata(false);
        Assertions.assertNotNull(entityMeta);
        Assertions.assertNull(entityMeta.getCatalogName());
        Assertions.assertEquals("accounts", entityMeta.getSchemaName());
        Assertions.assertEquals("transactions", entityMeta.getEntityName());

        Assertions.assertEquals(3, query.getSelectItemCount());
        Assertions.assertTrue(query.getSelectItem(0).isPlainColumn());
        Assertions.assertEquals("rowid", query.getSelectItem(0).getName());
        Assertions.assertFalse(query.getSelectItem(2).isPlainColumn());
    }

    @Test
    public void queryWithMaskedOperatorsShouldRemainUnparsed() {
        SQLQuery query = new SQLQuery(null, "SELECT id FROM orders WHERE @(total) > 10");

        Assertions.assertNotNull(query.getEntityMetadata(false));
        // The masked text has other operators, nobody may take it for the query itself
        Assertions.assertNull(query.getStatement());
        Assertions.assertNotNull(query.getParseError());
        Assertions.assertEquals(SQLQueryType.UNKNOWN, query.getType());
        Assertions.assertFalse(query.isPlainSelect());
    }

    @Test
    public void otherNonStandardOperatorsShouldNotHideSourceTable() {
        for (String sql : List.of(
            "SELECT id FROM orders WHERE |/ total = 5",
            "SELECT id FROM orders WHERE ||/ total = 3",
            "SELECT id FROM orders WHERE @-@ route > 3",
            "SELECT id FROM orders WHERE # route = 4",
            "SELECT id FROM orders WHERE name ^@ 'x'",
            "SELECT id FROM orders WHERE @-5 = total"
        )) {
            DBCEntityMetaData entityMeta = new SQLQuery(null, sql).getEntityMetadata(false);
            Assertions.assertNotNull(entityMeta, sql);
            Assertions.assertEquals("orders", entityMeta.getEntityName(), sql);
        }
    }

    @Test
    public void expressionWithMaskedOperatorShouldNotBecomePlainColumn() {
        SQLQuery query = new SQLQuery(null, "SELECT id, @(total), |/ total AS root FROM orders");

        Assertions.assertNotNull(query.getEntityMetadata(false));
        Assertions.assertEquals(3, query.getSelectItemCount());
        Assertions.assertTrue(query.getSelectItem(0).isPlainColumn());
        Assertions.assertFalse(query.getSelectItem(1).isPlainColumn());
        Assertions.assertFalse(query.getSelectItem(2).isPlainColumn());
        Assertions.assertEquals("root", query.getSelectItem(2).getName());
    }

    @Test
    public void selectItemShouldResolveTableAliasOfMaskedQuery() {
        SQLQuery query = new SQLQuery(null, "SELECT o.id FROM sales.orders o WHERE @(o.total) > 10");

        Assertions.assertNotNull(query.getEntityMetadata(false));
        DBCEntityMetaData itemMeta = query.getSelectItem(0).getEntityMetaData();
        Assertions.assertNotNull(itemMeta);
        Assertions.assertEquals("sales", itemMeta.getSchemaName());
        Assertions.assertEquals("orders", itemMeta.getEntityName());
    }

    @Test
    public void maskedQueryWithSeveralSourcesShouldHaveNoSingleSource() {
        for (String sql : List.of(
            "SELECT o.id FROM orders o JOIN customers c ON c.id = o.customer_id WHERE @(o.total) > 10",
            "SELECT count(*) FROM orders WHERE @(total) > 10 GROUP BY customer_id",
            "SELECT id FROM (SELECT id FROM orders WHERE @(total) > 10) o"
        )) {
            Assertions.assertNull(new SQLQuery(null, sql).getEntityMetadata(false), sql);
        }
    }

    @Test
    public void maskedModifyingQueryShouldKeepItsState() {
        SQLQuery query = new SQLQuery(null, "UPDATE orders SET total = 0 WHERE @(total) > 10");

        Assertions.assertNull(query.getEntityMetadata(false));
        Assertions.assertEquals(0, query.getSelectItemCount());
    }

    @Test
    public void maskShouldReplaceNonStandardOperatorsKeepingLength() {
        Assertions.assertEquals("WHERE +(a) = 1", mask("WHERE @(a) = 1"));
        Assertions.assertEquals("WHERE +   a > 1", mask("WHERE @-@ a > 1"));
        Assertions.assertEquals("WHERE a +   b < 1", mask("WHERE a <-> b < 1"));
        Assertions.assertEquals("SELECT +  a, +   b", mask("SELECT |/ a, ||/ b"));
        Assertions.assertEquals("WHERE a+ 'b'+  'c' = 1", mask("WHERE a->'b'->>'c' = 1"));
    }

    @Test
    public void maskShouldKeepStandardOperators() {
        for (String sql : List.of(
            "SELECT *, a + b - c * d / e % f FROM t WHERE a = 1 AND b <> 2 AND c != 3 AND d <= 4 AND e >= 5 AND f < g AND h > i",
            "SELECT a || b, count(*) FROM t",
            "WHERE a=-1 AND b<>-2 AND c>=+3 AND d*-1<0 AND e||-1 = f AND g - -1 = 0 AND h = ?"
        )) {
            Assertions.assertSame(sql, mask(sql));
        }
    }

    @Test
    public void maskShouldSkipLiteralsIdentifiersAndComments() {
        for (String sql : List.of(
            "WHERE a = '@(b) <-> c'",
            "WHERE \"a@b\" = 1",
            "WHERE a = 1 -- @(b)",
            "WHERE a = 1 /* @(b) <-> c */",
            "WHERE a =/* @ */1",
            "WHERE a =-- @\n1"
        )) {
            Assertions.assertSame(sql, mask(sql));
        }
    }

    @Test
    public void dollarQuotedLiteralShouldNotHideSourceTable() {
        SQLQuery query = new SQLQuery(null, "SELECT $$Joe's$$, id FROM orders WHERE @(total) > 10");

        DBCEntityMetaData entityMeta = query.getEntityMetadata(false);
        Assertions.assertNotNull(entityMeta);
        Assertions.assertEquals("orders", entityMeta.getEntityName());
        Assertions.assertEquals(2, query.getSelectItemCount());
    }

    @Test
    public void maskShouldSkipDollarQuotedLiterals() {
        for (String sql : List.of(
            "SELECT $$Joe's @ home$$ FROM t",
            "SELECT $fn$it's a <-> b$fn$, $_1$ @ $$ @ $_1$ FROM t"
        )) {
            Assertions.assertSame(sql, mask(sql));
        }
        Assertions.assertEquals(
            "SELECT $$Joe's$$, id FROM orders WHERE +(total) > 10",
            mask("SELECT $$Joe's$$, id FROM orders WHERE @(total) > 10")
        );
        // Parameters, identifiers with a dollar sign and an unclosed delimiter don't start a literal
        Assertions.assertEquals(
            "SELECT a$b$ FROM v$session WHERE +(c) = $1 AND +(d) = $2 AND e = $$",
            mask("SELECT a$b$ FROM v$session WHERE @(c) = $1 AND @(d) = $2 AND e = $$")
        );
    }

    @Test
    public void maskShouldSkipDoubledClosingQuotes() {
        Assertions.assertEquals(
            "SELECT a FROM [odd]]@table] WHERE +(b) = 1",
            SQLUtils.maskNonStandardOperators(SQL_SERVER_LIKE_DIALECT, "SELECT a FROM [odd]]@table] WHERE @(b) = 1")
        );
        Assertions.assertEquals(
            "SELECT \"a\"\"@b\", 'it''s @' FROM t WHERE +(c) = 1",
            mask("SELECT \"a\"\"@b\", 'it''s @' FROM t WHERE @(c) = 1")
        );
    }

    @Test
    public void maskShouldRespectDialectQuotesAndComments() {
        for (String sql : List.of(
            "SELECT `a@b` FROM t # @(c)",
            "SELECT 'it\\'s @(a)' FROM t",
            "SELECT a FROM t WHERE b =# @\n1"
        )) {
            Assertions.assertSame(sql, SQLUtils.maskNonStandardOperators(MYSQL_LIKE_DIALECT, sql));
        }
        Assertions.assertEquals(
            "SELECT `a@b` FROM t WHERE +(c) = 1 # @(d)",
            SQLUtils.maskNonStandardOperators(MYSQL_LIKE_DIALECT, "SELECT `a@b` FROM t WHERE @(c) = 1 # @(d)")
        );
        // A comment marker made of operator characters ends the operator before it
        Assertions.assertEquals(
            "SELECT a FROM t WHERE + # @\n(b) = 1",
            SQLUtils.maskNonStandardOperators(MYSQL_LIKE_DIALECT, "SELECT a FROM t WHERE @@# @\n(b) = 1")
        );
    }
}
