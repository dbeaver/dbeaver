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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DatabaseTransferUtilsTest {

    @Test
    public void validTargetTable() {
        DBSTable table = Mockito.mock(DBSTable.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        Assertions.assertTrue(DatabaseTransferUtils.isValidTargetTable(table));
    }

    @Test
    public void viewIsNotTargetTable() {
        DBSView view = Mockito.mock(DBSView.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        Assertions.assertFalse(DatabaseTransferUtils.isValidTargetTable(view));
    }

    @Test
    public void sequenceIsNotTargetTable() {
        DBSSequence sequence = Mockito.mock(DBSSequence.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        Assertions.assertFalse(DatabaseTransferUtils.isValidTargetTable(sequence));
    }

    @Test
    public void nonManipulatorIsNotTargetTable() {
        DBSObject object = Mockito.mock(DBSObject.class);
        Assertions.assertFalse(DatabaseTransferUtils.isValidTargetTable(object));
    }

    @Test
    public void nonRelationalManipulatorIsValidTarget() {
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Assertions.assertTrue(DatabaseTransferUtils.isValidTargetTable(target));
    }
}
