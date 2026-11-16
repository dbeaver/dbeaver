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
package org.jkiss.dbeaver.model.ai.functions;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

class SQLScopeHeuristicsTest {
    @Test
    void unparsedStatementStillChecksResolvedObjectScope() throws Exception {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class,
            Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);
        Mockito.when(executionContext.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        AIDatabaseContext context = Mockito.mock(AIDatabaseContext.class);
        Mockito.when(context.getScope()).thenReturn(AIDatabaseScope.CURRENT_SCHEMA);
        Mockito.when(context.isObjectAllowed(Mockito.any())).thenReturn(true);
        DBSSchema schema = Mockito.mock(DBSSchema.class);
        Mockito.when(schema.getName()).thenReturn("private");
        Mockito.when(schema.getDataSource()).thenReturn(dataSource);
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(executionContext.getContextDefaults()).thenReturn(defaults);
        Mockito.doReturn(Mockito.mock(DBSSchema.class)).when(defaults).getDefaultSchema();
        DBSObjectContainer root = (DBSObjectContainer) dataSource;
        Mockito.doReturn(DBSSchema.class).when(root).getPrimaryChildType(Mockito.any());
        Mockito.when(root.getChild(Mockito.any(), Mockito.anyString())).thenAnswer(invocation ->
            "private".equalsIgnoreCase(invocation.getArgument(1)) ? schema : null);
        DBSEntity table = Mockito.mock(DBSEntity.class);
        Mockito.when(table.getName()).thenReturn("payroll");
        Mockito.when(table.getDataSource()).thenReturn(dataSource);
        Mockito.when(table.getParentObject()).thenReturn(schema);
        Mockito.when(schema.getChild(Mockito.any(), Mockito.anyString())).thenAnswer(invocation ->
            "payroll".equalsIgnoreCase(invocation.getArgument(1)) ? table : null);
        SQLQuery query = Mockito.mock(SQLQuery.class);
        Mockito.when(query.getText()).thenReturn("SELECT * FROM private.payroll");
        ExecuteSQLFunction function = new ExecuteSQLFunction();
        AIFunctionResult denied = function.verifyObjectsInScope(query, executionContext, context);
        Assertions.assertTrue(denied.getValue().toString().contains("out of allowed scope"));
        Mockito.verify(schema).getChild(Mockito.any(), Mockito.anyString());

        Mockito.doReturn(schema).when(defaults).getDefaultSchema();
        Assertions.assertNull(function.verifyObjectsInScope(query, executionContext, context));

        Mockito.when(query.getText()).thenReturn("some unrecognized command");
        DBException error = Assertions.assertThrows(DBException.class,
            () -> function.verifyObjectsInScope(query, executionContext, context));
        Assertions.assertTrue(error.getMessage().contains("Cannot determine database objects"));
    }

    @Test
    void rejectsUnknownExplicitCatalogRatherThanUsingDefault() throws DBException {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBSInstance defaultInstance = Mockito.mock(DBSInstance.class);
        DBSInstance selectedInstance = Mockito.mock(DBSInstance.class);
        Mockito.when(dataSource.getDefaultInstance()).thenReturn(defaultInstance);
        Mockito.when(selectedInstance.getName()).thenReturn("selected");
        Mockito.doReturn(List.of(selectedInstance)).when(dataSource).getAvailableInstances();
        Assertions.assertSame(defaultInstance, ExecuteSQLFunction.resolveInstance(dataSource, null));
        Assertions.assertSame(selectedInstance, ExecuteSQLFunction.resolveInstance(dataSource, "selected"));
        Assertions.assertThrows(DBException.class, () -> ExecuteSQLFunction.resolveInstance(dataSource, "unknown"));
    }

