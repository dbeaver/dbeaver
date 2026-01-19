/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SQLQueryDangerousDetectionTest extends DBeaverUnitTest {


    @Test
    public void noDropStatementShouldReturnNoneTypeDrop() {
        // given
        var query = new SQLQuery(null, "SELECT * FROM table WHERE id = ?");
        // then
        assertFalse(query.isDropDangerous());
    }

    @Test
    public void noDangerousUpdateShouldReturnNotDangerous() {
        // given
        var query = new SQLQuery(null, "UPDATE table SET a = 1 WHERE id = 1");
        // then
        assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void noDangerousDeleteShouldReturnNotDangerous() {
        // given
        var query = new SQLQuery(null, "DELETE FROM table SET a = 1 WHERE id = 1");
        // then
        assertFalse(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dangerousUpdateShouldReturnDangerous() {
        // given
        var query = new SQLQuery(null, "UPDATE table SET a = 1");
        // then
        assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dangerousDeleteShouldReturnDangerous() {
        // given
        var query = new SQLQuery(null, "DELETE FROM table");
        // then
        assertTrue(query.isDeleteUpdateDangerous());
    }

    @Test
    public void dropTableStatementShouldReturnTableDropType() {
        // given
        var query = new SQLQuery(null, "DROP table users");
        // then
        assertTrue(query.isDropDangerous());
    }

    @Test
    public void dropSchemaStatementShouldReturnDropType() {
        // given
        var query = new SQLQuery(null, "DROP schema users");
        // then
        assertTrue(query.isDropDangerous());
    }

}
