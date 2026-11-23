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
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Types;

public class PostgreCharDataTypeTest extends DBeaverUnitTest {

    private static final long CATALOG_SCHEMA_ID = 11;

    @Mock
    private PostgreDatabase database;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCResultSet resultSet;

    @BeforeEach
    public void setUp() throws Exception {
        DBPDataSourceContainer dataSourceContainer = configureTestContainer("postgresql");
        PostgreDataSource dataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return true;
            }
        };
        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase testDatabase = dataSource.createDatabaseImpl(monitor, "testdb", testUser, null, null, null);
        PostgreSchema catalogSchema = new PostgreSchema(testDatabase, PostgreConstants.CATALOG_SCHEMA_NAME, testUser);

        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(database.getSchema(Mockito.any(), Mockito.eq(CATALOG_SCHEMA_ID))).thenReturn(catalogSchema);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(resultSet.getLong("typnamespace")).thenReturn(CATALOG_SCHEMA_ID);
        Mockito.when(resultSet.getLong("oid")).thenReturn((long) PostgreOid.CHAR);
        Mockito.when(resultSet.getString("typname")).thenReturn("char");
        Mockito.when(resultSet.getInt("typlen")).thenReturn(1);
    }

    @Test
    public void readSingleByteCharTypeOfInternalCategory() throws Exception {
        // PostgreSQL 15+
        PostgreDataType dataType = readCharDataType("Z");

        Assertions.assertEquals(Types.CHAR, dataType.getTypeID());
        Assertions.assertNull(dataType.getConditionTypeCasting(true, false));
    }

    @Test
    public void readSingleByteCharTypeOfStringCategory() throws Exception {
        // PostgreSQL 14 and older
        PostgreDataType dataType = readCharDataType("S");

        Assertions.assertEquals(Types.CHAR, dataType.getTypeID());
        Assertions.assertNull(dataType.getConditionTypeCasting(true, false));
    }

    @NotNull
    private PostgreDataType readCharDataType(@NotNull String typeCategory) throws Exception {
        Mockito.when(resultSet.getString("typcategory")).thenReturn(typeCategory);
        PostgreDataType dataType = PostgreDataType.readDataType(session, database, resultSet, false);
        Assertions.assertNotNull(dataType);
        return dataType;
    }
}
