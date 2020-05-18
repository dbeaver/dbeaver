package org.jkiss.dbeaver.ext.greenplum.edit;

import org.jkiss.dbeaver.ext.greenplum.model.GreenplumDataSource;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumExternalTable;
import org.jkiss.dbeaver.ext.greenplum.model.GreenplumSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
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
public class GreenplumExternalTableManagerTest {
    @Mock
    private GreenplumSchema mockSchema;

    @Mock
    private PostgreDatabase mockDatabase;

    @Mock
    private ResultSet mockResults;

    @Mock
    private GreenplumDataSource mockDataSource;

    private GreenplumExternalTableManager greenplumExternalTableManager;

    @Before
    public void setUp() {
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDataSource.isServerVersionAtLeast(Matchers.anyInt(), Matchers.anyInt())).thenReturn(false);

        greenplumExternalTableManager = new GreenplumExternalTableManager();
    }

    @Test
    public void createDeleteAction_whenObjectIsAnExternalTable_thenExternalTableDropActionIsReturned() throws SQLException {
        SQLDatabasePersistAction regularTableDropTableQuery =
                new SQLDatabasePersistAction("Drop table", "DROP EXTERNAL TABLE foo.bar");

        Mockito.when(mockSchema.getName()).thenReturn("foo");
        Mockito.when(mockResults.getString("relname")).thenReturn("bar");

        GreenplumExternalTable greenplumExternalTable = newGreenplumExternalTableFixture();

        SQLDatabasePersistAction sqlDatabasePersistAction =
                greenplumExternalTableManager.createDeleteAction(greenplumExternalTable, Collections.emptyMap());

        Assert.assertEquals(regularTableDropTableQuery.getScript(), sqlDatabasePersistAction.getScript());
    }

    @Test
    public void createDeleteAction_whenCascadeOptionIsProvided_thenExternalTableDropActionIsReturnedWithCascadeOption()
            throws SQLException {
        SQLDatabasePersistAction regularTableDropTableQuery =
                new SQLDatabasePersistAction("Drop table", "DROP EXTERNAL TABLE foo.bar CASCADE");

        Mockito.when(mockSchema.getName()).thenReturn("foo");
        Mockito.when(mockResults.getString("relname")).thenReturn("bar");

        GreenplumExternalTable greenplumExternalTable = newGreenplumExternalTableFixture();

        SQLDatabasePersistAction sqlDatabasePersistAction =
                greenplumExternalTableManager.createDeleteAction(greenplumExternalTable,
                        Collections.singletonMap("deleteCascade", true));

        Assert.assertEquals(regularTableDropTableQuery.getScript(), sqlDatabasePersistAction.getScript());
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
