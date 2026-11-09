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
package org.jkiss.dbeaver.model.sql.procedural;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * The eleven-site generic procedural scenario, rendered by {@link ProceduralNestedCompletionScenario}.
 * P0–P10 and indentation are for the reader; the generated SQL has no labels or line breaks.
 * Each SELECT uses the same local {@code scratch(id)} and mocked database {@code tab(order_id, amount)}:
 * <pre>{@code
 * [[prefix]]
 *   DECLARE counter_value int;
 *   DECLARE threshold_value int;
 *   [[temporaryTable]]
 *   SELECT counter_value, threshold_value, s.id, o.order_id
 *     FROM scratch s JOIN tab o ON s.id = o.order_id;                  -- P0
 *   WHILE 1 = 1 [[whileBodyKeyword]]
 *     IF 1 = 1 THEN
 *       BEGIN
 *         DECLARE first_branch int;
 *         SELECT first_branch, s.id, o.order_id
 *           FROM scratch s JOIN tab o ON s.id = o.order_id;           -- P1
 *         LOOP
 *           BEGIN
 *             DECLARE first_loop_value int;
 *             SELECT first_branch, first_loop_value, s.id, o.order_id
 *               FROM scratch s JOIN tab o ON s.id = o.order_id;       -- P2
 *           END;
 *         END LOOP;
 *       END;
 *     ELSE
 *       SELECT counter_value, s.id, o.order_id
 *         FROM scratch s JOIN tab o ON s.id = o.order_id;             -- P3
 *     END IF;
 *     SELECT counter_value, s.id, o.order_id
 *       FROM scratch s JOIN tab o ON s.id = o.order_id;               -- P4
 *   END WHILE;
 *   SELECT counter_value, s.id, o.order_id
 *     FROM scratch s JOIN tab o ON s.id = o.order_id;                 -- P5
 *   IF 1 = 1 THEN
 *     BEGIN
 *       DECLARE second_branch int;
 *       IF 1 = 1 THEN
 *         SELECT second_branch, s.id, o.order_id
 *           FROM scratch s JOIN tab o ON s.id = o.order_id;           -- P6
 *         LOOP
 *           BEGIN
 *             DECLARE second_loop_value int;
 *             SELECT second_branch, second_loop_value, s.id, o.order_id
 *               FROM scratch s JOIN tab o ON s.id = o.order_id;       -- P7
 *           END;
 *         END LOOP;
 *       ELSE
 *         SELECT second_branch, s.id, o.order_id
 *           FROM scratch s JOIN tab o ON s.id = o.order_id;           -- P8
 *       END IF;
 *     END;
 *   ELSE
 *     SELECT counter_value, s.id, o.order_id
 *       FROM scratch s JOIN tab o ON s.id = o.order_id;               -- P9
 *   END IF;
 *   SELECT counter_value, threshold_value, s.id, o.order_id
 *     FROM scratch s JOIN tab o ON s.id = o.order_id;                 -- P10
 * [[suffix]]
 * }</pre>
 * Each dialect chooses its procedure or block wrapper, temporary-table syntax, loop keyword, and suffix;
 * Oracle uses uppercase {@code TAB} in its mocked metadata. The test recognizes this whole program
 * once and requests completion at all eleven offsets in the same model. It checks local {@code id}
 * and variable proposals, including names excluded by lexical scope. A separate, short
 * {@code SELECT | FROM tab} verifies database column metadata once. The intact model checks all
 * eleven embedded SQL statements, the first IF and WHILE model kinds, and positive variable bindings;
 * the two focused tests below also probe negative bindings using a different SQL program.
 * These are structured-parser fixtures, not executable server programs. T-SQL and Firebird use
 * their native control-flow forms in their respective dialect-family tests instead.
 */
public class ProceduralNestedCompletionTest extends ProceduralDialectTestBase {
    private SQLDialect activeDialect;

    @NotNull
    @Override
    protected SQLDialect dialect() {
        return Objects.requireNonNull(this.activeDialect, "Choose the scenario dialect before recognition");
    }

    private void useDialect(@NotNull String dialectId) {
        this.activeDialect = switch (dialectId) {
            case "generic" -> BasicSQLDialect.INSTANCE;
            case "databricks" -> dialectFromBundle(
                "org.jkiss.dbeaver.ext.databricks", "org.jkiss.dbeaver.ext.databricks.DatabricksSQLDialect");
            default -> dialectById(dialectId);
        };
    }

