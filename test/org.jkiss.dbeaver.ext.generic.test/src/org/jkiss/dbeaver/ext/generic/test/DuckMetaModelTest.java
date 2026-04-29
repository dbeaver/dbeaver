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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.dbeaver.ext.duckdb.model.DuckMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class DuckMetaModelTest extends DBeaverUnitTest {

    @Test
    public void readsForeignKeysFromDuckDBConstraints() throws Exception {
        String sql = prepareForeignKeysSql();

        Driver driver = (Driver) Class.forName("org.duckdb.DuckDBDriver").getConstructor().newInstance();
        try (Connection connection = driver.connect("jdbc:duckdb:", new Properties());
            Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA s1");
            statement.execute("CREATE TABLE parent(id1 INTEGER, id2 INTEGER, PRIMARY KEY (id1, id2))");
            statement.execute(
                "CREATE TABLE child(parent_id1 INTEGER, parent_id2 INTEGER, FOREIGN KEY (parent_id1, parent_id2) " +
                    "REFERENCES parent(id1, id2))");
            statement.execute("CREATE TABLE s1.parent(id INTEGER PRIMARY KEY)");
            statement.execute("CREATE TABLE s1.child(parent_id INTEGER REFERENCES s1.parent(id))");

            assertForeignKey(
                sql,
                connection,
                "main",
                "child",
                "main",
                "parent",
                new String[] {"id1", "id2"},
                new String[] {"parent_id1", "parent_id2"});
            assertForeignKey(
                sql,
                connection,
                "s1",
                "child",
                "s1",
                "parent",
                new String[] {"id"},
                new String[] {"parent_id"});
        }
    }

    private static String prepareForeignKeysSql() throws Exception {
        DuckMetaModel metaModel = new DuckMetaModel();
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);

        GenericStructContainer owner = Mockito.mock(GenericStructContainer.class);
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        GenericCatalog catalog = Mockito.mock(GenericCatalog.class);
        GenericSchema schema = Mockito.mock(GenericSchema.class);
        GenericTableBase table = Mockito.mock(GenericTableBase.class);

        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(owner.getCatalog()).thenReturn(catalog);
        Mockito.when(catalog.getName()).thenReturn("memory");
        Mockito.when(owner.getSchema()).thenReturn(schema);
        Mockito.when(schema.getName()).thenReturn("main");
        Mockito.when(table.getName()).thenReturn("child");

        JDBCStatement result = metaModel.prepareForeignKeysLoadStatement(session, owner, table);

        Assert.assertSame(statement, result);
        Mockito.verify(statement).setString(1, "memory");
        Mockito.verify(statement).setString(2, "memory");
        Mockito.verify(statement).setString(3, "main");
        Mockito.verify(statement).setString(4, "main");
        Mockito.verify(statement).setString(5, "child");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sqlCaptor.capture());
        return sqlCaptor.getValue();
    }

    private static void assertForeignKey(
        String sql,
        Connection connection,
        String fkSchema,
        String fkTable,
        String pkSchema,
        String pkTable,
        String[] pkColumns,
        String[] fkColumns
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "memory");
            statement.setString(2, "memory");
            statement.setString(3, fkSchema);
            statement.setString(4, fkSchema);
            statement.setString(5, fkTable);

            try (ResultSet resultSet = statement.executeQuery()) {
                for (int i = 0; i < pkColumns.length; i++) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(pkSchema, resultSet.getString(JDBCConstants.PKTABLE_SCHEM));
                    Assert.assertEquals(pkTable, resultSet.getString(JDBCConstants.PKTABLE_NAME));
                    Assert.assertEquals(pkColumns[i], resultSet.getString(JDBCConstants.PKCOLUMN_NAME));
                    Assert.assertEquals(fkSchema, resultSet.getString(JDBCConstants.FKTABLE_SCHEM));
                    Assert.assertEquals(fkTable, resultSet.getString(JDBCConstants.FKTABLE_NAME));
                    Assert.assertEquals(fkColumns[i], resultSet.getString(JDBCConstants.FKCOLUMN_NAME));
                    Assert.assertEquals(i + 1, resultSet.getInt(JDBCConstants.KEY_SEQ));
                }
                Assert.assertFalse(resultSet.next());
            }
        }
    }
}
