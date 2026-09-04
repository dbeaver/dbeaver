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

import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerTimescale;
import org.jkiss.dbeaver.ext.postgresql.model.impls.timescale.TimescaleTable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class PostgreServerTimescaleTest extends DBeaverUnitTest {

    @Mock
    private PostgreDataSource mockDataSource;

    @Mock
    private PostgreSchema mockSchema;

    @Mock
    private JDBCResultSet mockResults;

    @InjectMocks
    private PostgreServerTimescale server;

    @BeforeEach
    public void setUp() {
        when(mockDataSource.getServerType()).thenReturn(server);
        when(mockSchema.getDataSource()).thenReturn(mockDataSource);
    }

    @Test
    public void getServerTypeNameReturnsTimescale() {
        Assertions.assertEquals("Timescale", server.getServerTypeName());
    }

    @Test
    public void supportsGeneratedColumnsWhenServerVersionIsAtLeast12ReturnsTrue() {
        when(mockDataSource.isServerVersionAtLeast(12, 0)).thenReturn(true);

        Assertions.assertTrue(server.supportsGeneratedColumns());
    }

    @Test
    public void supportsGeneratedColumnsWhenServerVersionIsLessThan12ReturnsFalse() {
        when(mockDataSource.isServerVersionAtLeast(12, 0)).thenReturn(false);

        Assertions.assertFalse(server.supportsGeneratedColumns());
    }

    @Test
    public void createRelationOfClassWhenTableTypeIsRegularReturnsInstanceOfTimescaleTable() {
        Assertions.assertEquals(
            TimescaleTable.class,
            server.createRelationOfClass(mockSchema, PostgreClass.RelKind.r, mockResults).getClass());
    }
}
