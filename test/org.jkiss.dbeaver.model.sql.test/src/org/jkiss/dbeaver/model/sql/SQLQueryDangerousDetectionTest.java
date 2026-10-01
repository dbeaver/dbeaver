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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SQLQueryDangerousDetectionTest extends DBeaverUnitTest {

    @Test
    public void noDropStatementShouldReturnNoneTypeDrop() {
        // given
        var query = new SQLQuery(null, "SELECT * FROM table WHERE id = ?");
        // then
        Assertions.assertFalse(query.isDropDangerous());
    }

    @Test
    public void noDangerousUpdateShouldReturnNotDangerous() {
        // given
        var query = new SQLQuery(null, "UPDATE table SET a = 1 WHERE id = 1");
        // then
        Assertions.assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void noDangerousDeleteShouldReturnNotDangerous() {
        // given
        var query = new SQLQuery(null, "DELETE FROM table SET a = 1 WHERE id = 1");
        // then
        Assertions.assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dangerousUpdateShouldReturnDangerous() {
        // given
        var query = new SQLQuery(null, "UPDATE table SET a = 1");
        // then
        Assertions.assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dangerousDeleteShouldReturnDangerous() {
        // given
        var query = new SQLQuery(null, "DELETE FROM table");
        // then
        Assertions.assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dropTableStatementShouldReturnTableDropType() {
        // given
        var query = new SQLQuery(null, "DROP table users");
        // then
        Assertions.assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropSchemaStatementShouldReturnDropType() {
        // given
        var query = new SQLQuery(null, "DROP schema users");
        // then
        Assertions.assertTrue(query.isDropDangerous());
    }

    @Test
    public void allSelectStatementsShouldHaveSelectType() {
        for (String queryText : List.of(
            "SELECT * FROM test",
            "SELECT id FROM test UNION SELECT id FROM other_test",
            "(SELECT * FROM test)"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertEquals(SQLQueryType.SELECT, query.getType(), queryText);
        }
    }

    @Test
    public void readOnlySelectStatementsShouldNotBeMutating() {
        for (String queryText : List.of(
            "SELECT * FROM test",
            "SELECT id FROM test UNION SELECT id FROM other_test",
            "WITH selected AS (SELECT * FROM test) SELECT * FROM selected",
            "(SELECT * FROM test)"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertFalse(query.isMutatingStatement(), queryText);
            Assertions.assertFalse(query.isModifying(), queryText);
        }
    }

    @Test
    public void modifyingSelectStatementsShouldBeMutating() {
        for (String queryText : List.of(
            "SELECT * INTO copy FROM test",
            "SELECT * FROM (SELECT * INTO copy FROM test) nested",
            "WITH changed AS (DELETE FROM test RETURNING *) SELECT * FROM changed",
            "WITH changed AS (INSERT INTO test VALUES (1) RETURNING *) SELECT * FROM changed",
            "WITH changed AS (UPDATE test SET id = 1 RETURNING *) SELECT * FROM changed",
            "WITH safe AS (SELECT 1), changed AS (DELETE FROM test RETURNING *) SELECT * FROM safe",
            "SELECT 1 ORDER BY (WITH changed AS (DELETE FROM test RETURNING *) SELECT count(*) FROM changed)",
            "SELECT 1 UNION SELECT 2 " +
                "ORDER BY (WITH changed AS (DELETE FROM test RETURNING *) SELECT count(*) FROM changed)",
            "SELECT 1 GROUP BY (WITH changed AS (DELETE FROM test RETURNING *) SELECT count(*) FROM changed)"
        )) {
            var query = new SQLQuery(null, queryText);
            Assertions.assertTrue(query.isMutatingStatement(), queryText);
            Assertions.assertTrue(query.isModifying(), queryText);
        }

        var lockingQuery = new SQLQuery(null, "SELECT * FROM test FOR UPDATE");
        Assertions.assertFalse(lockingQuery.isMutatingStatement());
        Assertions.assertTrue(lockingQuery.isModifying());
    }

}