    @Test
    void parsedCtesResolveOnlyPhysicalTablesInTheirScope() throws Exception {
        for (QueryCase test : List.of(
            query("CTE", "WITH recent AS (SELECT * FROM orders) SELECT * FROM recent", "orders"),
            query("Qualified table", "WITH recent AS (SELECT * FROM orders) SELECT * FROM app.recent", "orders", "app.recent"),
            query("Nested CTE", "SELECT * FROM (WITH recent AS (SELECT * FROM orders) SELECT * FROM recent) q JOIN recent ON true",
                "orders", "recent"),
            query("Non-recursive shadow", "WITH recent AS (SELECT * FROM recent) SELECT * FROM recent", "recent"),
            query("Recursive CTE", "WITH RECURSIVE recent AS (SELECT * FROM orders UNION ALL SELECT * FROM recent) SELECT * FROM recent",
                "orders")
        )) {
            Set<String> physicalTables = new HashSet<>();
            ExecuteSQLFunction.traverseQueryTables(CCJSqlParserUtil.parse(test.sql()),
                table -> physicalTables.add(table.getFullyQualifiedName()));
            Assertions.assertEquals(test.expected(), physicalTables, test.label());
        }
    }

    @Test
    void findsJoinedAndCommaSeparatedTables() throws DBException {
        Assertions.assertEquals(Set.of("public.orders", "private.payroll", "private.staff"),
            SQLScopeHeuristics.extractTableNames(
                "SELECT a.id FROM public.orders a JOIN private.payroll b ON a.id=b.id, private.staff c"));
    }

    @Test
    void preservesQuotedQualifiedIdentifiers() throws DBException {
        Assertions.assertEquals(Set.of("\"secret.schema\".\"pay\"\"roll\"", "[private].[staff]", "`db`.`table`"),
            SQLScopeHeuristics.extractTableNames(
                "SELECT * FROM \"secret.schema\" . \"pay\"\"roll\", [private].[staff], `db`.`table`"));
    }

    @Test
    void ignoresLiteralAndCommentDecoysButFindsNestedTables() throws DBException {
        Assertions.assertEquals(Set.of("public.orders", "private.payroll"),
            SQLScopeHeuristics.extractTableNames(
                "SELECT 'FROM fake.data' FROM /* JOIN fake.other */ public.orders "
                    + "WHERE id IN (SELECT id FROM private.payroll) -- FROM fake.last\n"));
    }

    @Test
    void findsModificationSourcesAndTargets() throws DBException {
        Assertions.assertEquals(Set.of("public.orders", "private.payroll"),
            SQLScopeHeuristics.extractTableNames("INSERT INTO public.orders SELECT * FROM private.payroll"));
        Assertions.assertEquals(Set.of("private.payroll"),
            SQLScopeHeuristics.extractTableNames("UPDATE private.payroll SET salary = 1"));
        Assertions.assertEquals(Set.of("private.payroll"),
            SQLScopeHeuristics.extractTableNames("DELETE FROM private.payroll WHERE id = 1"));
        Assertions.assertEquals(Set.of("public.orders", "private.payroll"),
            SQLScopeHeuristics.extractTableNames("UPDATE public.orders a, private.payroll b SET a.id=b.id"));
        Assertions.assertEquals(Set.of("public.orders", "private.payroll"),
            SQLScopeHeuristics.extractTableNames("DROP TABLE public.orders, private.payroll"));
    }

