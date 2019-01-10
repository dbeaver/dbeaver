package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    private final String exampleUriLocation = "gpfdist://filehost:8081/*.txt";
    private final String exampleEncoding = "UTF8";
    private final String exampleFormatOptions = "DELIMITER ','";
    private final String exampleFormatType = "c";
    private final String exampleExecLocation = "ALL_SEGMENTS";

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDatabase.getName()).thenReturn(exampleDatabaseName);
        Mockito.when(mockSchema.getName()).thenReturn(exampleSchemaName);
        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);
        Mockito.when(mockDataSource.isServerVersionAtLeast(Matchers.anyInt(), Matchers.anyInt())).thenReturn(false);

        Mockito.when(mockResults.getString("relname")).thenReturn(exampleTableName);
        Mockito.when(mockResults.getString("fmttype")).thenReturn(exampleFormatType);
        Mockito.when(mockResults.getString("urilocation")).thenReturn(exampleUriLocation);
        Mockito.when(mockResults.getString("fmtopts")).thenReturn(exampleFormatOptions);
        Mockito.when(mockResults.getString("encoding")).thenReturn(exampleEncoding);
        Mockito.when(mockResults.getString("execlocation")).thenReturn(exampleExecLocation);

        table = new GreenplumTable(mockSchema, mockResults);
    }

    @Test
    public void addUnloggedClause_whenTableDDLContainsCreateKeyword_itIsReplacedWithCreateUnlogged() {
        String tableDdl = "CREATE TABLE";

        Assert.assertEquals(table.addUnloggedClause(tableDdl), "CREATE UNLOGGED TABLE");
    }

    @Test
    public void addUnloggedClause_whenTableDDLDoesNotContainCreateKeyword_itIsReplacedWithCreateUnlogged() {
        String tableDdl = "SOME TABLE";

        Assert.assertEquals(table.addUnloggedClause(tableDdl), tableDdl);
    }
}