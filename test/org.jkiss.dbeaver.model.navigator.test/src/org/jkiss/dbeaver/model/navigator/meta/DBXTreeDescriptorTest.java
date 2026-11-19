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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class DBXTreeDescriptorTest extends DBeaverUnitTest {

    public static class DataSource {
        public List<Schema> getSchemas() {
            return List.of();
        }
    }

    public static class Schema {
        public List<Table> getTables() {
            return List.of();
        }
    }

    public static class Table implements DBSObject {
        @NotNull
        @Override
        public String getName() {
            return "table";
        }

        @Nullable
        @Override
        public String getDescription() {
            return null;
        }

        @Nullable
        @Override
        public DBSObject getParentObject() {
            return null;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPersisted() {
            return true;
        }
    }

    public static class ExtendedTable extends Table {
    }

    @Test
    public void injectedFolderSubtypeIsUsedOnlyWhenRequested() {
        AbstractDescriptor source = Mockito.mock(AbstractDescriptor.class);
        Mockito.when(source.getObjectClass(ExtendedTable.class.getName())).thenReturn(ExtendedTable.class);
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        DBXTreeDescriptor root = new DBXTreeDescriptor(
            source, null, null, "test", null, false, true, false, false, false, null, null);
        DBXTreeItem schemas = new DBXTreeItem(
            source, root, config, "schema", "schemas", false, true, false, false, false, null, null);
        DBXTreeFolder tables = new DBXTreeFolder(
            source, schemas, config, ExtendedTable.class.getName(), true, false, null, false);
        new DBXTreeItem(source, tables, config, "table", "tables", false, true, false, false, false, null, null);

        Assertions.assertEquals(Table.class,
            DBXTreeDescriptor.findImplementorTypeInDataSourceTree(root, DataSource.class, Table.class, null, false));
        Assertions.assertEquals(ExtendedTable.class,
            DBXTreeDescriptor.findImplementorTypeInDataSourceTree(root, DataSource.class, Table.class, null, true));

        tables.setType(Schema.class.getName());
        Mockito.when(source.getObjectClass(Schema.class.getName())).thenReturn(Schema.class);
        Assertions.assertEquals(Table.class,
            DBXTreeDescriptor.findImplementorTypeInDataSourceTree(root, DataSource.class, Table.class, null, true));
    }
}
