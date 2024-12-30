/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DataTypeConverterTest extends DBeaverUnitTest {

    @Mock
    DBSTypedObject mockTypedObject;
    @Mock
    JDBCDataSource mockDataSource;
    @Mock
    DBSDataType mockDataType;

    private JDBCSQLDialect dialect;

    @Before
    public void setUp() throws Exception {
        dialect = new JDBCSQLDialect("testName", "testID");
        Mockito.lenient().when(mockDataSource.getSQLDialect()).thenReturn(dialect);
    }

    @Test
    public void convertVarcharDataTypeWithoutModifiersToNothing() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("varchar");
        Mockito.when(mockTypedObject.getMaxLength()).thenReturn(-1L);
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, mockDataSource);
        Assert.assertNull(actualDataType);
    }

    @Test
    public void convertVarcharDataTypeWithoutModifiersToCLOB() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("varchar");
        Mockito.when(mockTypedObject.getMaxLength()).thenReturn(-1L);
        Mockito.when(mockDataSource.getLocalDataType("clob")).thenReturn(mockDataType);
        Mockito.when(mockDataType.getName()).thenReturn("CLOB");
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, mockDataSource);
        Assert.assertEquals("CLOB", actualDataType);
    }

    @Test
    public void convertVarcharDataTypeWithoutModifiersToTEXT() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("varchar");
        Mockito.when(mockTypedObject.getMaxLength()).thenReturn(-1L);
        Mockito.when(mockDataSource.getLocalDataType("text")).thenReturn(mockDataType);
        Mockito.when(mockDataType.getName()).thenReturn("text");
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, mockDataSource);
        Assert.assertEquals("text", actualDataType);
    }
}
