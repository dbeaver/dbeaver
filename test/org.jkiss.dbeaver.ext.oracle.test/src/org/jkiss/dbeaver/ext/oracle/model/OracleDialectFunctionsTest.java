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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class OracleDialectFunctionsTest extends DBeaverUnitTest {
    @Mock
    DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    DBSTypedObject mockTypedObject;
    @Mock
    private DBPConnectionConfiguration mockConnectionConfiguration;
    private OracleSQLDialect dialect;
    private OracleDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        dialect = new OracleSQLDialect();
        Mockito.when(mockDataSourceContainer.getConnectionConfiguration()).thenReturn(mockConnectionConfiguration);
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("oracle"));
        dataSource = new OracleDataSource(mockDataSourceContainer);
        dataSource.getDataTypeCache().cacheObject(new OracleDataType(dataSource, "JSON", true));
        dataSource.getDataTypeCache().cacheObject(new OracleDataType(dataSource, "NUMBER", true));
    }

    @Test
    public void generateCorrectDataTypeNameWithModifiersFromJSONDataType() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("json");
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, dataSource);
        Assert.assertEquals("JSON", actualDataType);
    }

    @Test
    public void generateCorrectDataTypeNameFromNUMERICDataTypeWithModifiers() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("numeric");
        Mockito.when(mockTypedObject.getPrecision()).thenReturn(null);
//        Mockito.when(mockTypedObject.getScale()).thenReturn(null);
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, dataSource);
        Assert.assertEquals("NUMBER", actualDataType);
    }

    @Test
    public void generateCorrectDataFromNUMERICDataTypeWithPrecision() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("numeric");
        Mockito.when(mockTypedObject.getPrecision()).thenReturn(33);
        Mockito.when(mockTypedObject.getScale()).thenReturn(null);
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, dataSource);
        Assert.assertEquals("NUMBER(33)", actualDataType);
    }

    @Test
    public void generateCorrectDataFromNUMERICDataTypeWithPrecisionAndScale() {
        Mockito.when(mockTypedObject.getTypeName()).thenReturn("numeric");
        Mockito.when(mockTypedObject.getPrecision()).thenReturn(22);
        Mockito.when(mockTypedObject.getScale()).thenReturn(11);
        String actualDataType = dialect.convertExternalDataType(dialect, mockTypedObject, dataSource);
        Assert.assertEquals("NUMBER(22,11)", actualDataType);
    }
}
