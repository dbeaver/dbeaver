package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GreenplumTable.class, JDBCUtils.class, DBUtils.class})
public class GreenplumTableTest {

    @Mock
    PostgreSchema mockSchema;

    @Mock
    PostgreDatabase mockDatabase;

    @Mock
    JDBCResultSet mockResults;

    @Mock
    PostgreDataSource mockDataSource;

    @Mock
    PostgreSchema.TableCache mockTableCache;

    private GreenplumTable table;

    private final String exampleDatabaseName = "sampleDatabase";
    private final String exampleSchemaName = "sampleSchema";
    private final String exampleTableName = "sampleTable";

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDatabase.getName()).thenReturn(exampleDatabaseName);
        Mockito.when(mockSchema.getName()).thenReturn(exampleSchemaName);
        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);
        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(false);

        Mockito.when(mockResults.getString("relname")).thenReturn(exampleTableName);
        Mockito.when(mockResults.getString("relpersistence")).thenReturn("x");
    }

    @Test
    public void addUnloggedClause_whenTableDDLContainsCreateKeyword_itIsReplacedWithCreateUnlogged() {
        String tableDdl = "CREATE TABLE";

        table = new GreenplumTable(mockSchema, mockResults);

        Assert.assertEquals(table.addUnloggedClause(tableDdl), "CREATE UNLOGGED TABLE");
    }

    @Test
    public void addUnloggedClause_whenTableDDLDoesNotContainCreateKeyword_itIsReplacedWithCreateUnlogged() {
        String tableDdl = "SOME TABLE";

        table = new GreenplumTable(mockSchema, mockResults);

        Assert.assertEquals(table.addUnloggedClause(tableDdl), tableDdl);
    }

    @Test
    public void appendTableModifiers_whenServerVersion8_andNoColumnSetForDistribution_resultsInRandom() throws Exception {
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        StringBuilder ddl = new StringBuilder();

        mockSetupForEmptyDistributionColumnList();

        table.appendTableModifiers(monitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED RANDOMLY", ddl.toString());
    }

    @Test
    public void appendTableModifiers_whenServerVersion8_andSingleColumnSetForDistribution_resultsInRandom() throws Exception {
        PowerMockito.spy(DBUtils.class);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        StringBuilder ddl = new StringBuilder();
        PostgreTableColumn column = Mockito.mock(PostgreTableColumn.class);

        Mockito.when(DBUtils.getQuotedIdentifier(column)).thenReturn("Column_Name");

        table = PowerMockito.spy(new GreenplumTable(mockSchema, mockResults));

        PowerMockito.doReturn(new ArrayList(Arrays.asList(column))).when(table, "getDistributionPolicy", Mockito.any());
        PowerMockito.doReturn(new ArrayList<PostgreTableColumn>()).when(table, "getPostgreTableColumns", Mockito.any(), Mockito.any());

        table.appendTableModifiers(monitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED BY (Column_Name)", ddl.toString());
    }

    @Test
    public void appendTableModifiers_whenServerVersion8_andMultipleColumnSetForDistribution_resultsInRandom() throws Exception {
        PowerMockito.spy(DBUtils.class);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        StringBuilder ddl = new StringBuilder();
        PostgreTableColumn column1 = Mockito.mock(PostgreTableColumn.class);
        PostgreTableColumn column2 = Mockito.mock(PostgreTableColumn.class);

        Mockito.when(DBUtils.getQuotedIdentifier(column1)).thenReturn("Column_1");
        Mockito.when(DBUtils.getQuotedIdentifier(column2)).thenReturn("Column_2");

        table = PowerMockito.spy(new GreenplumTable(mockSchema, mockResults));

        PowerMockito.doReturn(new ArrayList(Arrays.asList(column1, column2))).when(table, "getDistributionPolicy", Mockito.any());
        PowerMockito.doReturn(new ArrayList<PostgreTableColumn>()).when(table, "getPostgreTableColumns", Mockito.any(), Mockito.any());

        table.appendTableModifiers(monitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED BY (Column_1, Column_2)", ddl.toString());
    }

    @Test
    public void appendTableModifiers_whenServerVersion9_andNotReplicated_andNoColumnSetForDistribution_resultsInRandom() throws Exception {
        PowerMockito.spy(JDBCUtils.class);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        StringBuilder ddl = new StringBuilder();

        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);

        mockSetupForEmptyDistributionColumnList();

        PowerMockito.doReturn(false).when(table, "isDistributedByReplicated", Mockito.any());

        table.appendTableModifiers(monitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED RANDOMLY", ddl.toString());
    }

    @Test
    public void appendTableModifiers_whenServerVersion9_andIsReplicated_resultsInReplicated() throws Exception {
        PowerMockito.spy(JDBCUtils.class);
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        StringBuilder ddl = new StringBuilder();

        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);

        mockSetupForEmptyDistributionColumnList();

        PowerMockito.doReturn(true).when(table, "isDistributedByReplicated", Mockito.any());

        table.appendTableModifiers(monitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED REPLICATED", ddl.toString());
    }

    private void mockSetupForEmptyDistributionColumnList() throws Exception {
        table = PowerMockito.spy(new GreenplumTable(mockSchema, mockResults));

        PowerMockito.doReturn(new ArrayList<PostgreTableColumn>()).when(table, "getDistributionPolicy", Mockito.any());
        PowerMockito.doReturn(new ArrayList<PostgreTableColumn>()).when(table, "getPostgreTableColumns", Mockito.any(), Mockito.any());
    }
}