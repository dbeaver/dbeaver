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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.util.Arrays;

public class PostgreTablePartitionTest extends DBeaverUnitTest {

    private PostgreSchema testSchema;
    private PostgreTableRegular parentTable;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");
        PostgreDataSource testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return true;
            }
        };
        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(monitor, "testdb", testUser, null, null, null);
        testSchema = new PostgreSchema(testDatabase, "test_schema", testUser);
        parentTable = new PostgreTableRegular(testSchema);
        parentTable.setName("measurements");
    }

    @Test
    public void structuralParentIsSchemaUntilParentTableIsKnown() {
        PostgreTablePartition partition = createSchemaPartition("measurements_2026");

        Assertions.assertSame(testSchema, partition.getStructuralParent());
        assertPathEndsWith(DBUtils.getObjectPath(partition, true), testSchema, partition);
    }

    @Test
    public void resolvedPartitionIsPlacedUnderParentTable() throws DBException {
        PostgreTablePartition partition = createSchemaPartition("measurements_2026");
        partition.addSuperTableInheritance(parentTable, 1);

        partition.resolveParentTable(monitor);

        Assertions.assertSame(parentTable, partition.getParentTable());
        Assertions.assertFalse(partition.isSubPartition());
        Assertions.assertSame(parentTable, partition.getStructuralParent());
        assertPathEndsWith(DBUtils.getObjectPath(partition, true), testSchema, parentTable, partition);
        assertPathEndsWith(DBUtils.getObjectPath(partition, false), testSchema, parentTable);
    }

    @Test
    public void resolvedSubPartitionIsPlacedUnderWholeParentChain() throws DBException {
        PostgreTablePartition partition = createSchemaPartition("measurements_2026");
        partition.addSuperTableInheritance(parentTable, 1);
        PostgreTablePartition subPartition = createSchemaPartition("measurements_2026_01");
        subPartition.addSuperTableInheritance(partition, 1);

        subPartition.resolveParentTable(monitor);

        Assertions.assertTrue(subPartition.isSubPartition());
        Assertions.assertSame(partition, subPartition.getPartitionParent());
        Assertions.assertSame(parentTable, partition.getParentTable());
        assertPathEndsWith(DBUtils.getObjectPath(subPartition, true), testSchema, parentTable, partition, subPartition);
    }

    private PostgreTablePartition createSchemaPartition(String name) {
        PostgreTablePartition partition = new PostgreTablePartition(testSchema, Mockito.mock(ResultSet.class));
        partition.setName(name);
        return partition;
    }

    private static void assertPathEndsWith(DBSObject[] path, DBSObject... expectedTail) {
        Assertions.assertTrue(path.length >= expectedTail.length, () -> "Path too short: " + Arrays.toString(path));
        DBSObject[] tail = Arrays.copyOfRange(path, path.length - expectedTail.length, path.length);
        Assertions.assertArrayEquals(expectedTail, tail, () -> "Unexpected path: " + Arrays.toString(path));
    }
}
