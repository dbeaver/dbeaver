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

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectRelational;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.sql.transformers.SQLQueryTransformerCount;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Locale;

public class SQLQueryTransformerCountTest extends DBeaverUnitTest {

    @Test
    public void acceptsCommonTableExpressionSelect() throws Exception {
        Statement statement = SQLSemanticProcessor.parseQuery(
            BasicSQLDialect.INSTANCE,
            "WITH active_users AS (SELECT id FROM users WHERE active = 1) SELECT id FROM active_users"
        );

        Assertions.assertTrue(SQLQueryTransformerCount.isCountableSelect(statement));
    }

    @Test
    public void acceptsNonPlainSelectSyntaxTrees() {
        ParenthesedSelect statement = new ParenthesedSelect();

        Assertions.assertFalse(PlainSelect.class.isInstance(statement));
        Assertions.assertTrue(SQLQueryTransformerCount.isCountableSelect(statement));
    }

    @Test
    public void injectsCountIntoParenthesizedMySQLSelectWithDuplicateColumnNames() throws Exception {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialectRelational dialect = Mockito.mock(SQLDialectRelational.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.supportsSubqueries()).thenReturn(true);
        Mockito.when(dialect.isAmbiguousCountBroken()).thenReturn(true);

        SQLQuery query = new SQLQuery(dataSource, "(SELECT 1 AS x, 2 AS x)");
        SQLQuery counted = new SQLQueryTransformerCount().transformQuery(
            dataSource, Mockito.mock(SQLSyntaxManager.class), query);

        String countText = counted.getText().toLowerCase(Locale.ROOT);
        Assertions.assertTrue(countText.contains("count(*)"), countText);
        Assertions.assertFalse(countText.contains("dbvrcnt"), countText);
    }

    @Test
    public void removesOuterOrderingWhenInjectingCountIntoParenthesizedSelect() throws Exception {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialectRelational dialect = Mockito.mock(SQLDialectRelational.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.supportsSubqueries()).thenReturn(true);
        Mockito.when(dialect.isAmbiguousCountBroken()).thenReturn(true);

        // ORDER BY belongs to the enclosing ParenthesedSelect, not the inner
        // PlainSelect. The alias x disappears when the output becomes COUNT(*).
        SQLQuery query = new SQLQuery(dataSource, "(SELECT id AS x FROM t) ORDER BY x");
        SQLQuery counted = new SQLQueryTransformerCount().transformQuery(
            dataSource, Mockito.mock(SQLSyntaxManager.class), query);

        String countText = counted.getText().toLowerCase(Locale.ROOT);
        Assertions.assertTrue(countText.contains("count(*)"), countText);
        Assertions.assertFalse(countText.contains("order by"), countText);
        Assertions.assertFalse(countText.contains("dbvrcnt"), countText);
    }

    @Test
    public void acceptsUnionSelect() throws Exception {
        Statement statement = SQLSemanticProcessor.parseQuery(
            BasicSQLDialect.INSTANCE,
            "SELECT id FROM users UNION ALL SELECT id FROM archived_users"
        );

        Assertions.assertTrue(SQLQueryTransformerCount.isCountableSelect(statement));
    }

    @Test
    public void rejectsNonSelectStatements() throws Exception {
        Statement update = SQLSemanticProcessor.parseQuery(
            BasicSQLDialect.INSTANCE,
            "UPDATE users SET active = 0 WHERE id = 1"
        );
        Statement delete = SQLSemanticProcessor.parseQuery(
            BasicSQLDialect.INSTANCE,
            "DELETE FROM users WHERE id = 1"
        );

        Assertions.assertFalse(SQLQueryTransformerCount.isCountableSelect(update));
        Assertions.assertFalse(SQLQueryTransformerCount.isCountableSelect(delete));
        Assertions.assertFalse(SQLQueryTransformerCount.isCountableSelect(null));
    }
}