    /**
     * Checks the generic procedure fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void genericNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("generic");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE p AS BEGIN",
            "CREATE TEMP TABLE scratch (id int);", "END", "DO", "tab");
    }

    /**
     * Checks the H2 procedure fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void h2NestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("h2");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE p AS BEGIN",
            "CREATE TEMP TABLE scratch (id int);", "END", "DO", "tab");
    }

    /**
     * Checks the PostgreSQL procedure fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void postgreSqlNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("postgresql");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE p AS BEGIN",
            "CREATE TEMP TABLE scratch (id int);", "END", "LOOP", "tab");
    }

    /**
     * Checks the Oracle procedure fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Uppercase {@code TAB} metadata proposes {@code order_id}.</li>
     *     <li>Uppercase {@code TAB} metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void oracleNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("oracle");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE p AS BEGIN",
            "CREATE GLOBAL TEMPORARY TABLE scratch (id int);", "END", "LOOP", "TAB");
    }

    /**
     * Checks the MySQL procedure fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void mySqlNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("mysql");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE p() BEGIN",
            "CREATE TEMPORARY TABLE scratch (id int);", "END", "DO", "tab");
    }

    /**
     * Checks the Snowflake quoted procedure body:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void snowflakeNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("snowflake");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE p() RETURNS VARCHAR LANGUAGE SQL AS $$ BEGIN",
            "CREATE TEMP TABLE scratch (id int);", "END; $$", "DO", "tab");
    }

    /**
     * Checks the BigQuery procedure fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void bigQueryNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("google_bigquery");
        ProceduralNestedCompletionScenario.verify(this, "CREATE PROCEDURE dataset.p() BEGIN",
            "CREATE TEMP TABLE scratch (id INT64);", "END", "DO", "tab");
    }

    /**
     * Checks the Databricks anonymous block fixture:
     * <ul>
     *     <li>Value variables have lexical visibility.</li>
     *     <li>The root is a programmatical body model.</li>
     *     <li>The body contains eleven embedded SQL statements.</li>
     *     <li>Declaration bindings:
     *         <ul>
     *             <li>{@code counter_value} at P0.</li>
     *             <li>{@code first_branch} at P1.</li>
     *             <li>{@code first_branch} at P2.</li>
     *             <li>{@code first_loop_value} at P2.</li>
     *             <li>{@code second_branch} at P6.</li>
     *             <li>{@code second_loop_value} at P7.</li>
     *         </ul>
     *     </li>
     *     <li>The first WHILE is a conditional model of kind LOOP.</li>
     *     <li>The first IF is a conditional model of kind IF.</li>
     *     <li>Standalone table metadata proposes {@code order_id}.</li>
     *     <li>Standalone table metadata proposes {@code amount}.</li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At each SELECT position:
     *         <ul>
     *             <li>Completion proposes the local {@code id} column.</li>
     *             <li>Local-variable proposals equal the lexically visible set.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    @Test
    void databricksNestedConditionsAndLoopsResolveEveryEmbeddedSelect() throws DBException {
        this.useDialect("databricks");
        ProceduralNestedCompletionScenario.verify(this, "BEGIN",
            "CREATE TEMP TABLE scratch (id int);", "END", "DO", "tab");
    }

    /**
     * Checks generic-procedure binding across all query sites:
     * <ul>
     *     <li>The collected local declarations are exactly:
     *         <ul>
     *             <li>{@code counter_value}.</li>
     *             <li>{@code threshold_value}.</li>
     *             <li>{@code first_branch}.</li>
     *             <li>{@code first_loop_value}.</li>
     *             <li>{@code second_branch}.</li>
     *             <li>{@code second_loop_value}.</li>
     *         </ul>
     *     </li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At every position, the collected references are exactly:
     *         <ul>
     *             <li>{@code counter_value}.</li>
     *             <li>{@code threshold_value}.</li>
     *             <li>{@code first_branch}.</li>
     *             <li>{@code first_loop_value}.</li>
     *             <li>{@code second_branch}.</li>
     *             <li>{@code second_loop_value}.</li>
     *         </ul>
     *     </li>
     *     <li>In-scope references bind to their declarations at each position.</li>
     *     <li>Out-of-scope references do not bind to those declarations at each position.</li>
     * </ul>
     */
    @Test
    void genericNestedLocalVariablesResolveOnlyWithinTheirLexicalScopes() {
        this.useDialect("generic");
        ProceduralNestedCompletionScenario.assertVariableResolution(this, "CREATE PROCEDURE p AS BEGIN",
            "CREATE TEMP TABLE scratch (id int);", "END", "DO");
    }

    /**
     * Checks BigQuery-procedure binding across all query sites:
     * <ul>
     *     <li>The collected local declarations are exactly:
     *         <ul>
     *             <li>{@code counter_value}.</li>
     *             <li>{@code threshold_value}.</li>
     *             <li>{@code first_branch}.</li>
     *             <li>{@code first_loop_value}.</li>
     *             <li>{@code second_branch}.</li>
     *             <li>{@code second_loop_value}.</li>
     *         </ul>
     *     </li>
     *     <li>There are eleven SELECT positions.</li>
     *     <li>At every position, the collected references are exactly:
     *         <ul>
     *             <li>{@code counter_value}.</li>
     *             <li>{@code threshold_value}.</li>
     *             <li>{@code first_branch}.</li>
     *             <li>{@code first_loop_value}.</li>
     *             <li>{@code second_branch}.</li>
     *             <li>{@code second_loop_value}.</li>
     *         </ul>
     *     </li>
     *     <li>In-scope references bind to their declarations at each position.</li>
     *     <li>Out-of-scope references do not bind to those declarations at each position.</li>
     * </ul>
     */
    @Test
    void bigQueryNestedLocalVariablesResolveOnlyWithinTheirLexicalScopes() {
        this.useDialect("google_bigquery");
        ProceduralNestedCompletionScenario.assertVariableResolution(this, "CREATE PROCEDURE dataset.p() BEGIN",
            "CREATE TEMP TABLE scratch (id INT64);", "END", "DO");
    }
}
