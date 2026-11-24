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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.util.List;

public class MySQLTemporaryTableTest extends DBeaverUnitTest {
    @Test
    void keepsExistingTableForIfNotExistsAndInvalidatesReplacements() throws Exception {
        MySQLExecutionContext context = createContext();
        JDBCSession session = Mockito.mock(JDBCSession.class);
        List<String> name = List.of("test", "temp_table");
        var original = context.registerTemporaryTable(session, name, false);

        Assertions.assertSame(original, context.registerTemporaryTable(session, name, true));
        Assertions.assertTrue(context.isCurrentTemporaryTable((MySQLTemporaryTable) original));
        Assertions.assertNotSame(original, context.registerTemporaryTable(session, name, false));
        Assertions.assertFalse(context.isCurrentTemporaryTable((MySQLTemporaryTable) original));
    }

    @Test
    void movesRenamedTableToDestinationAndInvalidatesOldGrid() throws Exception {
        MySQLExecutionContext context = createContext();
        JDBCSession session = Mockito.mock(JDBCSession.class);
        var original = context.registerTemporaryTable(session, List.of("test", "original"), false);
        var renamed = context.temporaryTableChanged(session, List.of("test", "original"), true, List.of("renamed"));

        Assertions.assertNull(context.findTemporaryTable(metadata("original")));
        Assertions.assertSame(renamed, context.findTemporaryTable(metadata("renamed")));
        Assertions.assertFalse(context.isCurrentTemporaryTable((MySQLTemporaryTable) original));
        Assertions.assertTrue(context.isCurrentTemporaryTable((MySQLTemporaryTable) renamed));
    }

    @Test
    void populatesEnumAndSetMembersFromFullType() throws Exception {
        MySQLTableColumn column = new MySQLTableColumn(Mockito.mock(MySQLTable.class));
        column.loadEnumValues("enum('first','second','it''s')");
        Assertions.assertEquals(List.of("first", "second", "it's"), column.getEnumValues());
        Assertions.assertTrue(column.isTypeEnum());
        column.loadEnumValues("set('one','two')");
        Assertions.assertEquals(List.of("one", "two"), column.getEnumValues());
        Assertions.assertTrue(column.isTypeSet());
    }

    private MySQLExecutionContext createContext() {
        MySQLDataSource source = Mockito.mock(MySQLDataSource.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(source.getContainer()).thenReturn(container);
        Mockito.when(container.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        MySQLCatalog catalog = Mockito.mock(MySQLCatalog.class);
        Mockito.when(catalog.getName()).thenReturn("test");
        Mockito.when(catalog.getDataSource()).thenReturn(source);
        Mockito.when(source.getCatalog("test")).thenReturn(catalog);
        JDBCRemoteInstance instance = Mockito.mock(JDBCRemoteInstance.class);
        Mockito.when(instance.getDataSource()).thenReturn(source);
        MySQLExecutionContext context = Mockito.spy(new MySQLExecutionContext(instance, "Test"));
        Mockito.doReturn(Mockito.mock(Connection.class)).when(context).getConnectionOrNull();
        return context;
    }

    private DBCEntityMetaData metadata(String name) {
        DBCEntityMetaData metadata = Mockito.mock(DBCEntityMetaData.class);
        Mockito.when(metadata.getCatalogName()).thenReturn("test");
        Mockito.when(metadata.getEntityName()).thenReturn(name);
        return metadata;
    }
}
