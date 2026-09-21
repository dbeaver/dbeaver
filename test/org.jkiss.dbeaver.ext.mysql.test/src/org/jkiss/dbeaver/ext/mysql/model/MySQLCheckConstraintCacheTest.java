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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog.CheckConstraintCache;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the check constraint lookup of dbeaver/dbeaver#41909 and
 * dbeaver/dbeaver#41941.
 */
public class MySQLCheckConstraintCacheTest extends DBeaverUnitTest {

    private static final String TABLE_CONSTRAINTS = "TABLE_CONSTRAINTS";

    @Test
    public void mariaDBQueryDoesNotReadTableConstraints() {
        // MariaDB hides TABLE_CONSTRAINTS from accounts holding only SELECT, which used to
        // wipe out every check constraint for read-only users (#41909)
        String sql = CheckConstraintCache.buildCheckConstraintsQuery(true, false);
        Assertions.assertFalse(sql.contains(TABLE_CONSTRAINTS), sql);
    }

    @Test
    public void mariaDBQueryTakesTableNameFromCheckConstraints() {
        // Reading TABLE_NAME from cc is what keeps same-named constraints of different
        // tables apart (#41941)
        String sql = CheckConstraintCache.buildCheckConstraintsQuery(true, false);
        Assertions.assertTrue(sql.contains("cc.TABLE_NAME"), sql);
    }

    @Test
    public void mySQLQueryJoinsTableConstraints() {
        // MySQL has no TABLE_NAME in CHECK_CONSTRAINTS, so the join is the only source
        String sql = CheckConstraintCache.buildCheckConstraintsQuery(false, false);
        Assertions.assertTrue(sql.contains(TABLE_CONSTRAINTS), sql);
        Assertions.assertTrue(sql.contains("tc.CONSTRAINT_NAME=cc.CONSTRAINT_NAME"), sql);
    }

    @Test
    public void wholeSchemaQueryBindsSchemaOnly() {
        Assertions.assertEquals(1, countBinds(CheckConstraintCache.buildCheckConstraintsQuery(true, false)));
        Assertions.assertEquals(1, countBinds(CheckConstraintCache.buildCheckConstraintsQuery(false, false)));
    }

    @Test
    public void singleTableQueryBindsSchemaAndTable() {
        // prepareObjectsStatement binds the schema first and the table second
        Assertions.assertEquals(2, countBinds(CheckConstraintCache.buildCheckConstraintsQuery(true, true)));
        Assertions.assertEquals(2, countBinds(CheckConstraintCache.buildCheckConstraintsQuery(false, true)));
    }

    @Test
    public void singleTableQueryFiltersByTableName() {
        Assertions.assertTrue(
            CheckConstraintCache.buildCheckConstraintsQuery(true, true).contains("cc.TABLE_NAME = ?"));
        Assertions.assertTrue(
            CheckConstraintCache.buildCheckConstraintsQuery(false, true).contains("tc.TABLE_NAME = ?"));
    }

    private static long countBinds(String sql) {
        return sql.chars().filter(c -> c == '?').count();
    }
}
