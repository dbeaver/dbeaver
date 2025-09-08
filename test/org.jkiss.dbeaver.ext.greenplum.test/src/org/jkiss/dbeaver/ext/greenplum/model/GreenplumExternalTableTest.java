/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GreenplumExternalTableTest extends DBeaverUnitTest {
    @Mock
    DBRProgressMonitor monitor;

    @Mock
    PostgreSchema mockSchema;

    @Mock
    PostgreDatabase mockDatabase;

    @Mock
    JDBCResultSet mockResults;

    @Mock
    GreenplumDataSource mockDataSource;

    @Mock
    PostgreSchema.TableCache mockTableCache;

    @Mock
    private PostgreServerGreenplum mockServerGreenplum;

    private final String exampleDatabaseName = "sampleDatabase";
    private final String exampleSchemaName = "sampleSchema";
    private final String exampleTableName = "sampleTable";
    private final String exampleUriLocation = "gpfdist://filehost:8081/*.txt";
    private final String exampleEncoding = "UTF8";
    private final String exampleFormatOptions = "DELIMITER ','";
    private final String exampleFormatType = "c";
    private final int exampleRejectLimit = 100_000;
    private final String exampleRejectLimitType = "r";
    private final String exampleExecLocation = "ALL_SEGMENTS";

    @Before
    public void setup() throws SQLException {
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDatabase.getName()).thenReturn(exampleDatabaseName);
        Mockito.when(mockSchema.getSchema()).thenReturn(mockSchema);
        Mockito.when(mockSchema.getName()).thenReturn(exampleSchemaName);
        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockDataSource.getServerType()).thenReturn(mockServerGreenplum);
        Mockito.when(mockDataSource.isServerVersionAtLeast(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(false);

        Mockito.when(mockResults.getString("relname")).thenReturn(exampleTableName);
        Mockito.when(mockResults.getString("fmttype")).thenReturn(exampleFormatType);
        Mockito.when(mockResults.getString("urilocation")).thenReturn(exampleUriLocation);
        Mockito.when(mockResults.getString("fmtopts")).thenReturn(exampleFormatOptions);
        Mockito.when(mockResults.getString("encoding")).thenReturn(exampleEncoding);
        Mockito.when(mockResults.getString("execlocation")).thenReturn(exampleExecLocation);
    }

    @Test
    public void onCreation_whenNoInitialDbResultIsProvided_thenDefaultEncodingIsSetToUTF8() {
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema);
        Assert.assertEquals("UTF8", table.getEncoding());
    }

    @Test
    public void onCreation_whenNoInitialDbResultIsProvided_thenDefaultFormatIsText() {
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema);
        Assert.assertEquals("TEXT", table.getFormatType());
    }

    @Test
    public void onCreation_whenNoInitialDbResultIsProvided_thenDefaultFormatOptionsAreBasedOnTextFormatType() {
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema);
        Assert.assertEquals("delimiter ',' null '' escape '\"' quote '\"' header", table.getFormatOptions());
    }

    @Test
    public void onCreation_readsASingleUriLocationFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("urilocation")).thenReturn("SOME_EXTERNAL_LOCATION");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals("SOME_EXTERNAL_LOCATION", table.getUriLocations());
    }

    @Test
    public void onCreation_readsMultipleUriLocationsFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("urilocation"))
                .thenReturn("SOME_EXTERNAL_LOCATION,ANOTHER_EXTERNAL_LOCATION");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals("SOME_EXTERNAL_LOCATION,ANOTHER_EXTERNAL_LOCATION",
                table.getUriLocations());
    }

    @Test
    public void onCreation_readsNoLocationsFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("urilocation")).thenReturn("");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertTrue(table.getUriLocations().isEmpty());
    }

    @Test
    public void onCreation_readsExecLocationFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("execlocation")).thenReturn("ALL_SEGMENTS");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        String expectedUriLocation = "ALL_SEGMENTS";
        Assert.assertEquals(expectedUriLocation, table.getExecLocation());
    }

    @Test
    public void onCreation_readsFormatTypeFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("fmttype")).thenReturn("t");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals("TEXT", table.getFormatType());
    }

    @Test
    public void onCreation_readsFormatOptionsFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("fmtopts"))
                .thenReturn("delimiter ',' null '' escape '\"' quote '\"' header");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        String expectedUriLocation = "delimiter ',' null '' escape '\"' quote '\"' header";
        Assert.assertEquals(expectedUriLocation, table.getFormatOptions());
    }

    @Test
    public void onCreation_readsEncodingFromDBResult() throws SQLException {
        Mockito.when(mockResults.getString("encoding")).thenReturn("UTF8");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        String expectedUriLocation = "UTF8";
        Assert.assertEquals(expectedUriLocation, table.getEncoding());
    }

    @Test
    public void onCreation_readsRejectLimitTypeFromDBResult() throws SQLException {
        Mockito.when(mockResults.getString("rejectlimittype")).thenReturn("r");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals(GreenplumExternalTable.RejectLimitType.r, table.getRejectLimitType());
    }

    @Test
    public void onCreation_readsRejectLimitFromDBResult() throws SQLException {
        Mockito.when(mockResults.getInt("rejectlimit")).thenReturn(50_000);
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        int expectedUriLocation = 50_000;
        Assert.assertEquals(expectedUriLocation, table.getRejectLimit());
    }

    @Test
    public void onCreation_readsWritableFlagFromDBResult() throws SQLException {
        Mockito.when(mockResults.getBoolean("writable")).thenReturn(true);
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertTrue(table.isWritable());
    }

    @Test
    public void onCreation_readsTemporaryTableFlagFromDBResult() throws SQLException {
        Mockito.when(mockResults.getBoolean("is_temp_table")).thenReturn(true);
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertTrue(table.isTemporaryTable());
    }

    @Test
    public void onCreation_readsLoggingErrorsFlagFromDBResult() throws SQLException {
        Mockito.when(mockResults.getBoolean("is_logging_errors")).thenReturn(true);
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertTrue(table.isLoggingErrors());
    }

    @Test
    public void onCreation_setsDefaultLoggingErrorsToFalse_ifGreenplumServerVersionIs6andAbove() throws SQLException {
        // Greenplum 6 runs on Postgre 9.4.x
        Mockito.when(mockDataSource.isServerVersionAtLeast(9,4)).thenReturn(true);
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertFalse(table.isLoggingErrors());
        Mockito.verify(mockResults, Mockito.times(0)).getBoolean("is_logging_errors");
    }

    @Test
    public void generateDDL_whenTableHasASingleColumn_returnsDDLStringForASingleColumn()
            throws DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasNoEncodingSet_returnsDDLStringWithNoEncoding()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("encoding")).thenReturn(null);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDLWithNoEncodingSet =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )";

        Assert.assertEquals(expectedDDLWithNoEncodingSet, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasMultiColumns_returnsDDLStringForMultiColumns()
            throws DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        PostgreTableColumn mockPostgreTableColumn2 = mockDbColumn("column2", "int2", 2);
        List<PostgreTableColumn> tableColumns = Arrays.asList(mockPostgreTableColumn, mockPostgreTableColumn2);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDLWithMultiColumns =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4,\n\tcolumn2 int2\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDLWithMultiColumns, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasASegmentRejectLimit_returnsDDLStringWithSegmentRejectLimit()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getInt("rejectlimit")).thenReturn(exampleRejectLimit);
        Mockito.when(mockResults.getString("rejectlimittype")).thenReturn(exampleRejectLimitType);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'\n" +
                        "SEGMENT REJECT LIMIT 100000 ROWS";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenExecLocationIsMasterOnly_returnsDDLStringWithAMasterOnlyExecLocation()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("execlocation")).thenReturn("MASTER_ONLY");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON MASTER\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasMultipleUriLocations_returnsDDLStringForASingleColumn()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("urilocation"))
                .thenReturn("gpfdist://filehost:8081/*.txt,gpfdist://filehost:8081/*.gz");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt',\n" +
                        "\t'gpfdist://filehost:8081/*.gz'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasACustomFormatType_returnsDDLStringWithACustomFormat()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("fmttype")).thenReturn("b");
        Mockito.when(mockResults.getString("fmtopts")).thenReturn("FORMATTER 'formatter_export_s'");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CUSTOM' ( FORMATTER='formatter_export_s' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableIsAWebTable_returnsDDLStringForAWebTable()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("urilocation")).thenReturn("http://example.com/test.txt");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL WEB TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'http://example.com/test.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableIsAnExternalWritableTable_returnsDDLStringForAWritableTable()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getBoolean("writable")).thenReturn(true);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE WRITABLE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableIsAExternalTemporaryTable_returnsDDLStringForAExternalTemporaryTable()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getBoolean("is_temp_table")).thenReturn(true);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TEMPORARY TABLE sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableIsAExternalWebTemporaryTable_returnsDDLStringForAExternalWebTemporaryTable()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getBoolean("is_temp_table")).thenReturn(true);
        Mockito.when(mockResults.getString("urilocation")).thenReturn("http://example.com/test.txt");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL WEB TEMPORARY TABLE sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'http://example.com/test.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenExternalTableIsLoggingErrors_returnsDDLStringWithLoggingErrorsClause()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getBoolean("is_logging_errors")).thenReturn(true);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'\n" +
                        "LOG ERRORS";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenExternalTableIsLoggingErrorsWithSegmentRejectLimit_returnsDDLStringWithLoggingErrorsClauseWithSegmentRejectLimit()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getInt("rejectlimit")).thenReturn(exampleRejectLimit);
        Mockito.when(mockResults.getString("rejectlimittype")).thenReturn(exampleRejectLimitType);
        Mockito.when(mockResults.getBoolean("is_logging_errors")).thenReturn(true);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'\n" +
                        "LOG ERRORS SEGMENT REJECT LIMIT 100000 ROWS";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenExternalTableHasNoFormatOptionsSet_returnsDDLStringWithOmittedFormatOptions()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("fmtopts")).thenReturn(null);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV'\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasNoColumns_returnsDDLStringForATableWithNoColumns()
            throws DBException {
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(null, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableIsNotYetPersisted_returnsDDLStringForColumnsToBeCreated()
            throws DBException {
        // Ordinal Position of -1 is applied to all non-persisted table columns
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", -1);
        PostgreTableColumn mockPostgreTableColumn2 = mockDbColumn("column2", "int2", -1);
        List<PostgreTableColumn> tableColumns = Arrays.asList(mockPostgreTableColumn, mockPostgreTableColumn2);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema);
        table.setName("sampleTable");
        table.setUriLocations(exampleUriLocation);
        table.setPersisted(false);

        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4,\n\tcolumn2 int2\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'TEXT' ( delimiter ',' null '' escape '\"' quote '\"' header )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenAWebTableHasExecuteClause_returnsDDLWithTheExecuteClauseAndDefaultExecLocation() throws SQLException, DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("command")).thenReturn("execute something");
        Mockito.when(mockResults.getString("urilocation")).thenReturn("");
        Mockito.when(mockResults.getString("fmttype")).thenReturn("t");
        Mockito.when(mockResults.getString("fmtopts")).thenReturn("");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL WEB TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "EXECUTE 'execute something' ON ALL\n" +
                        "FORMAT 'TEXT'\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenAWebTableHasExecuteClauseAndExecLocationIsMasterOnly_returnsDDLWithTheExecuteClauseAndMasterOnlyExecLocation() throws SQLException, DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("command")).thenReturn("execute something");
        Mockito.when(mockResults.getString("urilocation")).thenReturn("");
        Mockito.when(mockResults.getString("fmttype")).thenReturn("t");
        Mockito.when(mockResults.getString("fmtopts")).thenReturn("");
        Mockito.when(mockResults.getString("execlocation")).thenReturn("MASTER_ONLY");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL WEB TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "EXECUTE 'execute something' ON MASTER\n" +
                        "FORMAT 'TEXT'\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void setFormatType_whenProvidedAValidStringRepresentationOfFormatType_setsFormatTypeEnumSuccessfully() {
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema);
        table.setFormatType("CUSTOM");
        Assert.assertEquals("CUSTOM", table.getFormatType());
    }

    @Test
    public void generateChangeOwnerQuery_whenProvidedAValidOwner_thenShouldGenerateQuerySuccessfully() {
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals("ALTER EXTERNAL TABLE \"sampleSchema\".\"sampleTable\" OWNER TO someOwner",
                table.generateChangeOwnerQuery("someOwner", new HashMap<>()));
    }

    private PostgreTableColumn mockDbColumn(String columnName, String columnType, int ordinalPosition) {
        PostgreTableColumn mockPostgreTableColumn = Mockito.mock(PostgreTableColumn.class);
        Mockito.when(mockPostgreTableColumn.getName()).thenReturn(columnName);
        Mockito.when(mockPostgreTableColumn.getTypeName()).thenReturn(columnType);
        Mockito.when(mockPostgreTableColumn.getOrdinalPosition()).thenReturn(ordinalPosition);
        return mockPostgreTableColumn;
    }

    private void addMockColumnsToTableCache(List<PostgreTableColumn> tableColumns, GreenplumExternalTable table)
            throws DBException {
        Mockito.when(mockTableCache.getChildren(monitor, mockSchema, table)).thenReturn(tableColumns);
    }
}