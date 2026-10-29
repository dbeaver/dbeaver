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
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryAnonymousBlockModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryTriggerCreateModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FirebirdProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("firebird");
    }

    /**
     * <ul>
     * <li>Anonymous block structure:
     * <ul>
     * <li>The root is an anonymous block model.</li>
     * <li>Its body contains 11 embedded SQL statements with non-null SQL models.</li>
     * </ul></li>
     * <li>Nested control flow:
     * <ul>
     * <li>The {@code IF (n = 0)} site resolves to a conditional statement.</li>
     * <li>That statement has kind IF.</li>
     * <li>The {@code WHILE (n &lt; 1)} site resolves to a conditional statement.</li>
     * <li>That statement has kind LOOP.</li>
     * <li>The {@code IF (n = 2)} site resolves to a conditional statement.</li>
     * <li>That statement has kind IF.</li>
     * <li>The {@code WHILE (n &lt; 3)} site resolves to a conditional statement.</li>
     * <li>That statement has kind LOOP.</li>
     * </ul></li>
     * <li>Column proposals at the 11 SELECT sites:
     * <ul>
     * <li>The initial SELECT from {@code scratch} proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the first IF arm proposes {@code id}.</li>
     * <li>The SELECT from {@code scratch} in the first inner WHILE proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the first ELSE arm proposes {@code id}.</li>
     * <li>The SELECT from {@code scratch} after the first IF proposes {@code id}.</li>
     * <li>The SELECT from {@code scratch} after the outer WHILE proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the second inner IF arm proposes {@code id}.</li>
     * <li>The SELECT from {@code scratch} in the second inner WHILE proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the second inner ELSE arm proposes {@code id}.</li>
     * <li>The SELECT from {@code tab} in the outer ELSE arm proposes {@code id}.</li>
     * <li>The final SELECT from {@code scratch} proposes {@code id}.</li>
     * <li>The proposal check visits exactly 11 SELECT sites.</li>
     * </ul></li>
     * <li>Full-script completion at the final {@code scratch} SELECT contains {@code id}.</li>
     * </ul>
     */
    @Test
    void nativeConditionsAndLoopsRetainAllEmbeddedQueriesAndProposals() throws DBException {
        String sql = """
            EXECUTE BLOCK AS DECLARE VARIABLE n INTEGER = 0; BEGIN
              SELECT id FROM scratch INTO :n;
              WHILE (n < 2) DO BEGIN
                IF (n = 0) THEN BEGIN
                  SELECT id FROM tab INTO :n;
                  WHILE (n < 1) DO BEGIN SELECT id FROM scratch INTO :n; n = n + 1; END
                END ELSE BEGIN SELECT id FROM tab INTO :n; END
                SELECT id FROM scratch INTO :n;
                n = n + 1;
              END
              SELECT id FROM scratch INTO :n;
              IF (n >= 2) THEN BEGIN
                IF (n = 2) THEN BEGIN
                  SELECT id FROM tab INTO :n;
                  WHILE (n < 3) DO BEGIN SELECT id FROM scratch INTO :n; n = n + 1; END
                END ELSE BEGIN SELECT id FROM tab INTO :n; END
              END ELSE BEGIN SELECT id FROM tab INTO :n; END
              SELECT id FROM scratch INTO :n;
            END
            """;
        var model = recognize(sql);
        var block = root(model, SQLQueryAnonymousBlockModel.class);
        assertEmbeddedSqlCount(block, 11);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.IF,
            statementAt(model, sql, "IF (n = 0)", SQLQueryConditionalStatement.class).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.LOOP,
            statementAt(model, sql, "WHILE (n < 1)", SQLQueryConditionalStatement.class).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.IF,
            statementAt(model, sql, "IF (n = 2)", SQLQueryConditionalStatement.class).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.LOOP,
            statementAt(model, sql, "WHILE (n < 3)", SQLQueryConditionalStatement.class).getKind());

        // A Firebird temporary table is created outside PSQL and appears as database metadata here.
        var request = RequestBuilder.tables(tables -> {
            tables.table("scratch", table -> table.attribute("id"));
            tables.table("tab", table -> table.attribute("id"));
        }).dialect(dialect()).prepare();
        assertEverySelectProposes(request, sql, "id", 11);
        int lastSelect = sql.lastIndexOf("SELECT id FROM scratch");
        String last = sql.substring(0, lastSelect) + sql.substring(lastSelect).replace("SELECT id", "SELECT |");
        Assertions.assertTrue(request.requestNewScriptStrings(last).contains("id"), last);
    }

    /**
     * <ul>
     * <li>The EXECUTE BLOCK root is an anonymous block model.</li>
     * <li>Its body contains two embedded SQL statements with non-null SQL models.</li>
     * </ul>
     */
    @Test
    void executeBlockContainsNestedSql() {
        var block = root("EXECUTE BLOCK AS BEGIN SELECT 1; SELECT 2; END", SQLQueryAnonymousBlockModel.class);
        assertEmbeddedSqlCount(block, 2);
    }

    /**
     * <ul>
     * <li>Recognizes the trigger creation root for the body containing {@code changed.id} references.</li>
     * </ul>
     */
    @Test
    void triggerBodyKeepsTransitionRowReferences() {
        root("CREATE TRIGGER tr AFTER UPDATE ON tab REFERENCING NEW AS changed FOR EACH ROW " +
            "BEGIN SELECT changed.id; SELECT changed.id; END", SQLQueryTriggerCreateModel.class);
    }

    /**
     * <ul>
     * <li>Proposes the id column of tab in the second SELECT inside an EXECUTE BLOCK.</li>
     * </ul>
     */
    @Test
    void executeBlockProposesColumnsForEmbeddedSql() throws DBException {
        var proposals = RequestBuilder.tables(tables -> tables.table("tab", table -> table.attribute("id")))
            .dialect(dialect()).prepare().requestNewStrings(
                "EXECUTE BLOCK AS BEGIN SELECT 1 FROM tab; SELECT | FROM tab; END"
            );
        Assertions.assertTrue(proposals.stream().anyMatch(p -> p.trim().equalsIgnoreCase("id")), proposals.toString());
    }
}
