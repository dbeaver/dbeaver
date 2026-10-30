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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DBNModelTest {
    @Test
    public void navigatorPathIncludesParentForDirectlyAddressablePartition() {
        DBSObject schema = Mockito.mock(DBSObject.class);
        DBSTable table = Mockito.mock(DBSTable.class);
        DBSTablePartition partition = Mockito.mock(DBSTablePartition.class);
        Mockito.when(table.getParentObject()).thenReturn(schema);
        Mockito.when(partition.needFullPath()).thenReturn(false);
        Mockito.when(partition.getParentTable()).thenReturn(table);

        Assertions.assertArrayEquals(
            new DBSObject[] {schema, table, partition},
            DBNModel.getNavigatorObjectPath(partition, true));
        Assertions.assertArrayEquals(
            new DBSObject[] {schema, table},
            DBNModel.getNavigatorObjectPath(partition, false));
    }
}
