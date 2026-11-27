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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class FireBirdColumnTypeNameListProviderTest extends DBeaverUnitTest {

    @Test
    public void excludesSystemAndImplicitDomainsFromColumnTypeNames() {
        FireBirdDataSource dataSource = Mockito.mock(FireBirdDataSource.class);
        Mockito.doReturn(List.of(
            dataType("INTEGER", DBPDataKind.NUMERIC),
            dataType("VARCHAR", DBPDataKind.STRING),
            dataType("NEW_DOMAIN", DBPDataKind.STRING),
            dataType("RDB$1", DBPDataKind.NUMERIC),
            dataType("RDB$100010", DBPDataKind.STRING),
            dataType("RDB$FIELD_NAME", DBPDataKind.STRING)
        )).when(dataSource).getLocalDataTypes();
        FireBirdTableColumn column = Mockito.mock(FireBirdTableColumn.class);
        Mockito.when(column.getDataSource()).thenReturn(dataSource);

        Object[] typeNames = new FireBirdTableColumn.FireBirdColumnTypeNameListProvider().getPossibleValues(column);

        Assertions.assertArrayEquals(new String[] {"INTEGER", "NEW_DOMAIN", "VARCHAR"}, typeNames);
    }

    @NotNull
    private static DBSDataType dataType(@NotNull String name, @NotNull DBPDataKind dataKind) {
        DBSDataType dataType = Mockito.mock(DBSDataType.class);
        Mockito.when(dataType.getName()).thenReturn(name);
        Mockito.when(dataType.getDataKind()).thenReturn(dataKind);
        return dataType;
    }
}
