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
package org.jkiss.dbeaver.model.sql.completion.usage;

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

public class SQLCompletionUsageModelTest extends DBeaverUnitTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final String ORDERS = SQLUsageQuery.tableKey("s", "orders");
    private static final String ITEMS = SQLUsageQuery.tableKey("s", "items");
    private static final String CUSTOMERS = SQLUsageQuery.tableKey("s", "customers");
    private static final String ACCOUNTS = SQLUsageQuery.tableKey("s", "accounts");
    private static final List<String> TABLES = List.of(ACCOUNTS, CUSTOMERS, ITEMS, ORDERS);
    private static final SQLUsageContext AFTER_FROM = new SQLUsageContext(List.of(), List.of("SELECT", "FROM"));

    @NotNull
    private static SQLUsageQuery select(@NotNull String table, @NotNull String... joined) {
        SQLUsageQuery query = new SQLUsageQuery();
        query.addKeyword("SELECT");
        query.addColumn(SQLUsageQuery.columnKey("s", table.substring(table.lastIndexOf('.') + 1), "id"));
        query.addKeyword("FROM");
        query.addTable(table);
        for (String other : joined) {
            query.addKeyword("JOIN");
            query.addTable(other);
        }
        return query;
    }

    @Test
    public void keepsOrderWithoutUsage() {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        Assertions.assertEquals(TABLES, model.rank(AFTER_FROM, TABLES, 0));
    }

    @Test
    public void ranksUsedObjectsFirst() {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        model.learnQuery(select(ORDERS), 0);
        model.learnQuery(select(ORDERS), 1);
        model.learnQuery(select(ITEMS), 2);
        // Used ones first by usage, the others in their original order
        Assertions.assertEquals(List.of(ORDERS, ITEMS, ACCOUNTS, CUSTOMERS), model.rank(AFTER_FROM, TABLES, 3));
    }

    @Test
    public void recentUseWinsOverOldUse() {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        for (int i = 0; i < 3; i++) {
            model.learnQuery(select(ITEMS), i);
        }
        // Three uses a month ago weigh less than two uses today
        long now = 30 * DAY;
        model.learnQuery(select(ORDERS), now);
        model.learnQuery(select(ORDERS), now);
        Assertions.assertEquals(ORDERS, model.rank(AFTER_FROM, TABLES, now).getFirst());
    }

    @Test
    public void suggestsJoinedTables() {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        model.learnQuery(select(ORDERS, CUSTOMERS), 0);
        for (int i = 0; i < 5; i++) {
            model.learnQuery(select(ITEMS), 1);
        }
        // After "FROM orders JOIN", the table joined with orders before comes first, although items is used more
        SQLUsageContext afterJoin = new SQLUsageContext(List.of(ORDERS), List.of("SELECT", "FROM", ORDERS, "JOIN"));
        Assertions.assertEquals(CUSTOMERS, model.rank(afterJoin, List.of(ACCOUNTS, CUSTOMERS, ITEMS), 2).getFirst());
    }

    @Test
    public void learnsFromChoices() {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        model.learnQuery(select(ORDERS), 0);
        model.learnQuery(select(ITEMS), 0);
        List<String> shown = model.rank(AFTER_FROM, TABLES, 1);
        for (int i = 0; i < 20; i++) {
            model.learnChoice(AFTER_FROM, shown, ITEMS, 1);
        }
        // Weights stay non-negative: used objects are still above unused ones
        List<String> ranked = model.rank(AFTER_FROM, TABLES, 1);
        Assertions.assertEquals(List.of(ACCOUNTS, CUSTOMERS), ranked.subList(2, 4));
    }

    @Test
    public void writesAndReadsModel() throws Exception {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        model.learnQuery(select(ORDERS, CUSTOMERS), 0);
        model.learnQuery(select(ITEMS), 1);
        StringWriter out = new StringWriter();
        model.write(out, 2);

        SQLCompletionUsageModel restored = new SQLCompletionUsageModel(7 * DAY);
        restored.read(new StringReader(out.toString()));
        Assertions.assertEquals(model.rank(AFTER_FROM, TABLES, 2), restored.rank(AFTER_FROM, TABLES, 2));
        SQLUsageContext afterJoin = new SQLUsageContext(List.of(ORDERS), List.of("SELECT", "FROM", ORDERS, "JOIN"));
        Assertions.assertEquals(model.rank(afterJoin, TABLES, 2), restored.rank(afterJoin, TABLES, 2));
    }

    @Test
    public void proposesTablesUsedMost() throws Exception {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        SQLUsageQuery query = new SQLUsageQuery();
        query.addKeyword("FROM");
        query.addTable(ITEMS, List.of("S", "ITEMS"));
        model.learnQuery(query, 0);
        query = new SQLUsageQuery();
        query.addKeyword("FROM");
        query.addTable(ORDERS, List.of("S", "ORDERS"));
        model.learnQuery(query, 1);
        model.learnQuery(query, 2);
        Assertions.assertEquals(List.of(List.of("S", "ORDERS"), List.of("S", "ITEMS")), model.getTopTables(5, 3));
        Assertions.assertEquals(List.of(List.of("S", "ORDERS")), model.getTopTables(1, 3));

        // Paths and schema usage survive a restart
        StringWriter out = new StringWriter();
        model.write(out, 3);
        SQLCompletionUsageModel restored = new SQLCompletionUsageModel(7 * DAY);
        restored.read(new StringReader(out.toString()));
        Assertions.assertEquals(model.getTopTables(5, 3), restored.getTopTables(5, 3));
        List<String> schemas = List.of(SQLUsageQuery.schemaKey("other"), SQLUsageQuery.schemaKey("S"));
        Assertions.assertEquals(SQLUsageQuery.schemaKey("s"), restored.rank(AFTER_FROM, schemas, 3).getFirst());
    }

    @Test
    public void choiceCountsAtOnce() {
        SQLCompletionUsageModel model = new SQLCompletionUsageModel(7 * DAY);
        model.learnUse(List.of(CUSTOMERS), 0);
        Assertions.assertEquals(CUSTOMERS, model.rank(AFTER_FROM, TABLES, 1).getFirst());
    }

    @Test
    public void columnKeysBelongToTheirTable() {
        Assertions.assertEquals(ORDERS, SQLUsageQuery.tableOf(SQLUsageQuery.columnKey("S", "Orders", "ID")));
        Assertions.assertEquals(ORDERS, SQLUsageQuery.tableOf(ORDERS));
        Assertions.assertNull(SQLUsageQuery.tableOf("SELECT"));
    }
}
