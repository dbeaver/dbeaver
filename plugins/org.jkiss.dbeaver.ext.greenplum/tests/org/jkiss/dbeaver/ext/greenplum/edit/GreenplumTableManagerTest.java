package org.jkiss.dbeaver.ext.greenplum.edit;

import org.jkiss.dbeaver.ext.greenplum.model.GreenplumExternalTable;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumSchema;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumTable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeign;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class GreenplumTableManagerTest {
    @Mock
    private GreenplumSchema mockSchema;

    @Mock
    private PostgreDatabase mockDatabase;

    @Mock
    private ResultSet mockResults;

    @Mock
    private PostgreDataSource mockDataSource;

    private GreenplumTableManager greenplumTableManager;

    @Before
    public void setUp() {
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDataSource.isServerVersionAtLeast(Matchers.anyInt(), Matchers.anyInt())).thenReturn(false);

        greenplumTableManager = new GreenplumTableManager();
    }

    @Test
    public void addObjectDeleteActions_whenObjectIsARegularTable_thenRegularTableDropActionIsReturned() throws SQLException {
        SQLDatabasePersistAction regularTableDropTableQuery =
                new SQLDatabasePersistAction("Drop table", "DROP TABLE foo.bar");

        Mockito.when(mockSchema.getName()).thenReturn("foo");
        Mockito.when(mockResults.getString("relname")).thenReturn("bar");
        GreenplumTable greenplumTable = new GreenplumTable(mockSchema, mockResults);

        SQLDatabasePersistAction sqlDatabasePersistAction =
                greenplumTableManager.createDeleteAction(greenplumTable, Collections.emptyMap());

        Assert.assertEquals(regularTableDropTableQuery.getScript(), sqlDatabasePersistAction.getScript());
        Assert.assertEquals(regularTableDropTableQuery.getTitle(), sqlDatabasePersistAction.getTitle());
    }

    @Test
    public void addObjectDeleteActions_whenObjectIsAForeignTable_thenForeignTableDropActionIsReturned() throws SQLException {
        SQLDatabasePersistAction regularTableDropTableQuery =
                new SQLDatabasePersistAction("Drop table", "DROP FOREIGN TABLE foo.bar");

        Mockito.when(mockSchema.getName()).thenReturn("foo");
        Mockito.when(mockResults.getString("relname")).thenReturn("bar");

        PostgreTableForeign postgreForeignTable = new PostgreTableForeign(mockSchema, mockResults);

        SQLDatabasePersistAction sqlDatabasePersistAction =
                greenplumTableManager.createDeleteAction(postgreForeignTable, Collections.emptyMap());

        Assert.assertEquals(regularTableDropTableQuery.getScript(), sqlDatabasePersistAction.getScript());
        Assert.assertEquals(regularTableDropTableQuery.getTitle(), sqlDatabasePersistAction.getTitle());
    }

    @Test
    public void addObjectDeleteActions_whenObjectIsAnExternalTable_thenExternalTableDropActionIsReturned() throws SQLException {
        SQLDatabasePersistAction regularTableDropTableQuery =
                new SQLDatabasePersistAction("Drop table", "DROP EXTERNAL TABLE foo.bar");

        Mockito.when(mockSchema.getName()).thenReturn("foo");
        Mockito.when(mockResults.getString("relname")).thenReturn("bar");

        GreenplumExternalTable greenplumExternalTable = newGreenplumExternalTableFixture();

        SQLDatabasePersistAction sqlDatabasePersistAction =
                greenplumTableManager.createDeleteAction(greenplumExternalTable, Collections.emptyMap());

        Assert.assertEquals(regularTableDropTableQuery.getScript(), sqlDatabasePersistAction.getScript());
        Assert.assertEquals(regularTableDropTableQuery.getTitle(), sqlDatabasePersistAction.getTitle());
    }

    private GreenplumExternalTable newGreenplumExternalTableFixture() throws SQLException {
        Mockito.when(mockResults.getString("fmttype")).thenReturn("b");
        Mockito.when(mockResults.getString("urilocation")).thenReturn("some_location");
        Mockito.when(mockResults.getString("fmtopts")).thenReturn("\n");
        Mockito.when(mockResults.getString("encoding")).thenReturn("UTF8");
        Mockito.when(mockResults.getString("execlocation")).thenReturn("some_location");
        return new GreenplumExternalTable(mockSchema, mockResults);
    }
}