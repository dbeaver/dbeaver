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
package org.jkiss.dbeaver.ext.timeplus;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusDataSource;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusMetaModel;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class TimeplusSQLDialectTest extends DBeaverUnitTest {

    private TimeplusDataSource dataSource;
    private DBSEntityAttribute sourceAttribute;

    @BeforeEach
    public void setUp() throws DBException {
        DBPDataSourceContainer container = configureTestContainer("timeplus_native");
        dataSource = Mockito.spy(new TimeplusDataSource(monitor, new TimeplusMetaModel(), container));
        DBSDataType stringType = numericType("string");
        Mockito.when(stringType.getDataKind()).thenReturn(DBPDataKind.STRING);
        // Match the native driver's ordering: int8 comes before the wider integer types.
        List<DBSDataType> types = List.of(
            stringType, numericType("int8"), numericType("uint8"), numericType("int16"), numericType("int32"),
            numericType("int64"), numericType("uint64"), numericType("float32"), numericType("float64")
        );
        Mockito.doReturn(types).when(dataSource).getLocalDataTypes();
        Mockito.doAnswer(invocation -> types.stream()
            .filter(type -> type.getTypeName().equalsIgnoreCase(invocation.getArgument(0)))
            .findFirst().orElse(null)).when(dataSource).getLocalDataType(Mockito.anyString());
        DBPDataSource sourceDataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(sourceDataSource.getSQLDialect()).thenReturn(new GenericSQLDialect());
        sourceAttribute = Mockito.mock(DBSEntityAttribute.class);
        Mockito.when(sourceAttribute.getDataSource()).thenReturn(sourceDataSource);
        Mockito.when(sourceAttribute.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(sourceAttribute.getTypeID()).thenReturn(-1);
        Mockito.when(sourceAttribute.getPrecision()).thenReturn(null);
        Mockito.when(sourceAttribute.getScale()).thenReturn(null);
    }

    @Test
    public void mapCSVNumericTypesWithoutNarrowing() {
        Assertions.assertInstanceOf(TimeplusSQLDialect.class, dataSource.getSQLDialect());
        assertMapping("INTEGER", "int32");
        assertMapping("BIGINT", "int64");
        assertMapping("REAL", "float64");
    }

    @Test
    public void mapStandardNumericAliases() {
        assertMapping("int", "int32");
        assertMapping("smallint", "int16");
        assertMapping("tinyint", "int8");
        assertMapping("float", "float64");
        assertMapping("double", "float64");
    }

    @Test
    public void preserveNativeTypes() {
        assertMapping("int8", "int8");
        assertMapping("int32", "int32");
        assertMapping("uint64", "uint64");
        assertMapping("float32", "float32");
        Mockito.when(sourceAttribute.getDataKind()).thenReturn(DBPDataKind.STRING);
        assertMapping("string", "string");
    }

    @Test
    public void onlyNativeDriverRequiresSerializedStatements() {
        Assertions.assertFalse(dataSource.getContainer().getDriver().isThreadSafeDriver());
        Assertions.assertTrue(configureTestContainer("timeplus_proton").getDriver().isThreadSafeDriver());
    }

    private void assertMapping(@NotNull String sourceType, @NotNull String expectedType) {
        Mockito.when(sourceAttribute.getTypeName()).thenReturn(sourceType);
        Assertions.assertEquals(expectedType, DBStructUtils.mapTargetDataType(dataSource, sourceAttribute, false));
        Assertions.assertEquals(expectedType, DBStructUtils.mapTargetDataType(dataSource, sourceAttribute, true));
    }

    @NotNull
    private DBSDataType numericType(@NotNull String name) {
        DBSDataType type = Mockito.mock(DBSDataType.class);
        Mockito.when(type.getTypeName()).thenReturn(name);
        Mockito.when(type.getDataKind()).thenReturn(DBPDataKind.NUMERIC);
        Mockito.when(type.getPrecision()).thenReturn(0);
        Mockito.when(type.getScale()).thenReturn(0);
        return type;
    }
}