    @Test
    void vendorSyntaxAndMalformedInput() {
        Assertions.assertAll(Stream.of(
            query("PostgreSQL cast", "SELECT payload::jsonb->>'name' FROM public.events", "public.events"),
            query("PostgreSQL ONLY", "SELECT * FROM ONLY public.events FOR UPDATE", "public.events"),
            query("PostgreSQL dollar literal", "SELECT $tag$ FROM decoy.table $tag$ FROM public.events", "public.events"),
            query("PostgreSQL CTE", "WITH x AS MATERIALIZED (SELECT * FROM private.payroll) SELECT * FROM x", "private.payroll"),
            query("Recursive CTE",
                "WITH RECURSIVE x(id) AS (SELECT id FROM tree.nodes UNION ALL SELECT id FROM x) SELECT * FROM x", "tree.nodes"),
            query("Multiple CTEs", "WITH x AS (SELECT * FROM s.a), y AS (SELECT * FROM s.b JOIN x ON true) SELECT * FROM y", "s.a", "s.b"),
            query("CTE alias reused as table", "WITH x AS (SELECT * FROM s.a) SELECT * FROM x; SELECT * FROM x", "s.a", "x"),
            query("CREATE TABLE AS", "CREATE TABLE archive.events AS SELECT * FROM public.events", "archive.events", "public.events"),
            query("CREATE VIEW", "CREATE OR REPLACE VIEW reporting.v AS SELECT * FROM private.payroll", "reporting.v", "private.payroll"),
            query("Foreign key DDL", "ALTER TABLE public.orders ADD CONSTRAINT fk FOREIGN KEY (id) REFERENCES private.staff(id)",
                "public.orders", "private.staff"),
            query("Rename", "ALTER TABLE public.orders RENAME TO private.orders", "public.orders", "private.orders"),
            query("Create index", "CREATE INDEX CONCURRENTLY ix ON public.orders (id)", "public.orders"),
            query("Create trigger", "CREATE TRIGGER trg BEFORE INSERT ON public.orders FOR EACH ROW CALL audit.record()",
                "public.orders", "audit.record"),
            query("COPY", "COPY public.orders TO '/tmp/orders.csv'", "public.orders"),
            query("COPY query", "COPY (SELECT * FROM private.staff) TO STDOUT", "private.staff"),
            query("Oracle hierarchical",
                "SELECT * FROM hr.employees START WITH manager_id IS NULL CONNECT BY PRIOR id=manager_id", "hr.employees"),
            query("Oracle q literal", "SELECT q'[it's FROM fake.table]' FROM hr.employees", "hr.employees"),
            query("Oracle flashback", "SELECT * FROM hr.employees AS OF TIMESTAMP :ts", "hr.employees"),
            query("Oracle merge", "MERGE INTO hr.target t USING hr.source s ON (t.id=s.id) WHEN MATCHED THEN UPDATE SET t.x=s.x",
                "hr.target", "hr.source"),
            query("SQL Server hints", "SELECT TOP (10) * FROM [db].[dbo].[Orders] WITH (NOLOCK)", "[db].[dbo].[Orders]"),
            query("SQL Server omitted schema", "SELECT * FROM master..sysobjects", "master..sysobjects"),
            query("SQL Server temp table", "SELECT * INTO #tmp FROM dbo.orders", "#tmp", "dbo.orders"),
            query("SQL Server procedure", "EXEC dbo.read_payroll @id=1", "dbo.read_payroll"),
            query("MySQL insert modifier", "INSERT IGNORE INTO app.orders (id) VALUES (1)", "app.orders"),
            query("MySQL replace", "REPLACE INTO app.orders VALUES (1)", "app.orders"),
            query("MySQL executable comment", "SELECT * FROM public.orders /*!50000 UNION SELECT * FROM private.payroll */",
                "public.orders", "private.payroll"),
            query("MariaDB executable comment", "SELECT * FROM public.orders /*M!100100 UNION SELECT * FROM private.payroll */",
                "public.orders", "private.payroll"),
            query("MySQL multi-update", "UPDATE LOW_PRIORITY app.a, private.b SET app.a.id=private.b.id", "app.a", "private.b"),
            query("DB2 isolation", "SELECT * FROM APP.ORDERS WITH UR", "APP.ORDERS"),
            query("DB2 explain", "EXPLAIN PLAN FOR SELECT * FROM APP.ORDERS", "APP.ORDERS"),
            query("Snowflake qualify", "SELECT * FROM db.schema.orders QUALIFY row_number() OVER (PARTITION BY id ORDER BY ts)=1",
                "db.schema.orders"),
            query("BigQuery quoted path", "SELECT * FROM `my-project.dataset.orders` FOR SYSTEM_TIME AS OF @ts",
                "`my-project.dataset.orders`"),
            query("ClickHouse final", "SELECT * FROM analytics.events FINAL PREWHERE id=1 SETTINGS max_threads=2", "analytics.events"),
            query("Hive overwrite", "INSERT OVERWRITE TABLE warehouse.target SELECT * FROM warehouse.source",
                "warehouse.target", "warehouse.source"),
            query("SQLite pragma", "PRAGMA table_info('orders')"),
            query("Cassandra CQL", "SELECT * FROM ks.events WHERE id=? ALLOW FILTERING", "ks.events"),
            query("Cassandra batch", "BEGIN BATCH UPDATE ks.a SET x=1 WHERE id=1; INSERT INTO ks.b (id) VALUES (1); APPLY BATCH",
                "ks.a", "ks.b"),
            query("DynamoDB PartiQL", "SELECT * FROM \"Orders\" WHERE pk='x'", "\"Orders\""),
            query("Couchbase SQL++", "SELECT META(o).id FROM `bucket`.`scope`.`orders` o USE KEYS ['id']", "`bucket`.`scope`.`orders`"),
            query("Mongo find", "db.orders.find({status: 'open'})", "orders"),
            query("Mongo quoted collection", "db.getCollection('order.items').find({})", "\"order.items\""),
            query("Mongo sibling database", "db.getSiblingDB('private').getCollection('payroll').find({})", "\"private\".\"payroll\""),
            query("Mongo command", "db.runCommand({find: 'payroll', filter: {id: 1}})", "\"payroll\""),
            query("Mongo lookup", "db.orders.aggregate([{$lookup: {from: 'staff', localField:'id', foreignField:'id', as:'staff'}}])",
                "orders", "\"staff\""),
            query("Mongo sibling lookup", "db.getSiblingDB('private').orders.aggregate([{$lookup:{from:'staff',as:'staff'}}])",
                "\"private\".orders", "\"private\".\"staff\""),
            query("Mongo dynamic collection", "db.getCollection(collectionName).find({})"),
            query("Redis", "HGETALL customer:123"),
            query("Redis script", "EVAL \"return redis.call('GET', KEYS[1])\" 1 key"),
            query("Neo4j Cypher", "MATCH (p:Person)-[:WORKS_AT]->(c:Company) RETURN p,c"),
            query("Gremlin", "g.V().hasLabel('person').valueMap()"),
            query("Elasticsearch request", "GET /orders/_search {\"query\":{\"match_all\":{}}}"),
            query("InfluxQL", "SELECT * FROM \"metrics\".\"autogen\".\"cpu\" WHERE time > now() - 1h", "\"metrics\".\"autogen\".\"cpu\""),
            query("Flux", "from(bucket: \"metrics\") |> range(start: -1h)"),
            query("Dynamic SQL literal", "EXECUTE IMMEDIATE 'SELECT * FROM private.payroll'", "private.payroll"),
            query("Routine source", "CREATE FUNCTION f() RETURNS SETOF int AS $$SELECT id FROM private.payroll$$ LANGUAGE sql",
                "private.payroll"),
            query("Qualified routine", "SELECT private.read_payroll() FROM public.orders", "private.read_payroll", "public.orders"),
            query("Table function", "SELECT * FROM private.read_payroll()", "private.read_payroll"),
            query("Missing parentheses", "SELECT * FROM public.orders WHERE id IN (SELECT id FROM private.payroll",
                "public.orders", "private.payroll"),
            query("Broken select", "SELCT ??? FROM private.payroll WHER @@@", "private.payroll"),
            query("Extra parentheses", ")) SELECT * FROM private.payroll ))", "private.payroll"),
            query("Truncated qualification", "SELECT * FROM private."),
            query("Unterminated identifier", "SELECT * FROM \"unterminated"),
            query("Unterminated string", "SELECT 'not SQL FROM private.payroll"),
            query("Unterminated dollar string", "SELECT $body$not SQL FROM private.payroll"),
            query("Unterminated comment", "/* FROM private.payroll"),
            query("Nested comments", "/* outer /* FROM fake.a */ FROM fake.b */ SELECT * FROM public.orders", "public.orders"),
            query("Comment-only", "-- FROM secret.table\n/* JOIN fake.table */"),
            query("Unicode", "SELECT * FROM \"схема\".\"таблица\"", "\"схема\".\"таблица\""),
            query("Whitespace", " \t\r\n"),
            query("Empty", ""),
            query("No relations", "SELECT 1"),
            query("Missing relation", "SELECT * FROM"),
            query("Gibberish", "☃ @@@ ??? ! 12345 >>>")
        ).map(test -> () -> Assertions.assertEquals(
            test.expected(), SQLScopeHeuristics.extractTableNames(test.sql()), test.label() + ": " + test.sql())));
    }

