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
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryRoutineCreateModel;
import org.junit.jupiter.api.Test;

public class SnowflakeProceduralTest extends ProceduralDialectTestBase {
    @NotNull
    @Override
    protected SQLDialect dialect() {
        return dialectById("snowflake");
    }

    /**
     * <ul>
     * <li>Recognizes the quoted procedure as a routine creation model.</li>
     * <li>Checks the embedded SQL statements:
     * <ul>
     * <li>Finds a non-null routine body.</li>
     * <li>Counts two embedded SQL statements.</li>
     * <li>Confirms every embedded statement has non-null SQL.</li>
     * </ul>
     * </li>
     * </ul>
     */
    @Test
    void quotedBodyContainsMoreThanOneStatement() {
        var routine = root("CREATE PROCEDURE p() RETURNS VARCHAR LANGUAGE SQL AS $$ BEGIN SELECT 1; SELECT 2; END; $$",
            SQLQueryRoutineCreateModel.class);
        assertEmbeddedSqlCount(routine, 2);
    }

    /**
     * <ul>
     * <li>Proposes a temporary table column later in the quoted procedure body.</li>
     * </ul>
     */
    @Test
    void lateTemporaryTableColumnIsProposedWithinQuotedBody() throws DBException {
        assertProposes("CREATE PROCEDURE p() RETURNS VARCHAR LANGUAGE SQL AS $$ " +
            "BEGIN CREATE TEMP TABLE scratch (id int); SELECT id FROM scratch; SELECT | FROM scratch; END; $$", "id");
    }

    /**
     * <ul>
     * <li>Recognizes the dollar-quoted DO body with a nested block as an anonymous block model.</li>
     * </ul>
     */
    @Test
    void anonymousDollarBodyMaintainsNestedStatementSequence() {
        root("DO $$ BEGIN SELECT 1; BEGIN SELECT 2; END; SELECT 3; END; $$",
            org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryAnonymousBlockModel.class);
    }
}
