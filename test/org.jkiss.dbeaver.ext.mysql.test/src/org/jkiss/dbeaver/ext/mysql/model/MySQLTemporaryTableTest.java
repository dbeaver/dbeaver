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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.util.List;

public class MySQLTemporaryTableTest extends DBeaverUnitTest {
    private static boolean queryManagerInitialized;

    @BeforeAll
    static void initializeQueryManager() {
        try {
            QMUtils.getDefaultHandler();
        } catch (IllegalStateException e) {
            QMUtils.initPlatform(false);
            queryManagerInitialized = true;
        }
    }

    @AfterAll
    static void restoreQueryManager() {
        if (queryManagerInitialized) {
            QMUtils.disposePlatform();
        }
    }

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
    void rejectsOldGridWritesAfterKeyChanges() throws Exception {
        MySQLExecutionContext context = createContext();
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(session.getExecutionContext()).thenReturn(context);
        List<String> name = List.of("test", "temp_table");
        MySQLTemporaryTable original = (MySQLTemporaryTable) context.registerTemporaryTable(session, name, false);
        var other = context.registerTemporaryTable(session, List.of("test", "other"), false);
        var changed = context.temporaryTableChanged(session, name, false, List.of());

        Assertions.assertNotSame(original, changed);
        Assertions.assertSame(changed, context.findTemporaryTable(metadata("temp_table")));
        Assertions.assertSame(other, context.findTemporaryTable(metadata("other")));
        Assertions.assertTrue(changed.getConstraints(new VoidProgressMonitor()).isEmpty());
        Assertions.assertThrows(DBCException.class, () -> original.deleteData(
            session, new DBSAttributeBase[0], Mockito.mock(DBCExecutionSource.class)
        ));
    }

    @Test
    void invalidatesTemporaryMetadataRefreshTargetsAcrossAlterAndDrop() throws Exception {
        MySQLExecutionContext context = createContext();
        JDBCSession session = Mockito.mock(JDBCSession.class);
        List<String> name = List.of("test", "temp_table");
        var original = context.registerTemporaryTable(session, name, false);
        var other = context.registerTemporaryTable(session, List.of("test", "other"), false);
        var altered = context.temporaryTableChanged(session, name, false, List.of());

        Assertions.assertFalse(original.isCurrentTemporaryTable());
        Assertions.assertTrue(altered.isCurrentTemporaryTable());
        context.temporaryTableChanged(session, name, true, List.of());
        Assertions.assertFalse(original.isCurrentTemporaryTable());
        Assertions.assertFalse(altered.isCurrentTemporaryTable());
        Assertions.assertTrue(other.isCurrentTemporaryTable());

        Mockito.doReturn(Mockito.mock(Connection.class)).when(context).getConnectionOrNull();
        Assertions.assertFalse(other.isCurrentTemporaryTable());
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

    @Test
    void usesCompleteNonNullableUniqueKeysAsRowIdentifiers() throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class);
        MySQLTemporaryTable table = (MySQLTemporaryTable) createContext()
            .registerTemporaryTable(session, List.of("test", "temp_table"), false);
        MySQLTableColumn first = new MySQLTableColumn(table);
        first.setName("first");
        first.setRequired(true);
        MySQLTableColumn second = new MySQLTableColumn(table);
        second.setName("second");
        second.setRequired(true);
        MySQLTableColumn nullable = new MySQLTableColumn(table);
        nullable.setName("nullable");
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet result = Mockito.mock(JDBCResultSet.class);
        Mockito.when(session.prepareStatement("SHOW INDEX FROM temp_table WHERE Non_unique = 0")).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(result);
        Mockito.when(result.next()).thenReturn(true, true, true, true, true, true, false);
        Mockito.when(result.getString("Key_name")).thenReturn("valid", "valid", "nullable", "nullable", "functional", "functional");
        Mockito.when(result.getString("Column_name")).thenReturn("second", "first", "first", "nullable", "first", null);
        Mockito.when(result.getInt("Seq_in_index")).thenReturn(2, 1, 1, 2, 1, 2);

        List<MySQLTableConstraint> keys = table.readKeys(session, "temp_table", List.of(first, second, nullable));
        Assertions.assertEquals(1, keys.size());
        MySQLTableConstraint key = keys.getFirst();
        Assertions.assertEquals(DBSEntityConstraintType.UNIQUE_KEY, key.getConstraintType());
        Assertions.assertEquals(List.of(first, second), key.getAttributeReferences(null).stream().map(ref -> ref.getAttribute()).toList());
        Assertions.assertTrue(DBUtils.isIdentifierConstraint(new VoidProgressMonitor(), key));
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