    @Test
    void avoidsFalsePositiveReferences() {
        Assertions.assertAll(Stream.of(
            query("Sort direction", "SELECT * FROM app.orders ORDER BY id DESC NULLS LAST", "app.orders"),
            query("Scalar EXTRACT", "SELECT EXTRACT(YEAR FROM created_at) FROM app.orders", "app.orders"),
            query("Scalar TRIM", "SELECT TRIM(BOTH 'x' FROM customer_name) FROM app.orders", "app.orders"),
            query("Index access method", "CREATE INDEX ix ON app.orders USING btree (id)", "app.orders"),
            query("Variable assignment", "SELECT id INTO @order_id FROM app.orders", "app.orders"),
            query("Output file", "SELECT * INTO OUTFILE '/tmp/orders.csv' FROM app.orders", "app.orders"),
            query("COPY STDIN", "COPY app.orders FROM STDIN", "app.orders"),
            query("Conflict update", "INSERT INTO app.orders VALUES (1) ON CONFLICT (id) DO UPDATE SET id=1", "app.orders"),
            query("Duplicate key update", "INSERT INTO app.orders VALUES (1) ON DUPLICATE KEY UPDATE id=1", "app.orders"),
            query("Qualified column names", "SELECT o.from, o.table, o.into FROM app.orders o", "app.orders"),
            query("Derived table", "SELECT d.id FROM (SELECT id FROM app.orders) d", "app.orders"),
            query("UNNEST", "SELECT * FROM app.orders o CROSS JOIN UNNEST(o.items) item", "app.orders"),
            query("Lateral JSON", "SELECT * FROM app.orders o CROSS JOIN LATERAL jsonb_to_recordset(o.items) AS x(id int)", "app.orders"),
            query("Quoted case-sensitive CTE", "WITH \"X\" AS (SELECT * FROM app.orders) SELECT * FROM \"x\"", "app.orders", "\"x\""),
            query("Mongo ordinary filter fields", "db.orders.find({from:'not-a-collection', count:'not-a-collection'})", "orders"),
            query("Mongo nested filter fields", "db.runCommand({find:'orders',filter:{find:'not-a-collection',from:'not-a-collection'}})",
                "\"orders\""),
            query("Mongo aggregation match", "db.orders.aggregate([{$match:{from:'not-a-collection'}},{$lookup:{from:'staff',as:'s'}}])",
                "orders", "\"staff\""),
            query("Empty FROM", "SELECT * FROM WHERE id=1"),
            query("Unterminated name preserves known source", "SELECT * FROM app.orders JOIN \"unfinished", "app.orders")
        ).map(test -> () -> Assertions.assertEquals(
            test.expected(), SQLScopeHeuristics.extractTableNames(test.sql()), test.label() + ": " + test.sql())));
    }

    @Test
    void toleratesRandomBrokenInputAndLongTokens() {
        Random random = new Random(11492);
        String alphabet = "abcXYZ09_.$#@'\"`[]{}()/*-;,:?\\ \r\n☃";
        for (int sample = 0; sample < 1_000; sample++) {
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(512); i > 0; i--) {
                text.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            Assertions.assertDoesNotThrow(() -> SQLScopeHeuristics.extractTableNames(text.toString()));
        }
        String identifier = "x".repeat(100_000);
        Assertions.assertEquals(Set.of(identifier), SQLScopeHeuristics.extractTableNames("SELECT * FROM " + identifier));
        Assertions.assertEquals(Set.of("\"" + identifier + "\""),
            SQLScopeHeuristics.extractTableNames("SELECT * FROM \"" + identifier + "\""));
    }

    @NotNull
    private static QueryCase query(@NotNull String label, @NotNull String sql, @NotNull String... names) {
        return new QueryCase(label, sql, Set.of(names));
    }

    private record QueryCase(@NotNull String label, @NotNull String sql, @NotNull Set<String> expected) {
    }
}
