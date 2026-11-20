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

import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class PostgreSchemaMyTempTest extends DBeaverUnitTest {
    @Mock
    private PostgreDatabase database;
    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private PostgreServerExtension serverType;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCPreparedStatement statement;
    @Mock
    private JDBCResultSet resultSet;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getServerType()).thenReturn(serverType);
        Mockito.when(session.prepareStatement(Mockito.contains("pg_my_temp_schema()"))).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    public void readsSchemaFromQuerySession() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true);
        Mockito.when(resultSet.getString("nspname")).thenReturn("pg_temp_3");
        Mockito.when(resultSet.getLong("oid")).thenReturn(123L);

        PostgreSchemaMyTemp schema = new PostgreSchemaMyTemp(session, database);
        Assertions.assertEquals("pg_temp_3", schema.getName());
        Assertions.assertEquals(123L, schema.getObjectId());
        Assertions.assertTrue(schema.isPersisted());
    }

    @Test
    public void absentTemporarySchemaHasNoTables() throws Exception {
        PostgreSchemaMyTemp schema = new PostgreSchemaMyTemp(session, database);

        Assertions.assertFalse(schema.isPersisted());
        Assertions.assertTrue(schema.readTableColumns(session, "view6").isEmpty());
        Mockito.verify(session).prepareStatement(Mockito.contains("pg_my_temp_schema()"));
    }
}
