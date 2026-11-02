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

package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class PostgreRoleTest extends DBeaverUnitTest {

    private PostgreDatabase testDatabase;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");

        PostgreDataSource testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return false;
            }
        };
        PostgreRole owner = new PostgreRole(null, "tester", "test", true);
        testDatabase = testDataSource.createDatabaseImpl(monitor, "testdb", owner, null, null, null);
    }

    @Test
    public void generateRoleDDL_whenConnectionLimitIsZero_returnZeroLimit() throws Exception {
        String ddl = generateRoleDDL(0);

        Assertions.assertTrue(ddl.contains("CONNECTION LIMIT 0"), ddl);
        Assertions.assertFalse(ddl.contains("CONNECTION LIMIT -1"), ddl);
    }

    @Test
    public void generateRoleDDL_whenConnectionLimitIsPositive_returnLimit() throws Exception {
        String ddl = generateRoleDDL(25);

        Assertions.assertTrue(ddl.contains("CONNECTION LIMIT 25"), ddl);
    }

    @Test
    public void generateRoleDDL_whenConnectionLimitIsUnlimited_returnMinusOne() throws Exception {
        String ddl = generateRoleDDL(-1);

        Assertions.assertTrue(ddl.contains("CONNECTION LIMIT -1"), ddl);
    }

    @NotNull
    private String generateRoleDDL(int connectionLimit) throws Exception {
        PostgreRole role = new PostgreRole(testDatabase, "test_role", null, true);
        role.setConnLimit(connectionLimit);
        return role.getObjectDefinitionText(monitor, Collections.emptyMap());
    }
}
