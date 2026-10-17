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

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.update.Update;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class SQLSemanticProcessorQuotedIdentifierTest extends DBeaverUnitTest {

    private static final Map<List<String>, List<String>> TABLE_NAME_QUERIES = Map.ofEntries(
        Map.entry(List.of("s", "t"), List.of(
            "select * from s.t",
            "select * from s.t x",
            "select * from s.t as x"
        )),
        Map.entry(List.of("s", "q"), List.of(
            "select * from s.q",
            "select * from s.q, t",
            "select * from s.q, t y",
            "select * from s.q x",
            "select * from s.q x, t",
            "select * from s.q x, t y",
            "select * from s.q as x",
            "select * from s.q as x, t",
            "select * from s.q as x, t y"
        )),
        Map.entry(List.of("c", "s", "t"), List.of(
            "select * from c.s.t",
            "select * from c.s.t x",
            "select * from c.s.t as x"
        )),
        Map.entry(List.of("\"a.b.c\""), List.of(
            "select * from \"a.b.c\"",
            "select * from \"a.b.c\", t",
            "select * from \"a.b.c\", t y",
            "select * from \"a.b.c\" x",
            "select * from \"a.b.c\" x, t",
            "select * from \"a.b.c\" x, t y",
            "select * from \"a.b.c\" as x",
            "select * from \"a.b.c\" as x, t",
            "select * from \"a.b.c\" as x, t y"
        )),
        Map.entry(List.of("s", "\"a.b.c\""), List.of(
            "select * from s.\"a.b.c\"",
            "select * from s.\"a.b.c\", t",
            "select * from s.\"a.b.c\", t y",
            "select * from s.\"a.b.c\" x",
            "select * from s.\"a.b.c\" x, t",
            "select * from s.\"a.b.c\" x, t y",
            "select * from s.\"a.b.c\" as x",
            "select * from s.\"a.b.c\" as x, t",
            "select * from s.\"a.b.c\" as x, t y"
        )),
        Map.entry(List.of("\"x.y.z\"", "q"), List.of(
            "select * from \"x.y.z\".q",
            "select * from \"x.y.z\".q, t",
            "select * from \"x.y.z\".q, t y",
            "select * from \"x.y.z\".q x",
            "select * from \"x.y.z\".q x, t",
            "select * from \"x.y.z\".q x, t y",
            "select * from \"x.y.z\".q as x",
            "select * from \"x.y.z\".q as x, t",
            "select * from \"x.y.z\".q as x, t y"
        )),
        Map.entry(List.of("\"x.y.z\"", "\"a.b.c\""), List.of(
            "select * from \"x.y.z\".\"a.b.c\"",
            "select * from \"x.y.z\".\"a.b.c\", t",
            "select * from \"x.y.z\".\"a.b.c\", t y",
            "select * from \"x.y.z\".\"a.b.c\" x",
            "select * from \"x.y.z\".\"a.b.c\" x, t",
            "select * from \"x.y.z\".\"a.b.c\" x, t y",
            "select * from \"x.y.z\".\"a.b.c\" as x",
            "select * from \"x.y.z\".\"a.b.c\" as x, t",
            "select * from \"x.y.z\".\"a.b.c\" as x, t y"
        )),
        Map.entry(List.of("c", "s", "\"a.b.c\""), List.of(
            "select * from c.s.\"a.b.c\"",
            "select * from c.s.\"a.b.c\", t",
            "select * from c.s.\"a.b.c\", t y",
            "select * from c.s.\"a.b.c\" x",
            "select * from c.s.\"a.b.c\" x, t",
            "select * from c.s.\"a.b.c\" x, t y",
            "select * from c.s.\"a.b.c\" as x",
            "select * from c.s.\"a.b.c\" as x, t",
            "select * from c.s.\"a.b.c\" as x, t y"
        )),
        Map.entry(List.of("c", "\"x.y.z\"", "q"), List.of(
            "select * from c.\"x.y.z\".q",
            "select * from c.\"x.y.z\".q, t",
            "select * from c.\"x.y.z\".q, t y",
            "select * from c.\"x.y.z\".q x",
            "select * from c.\"x.y.z\".q x, t",
            "select * from c.\"x.y.z\".q x, t y",
            "select * from c.\"x.y.z\".q as x",
            "select * from c.\"x.y.z\".q as x, t",
            "select * from c.\"x.y.z\".q as x, t y"
        )),
        Map.entry(List.of("\"m.n.p\"", "s", "q"), List.of(
            "select * from \"m.n.p\".s.q",
            "select * from \"m.n.p\".s.q, t",
            "select * from \"m.n.p\".s.q, t y",
            "select * from \"m.n.p\".s.q x",
            "select * from \"m.n.p\".s.q x, t",
            "select * from \"m.n.p\".s.q x, t y",
            "select * from \"m.n.p\".s.q as x",
            "select * from \"m.n.p\".s.q as x, t",
            "select * from \"m.n.p\".s.q as x, t y"
        )),
        Map.entry(List.of("\"m.n.p\"", "\"x.y.z\"", "q"), List.of(
            "select * from \"m.n.p\".\"x.y.z\".q",
            "select * from \"m.n.p\".\"x.y.z\".q, t",
            "select * from \"m.n.p\".\"x.y.z\".q, t y",
            "select * from \"m.n.p\".\"x.y.z\".q x",
            "select * from \"m.n.p\".\"x.y.z\".q x, t",
            "select * from \"m.n.p\".\"x.y.z\".q x, t y",
            "select * from \"m.n.p\".\"x.y.z\".q as x",
            "select * from \"m.n.p\".\"x.y.z\".q as x, t",
            "select * from \"m.n.p\".\"x.y.z\".q as x, t y"
        )),
        Map.entry(List.of("c", "\"x.y.z\"", "\"a.b.c\""), List.of(
            "select * from c.\"x.y.z\".\"a.b.c\"",
            "select * from c.\"x.y.z\".\"a.b.c\", t",
            "select * from c.\"x.y.z\".\"a.b.c\", t y",
            "select * from c.\"x.y.z\".\"a.b.c\" x",
            "select * from c.\"x.y.z\".\"a.b.c\" x, t",
            "select * from c.\"x.y.z\".\"a.b.c\" x, t y",
            "select * from c.\"x.y.z\".\"a.b.c\" as x",
            "select * from c.\"x.y.z\".\"a.b.c\" as x, t",
            "select * from c.\"x.y.z\".\"a.b.c\" as x, t y"
        )),
        Map.entry(List.of("\"x.y.z\"", "s", "\"a.b.c\""), List.of(
            "select * from \"x.y.z\".s.\"a.b.c\"",
            "select * from \"x.y.z\".s.\"a.b.c\", t",
            "select * from \"x.y.z\".s.\"a.b.c\", t y",
            "select * from \"x.y.z\".s.\"a.b.c\" x",
            "select * from \"x.y.z\".s.\"a.b.c\" x, t",
            "select * from \"x.y.z\".s.\"a.b.c\" x, t y",
            "select * from \"x.y.z\".s.\"a.b.c\" as x",
            "select * from \"x.y.z\".s.\"a.b.c\" as x, t",
            "select * from \"x.y.z\".s.\"a.b.c\" as x, t y"
        )),
        Map.entry(List.of("\"m.n.p\"", "\"x.y.z\"", "\"a.b.c\""), List.of(
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\"",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\", t",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\", t y",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\" x",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\" x, t",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\" x, t y",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\" as x",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\" as x, t",
            "select * from \"m.n.p\".\"x.y.z\".\"a.b.c\" as x, t y"
        ))
    );

    private static final SQLDialect BRACKET_DIALECT = new BasicSQLDialect() {
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"[", "]"}};
        }
    };

    private static final SQLDialect BIGQUERY_LIKE_DIALECT = new BasicSQLDialect() {
        @Override
        public String[][] getIdentifierQuoteStrings() {
            return new String[][]{{"`", "`"}};
        }

        @Override
        public boolean treatsQuotedIdentifierAsQualifiedName(@NotNull String identifier) {
            return identifier.startsWith("`") && identifier.endsWith("`");
        }
    };

    @Test
    public void tableNamesAreRecognizedAcrossQualificationAndAliasForms() throws Exception {
        for (Map.Entry<List<String>, List<String>> entry : TABLE_NAME_QUERIES.entrySet()) {
            List<String> expectedName = entry.getKey();
            for (String query : entry.getValue()) {
                Table table = getTable(SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, query));
                String actualCatalog = table.getDatabase() == null ? null : table.getDatabase().getDatabaseName();
                String expectedCatalog = expectedName.size() == 3 ? expectedName.get(0) : null;
                String expectedSchema = expectedName.size() >= 2 ? expectedName.get(expectedName.size() - 2) : null;

                Assertions.assertEquals(expectedCatalog, actualCatalog, query);
                Assertions.assertEquals(expectedSchema, table.getSchemaName(), query);
                Assertions.assertEquals(expectedName.getLast(), table.getName(), query);
            }
        }
    }

    /**
     * Documents the JSqlParser behavior that requires our quoted dotted identifier normalization.
     * A quoted identifier such as {@code   "a.b.c"} is expected to remain one table name with no catalog or schema,
     * but JSqlParser presents it as table {@code "c"} in schema {@code "b"} and catalog {@code "a"}.
     *
     * DISABLE THIS TEST IF JSqlParser is fixed to preserve such identifiers as a single table name,
     * then reevaluate whether the corresponding normalization workaround can be removed.
     */
    @Test
    public void rawJSqlParserSplitsQuotedDottedTableNames() throws Exception {
        List<String> queries = TABLE_NAME_QUERIES.get(List.of("\"a.b.c\""));
        Assertions.assertNotNull(queries);
        for (String query : queries) {
            Table table = getTable(CCJSqlParserUtil.parse(query));
            Assertions.assertEquals("\"a\"", table.getDatabase().getDatabaseName(), query);
            Assertions.assertEquals("\"b\"", table.getSchemaName(), query);
            Assertions.assertEquals("\"c\"", table.getName(), query);
            Assertions.assertEquals(List.of("\"c\"", "\"b\"", "\"a\""), table.getNameParts(), query);
        }
    }

    @Test
    public void quotedDottedTableIsSingleName() throws Exception {
        Table table = getTable(SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "SELECT * FROM \"table.name\""));
        Assertions.assertEquals(List.of("\"table.name\""), table.getNameParts());
        Assertions.assertEquals("\"table.name\"", table.getName());
        Assertions.assertNull(table.getSchemaName());
    }

    @Test
    public void nonSelectStatementsNormalizeQuotedDottedTableName() throws Exception {
        CreateTable create = Assertions.assertInstanceOf(
            CreateTable.class,
            SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "CREATE TABLE \"table.name\" (id INT)")
        );
        Update update = Assertions.assertInstanceOf(
            Update.class,
            SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "UPDATE \"table.name\" SET id = 1")
        );
        Delete delete = Assertions.assertInstanceOf(
            Delete.class,
            SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "DELETE FROM \"table.name\"")
        );
        Drop drop = Assertions.assertInstanceOf(
            Drop.class,
            SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "DROP TABLE \"table.name\"")
        );

        for (Table table : List.of(create.getTable(), update.getTable(), delete.getTable(), drop.getName())) {
            Assertions.assertEquals(List.of("\"table.name\""), table.getNameParts());
        }
    }

    @Test
    public void quotedDottedColumnQualifierIsSingleName() throws Exception {
        PlainSelect select = (PlainSelect) SQLSemanticProcessor.parseQuery(
            BasicSQLDialect.INSTANCE,
            "SELECT \"table.name\".column_name FROM source"
        );
        Column column = (Column) select.getSelectItem(0).getExpression();
        Assertions.assertEquals(List.of("\"table.name\""), column.getTable().getNameParts());
        Assertions.assertEquals("\"table.name\".column_name", column.toString());
    }

    @Test
    public void schemaQualifiedDottedComponentIsUnchanged() throws Exception {
        Table table = getTable(SQLSemanticProcessor.parseQuery(
            BasicSQLDialect.INSTANCE,
            "SELECT * FROM schema_name.\"table.name\""
        ));
        Assertions.assertEquals(List.of("\"table.name\"", "schema_name"), table.getNameParts());
    }

    @Test
    public void normalMultipartNameIsUnchanged() throws Exception {
        Table table = getTable(SQLSemanticProcessor.parseQuery(BasicSQLDialect.INSTANCE, "SELECT * FROM catalog.schema.table_name"));
        Assertions.assertEquals(List.of("table_name", "schema", "catalog"), table.getNameParts());
    }

    @Test
    public void bracketQuotedDottedTableIsSingleName() throws Exception {
        Table table = getTable(SQLSemanticProcessor.parseQuery(BRACKET_DIALECT, "SELECT * FROM [table.name]"));
        Assertions.assertEquals(List.of("[table.name]"), table.getNameParts());
    }

    @Test
    public void bigQueryQuotedPathRemainsQualified() throws Exception {
        Table table = getTable(SQLSemanticProcessor.parseQuery(
            BIGQUERY_LIKE_DIALECT,
            "SELECT * FROM `project.dataset.table_name`"
        ));
        Assertions.assertEquals(List.of("\"table_name\"", "\"dataset\"", "\"project\""), table.getNameParts());
    }

    @Test
    public void parseQueriesNormalizesEveryStatement() throws Exception {
        List<Statement> statements = SQLSemanticProcessor.parseQueries(
            BasicSQLDialect.INSTANCE,
            "SELECT * FROM \"first.table\"; CREATE TABLE \"second.table\" (id INT)"
        );
        Assertions.assertEquals(List.of("\"first.table\""), getTable(statements.get(0)).getNameParts());
        CreateTable create = Assertions.assertInstanceOf(CreateTable.class, statements.get(1));
        Assertions.assertEquals(List.of("\"second.table\""), create.getTable().getNameParts());
    }

    @Test
    public void dialectAwareExpressionNormalizesColumnQualifier() throws Exception {
        Expression expression = SQLSemanticProcessor.parseExpression(BRACKET_DIALECT, "[table.name].column_name");
        Column column = Assertions.assertInstanceOf(Column.class, expression);
        Assertions.assertEquals(List.of("[table.name]"), column.getTable().getNameParts());
    }

    @NotNull
    private static Table getTable(@NotNull Statement statement) {
        PlainSelect select = Assertions.assertInstanceOf(PlainSelect.class, statement);
        return Assertions.assertInstanceOf(Table.class, select.getFromItem());
    }
}
