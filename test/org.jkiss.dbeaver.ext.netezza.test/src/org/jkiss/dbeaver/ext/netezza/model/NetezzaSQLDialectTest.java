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
package org.jkiss.dbeaver.ext.netezza.model;

import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class NetezzaSQLDialectTest extends DBeaverUnitTest {

    @Test
    public void preservesNcharLengthWhenItMatchesDataTypeLength() {
        NetezzaSQLDialect dialect = new NetezzaSQLDialect();
        DBPDataSource dataSource = Mockito.mock(
            DBPDataSource.class,
            Mockito.withSettings().extraInterfaces(DBPDataTypeProvider.class));
        DBSTypedObject column = Mockito.mock(
            DBSTypedObject.class,
            Mockito.withSettings().extraInterfaces(DBSObject.class));
        DBSDataType dataType = Mockito.mock(DBSDataType.class);

        Mockito.when(((DBSObject) column).getDataSource()).thenReturn(dataSource);
        Mockito.when(column.getTypeName()).thenReturn("NCHAR");
        Mockito.when(column.getMaxLength()).thenReturn(20L);
        Mockito.when(dataType.getMaxLength()).thenReturn(20L);
        Mockito.when(((DBPDataTypeProvider) dataSource).getLocalDataType("NCHAR")).thenReturn(dataType);

        String modifiers = dialect.getColumnTypeModifiers(dataSource, column, "NCHAR", DBPDataKind.STRING);

        Assertions.assertEquals("(20)", modifiers);
    }

    @Test
    public void doesNotAddModifierToUnqualifiedNchar() {
        NetezzaSQLDialect dialect = new NetezzaSQLDialect();
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBSTypedObject column = Mockito.mock(DBSTypedObject.class);

        Mockito.when(column.getMaxLength()).thenReturn(0L);

        String modifiers = dialect.getColumnTypeModifiers(dataSource, column, "NCHAR", DBPDataKind.STRING);

        Assertions.assertNull(modifiers);
    }

    @Test
    public void registersUnicodeCharacterTypes() {
        NetezzaSQLDialect dialect = new NetezzaSQLDialect();
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class);
        Mockito.when(dataSource.getLocalDataTypes()).thenReturn(Collections.emptyList());

        dialect.getDataTypes(dataSource);

        Assertions.assertEquals(DBPKeywordType.TYPE, dialect.getKeywordType("NCHAR"));
        Assertions.assertEquals(DBPKeywordType.TYPE, dialect.getKeywordType("NVARCHAR"));
    }
}
