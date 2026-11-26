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

import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DBNLocalFolderTest {
    @Test
    public void folderDropRequiresSameRegistryAndValidDestination() {
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        DBNProjectDatabases parent = Mockito.mock(DBNProjectDatabases.class);
        Mockito.when(parent.getDataSourceRegistry()).thenReturn(registry);
        DBPDataSourceFolder destination = Mockito.mock(DBPDataSourceFolder.class);
        DBNLocalFolder target = new DBNLocalFolder(parent, destination);
        DBPDataSourceFolder folder = Mockito.mock(DBPDataSourceFolder.class);
        DBNLocalFolder source = new DBNLocalFolder(parent, folder);

        Assertions.assertFalse(target.supportsDrop(source));
        Mockito.when(folder.canMoveTo(destination)).thenReturn(true);
        Assertions.assertTrue(target.supportsDrop(source));

        DBNProjectDatabases otherParent = Mockito.mock(DBNProjectDatabases.class);
        Mockito.when(otherParent.getDataSourceRegistry()).thenReturn(Mockito.mock(DBPDataSourceRegistry.class));
        Assertions.assertFalse(target.supportsDrop(new DBNLocalFolder(otherParent, folder)));
    }
}
