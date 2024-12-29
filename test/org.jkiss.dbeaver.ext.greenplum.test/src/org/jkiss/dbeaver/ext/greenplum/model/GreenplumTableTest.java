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

import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GreenplumTableTest extends DBeaverUnitTest {
    @Mock
    PostgreSchema mockSchema;

    @Mock
    PostgreDatabase mockDatabase;

    @Mock
    JDBCResultSet mockResults;

    @Mock
    JDBCExecutionContext mockContext;

    @Mock
    GreenplumDataSource mockDataSource;

    @Mock
    DBRProgressMonitor mockMonitor;

    @Mock
    PostgreSchema.TableCache mockTableCache;

    @Mock
    PostgreSchema.ConstraintCache mockConstraintCache;

    @Mock
    private PostgreServerGreenplum mockServerGreenplum;

    private GreenplumTable table;

    private final String exampleDatabaseName = "sampleDatabase";
    private final String exampleSchemaName = "sampleSchema";
    private final String exampleTableName = "sampleTable";

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockSchema.getSchema()).thenReturn(mockSchema);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockSchema.getName()).thenReturn(exampleSchemaName);
        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);
        Mockito.when(mockSchema.getConstraintCache()).thenReturn(mockConstraintCache);
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);

        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockDataSource.getDefaultInstance()).thenReturn(mockDatabase);
        Mockito.when(mockDataSource.getServerType()).thenReturn(mockServerGreenplum);

        Mockito.when(mockDatabase.getDefaultContext(Mockito.any(), Mockito.anyBoolean())).thenReturn(mockContext);
        Mockito.when(mockDatabase.isInstanceConnected()).thenReturn(true);

        Mockito.when(mockResults.getString("relname")).thenReturn(exampleTableName);
        Mockito.when(mockResults.getString("relpersistence")).thenReturn("x");
    }

    @Test
    public void appendTableModifiers_whenServerVersion8_andNoColumnSetForDistribution_resultsInRandom() throws Exception {
        StringBuilder ddl = new StringBuilder();

        JDBCResultSet mockDCResults = mockResults(mockMonitor);
        Mockito.when(mockDCResults.next()).thenReturn(false);

        List<PostgreTableConstraint> constraints = Collections.emptyList();

        table = new GreenplumTable(mockSchema, mockResults);

        Mockito.when(mockConstraintCache.getTypedObjects(mockMonitor, mockSchema, table, PostgreTableConstraint.class))
                .thenReturn(constraints);

        table.appendTableModifiers(mockMonitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED RANDOMLY", ddl.toString());
    }


    @Test
    public void appendTableModifiers_whenServerVersion8_andSingleColumnSetForDistribution_resultsInDistributedByThatColumn() throws Exception {
        StringBuilder ddl = new StringBuilder();

        JDBCResultSet mockDCResults = mockResults(mockMonitor);
        Mockito.when(mockDCResults.next()).thenReturn(true);
        Mockito.when(mockDCResults.getObject(1)).thenReturn(new int[]{1});

        List<PostgreTableColumn> mockColumns = createMockColumns("Column_Name");

        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);

        table = new GreenplumTable(mockSchema, mockResults);
        Mockito.when(mockTableCache.getChildren(mockMonitor, mockSchema, table))
                .thenReturn(mockColumns);

        table.appendTableModifiers(mockMonitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED BY (\"Column_Name\")", ddl.toString());
    }

    @Test
    public void appendTableModifiers_whenServerVersion8_andMultipleSingleColumnSetForDistribution_resultsInDistributedByThoseColumns() throws Exception {
        StringBuilder ddl = new StringBuilder();

        JDBCResultSet mockDCResults = mockResults(mockMonitor);
        Mockito.when(mockDCResults.next()).thenReturn(true);
        Mockito.when(mockDCResults.getObject(1)).thenReturn(new int[]{1, 2});

        List<PostgreTableColumn> mockColumns = createMockColumns("Column_1", "Column_2");

        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);

        table = new GreenplumTable(mockSchema, mockResults);

        Mockito.when(mockTableCache.getChildren(mockMonitor, mockSchema, table))
                .thenReturn(mockColumns);

        table.appendTableModifiers(mockMonitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED BY (\"Column_1\", \"Column_2\")", ddl.toString());
    }


    @Test
    public void appendTableModifiers_whenServerVersion9_andNotReplicated_andNoColumnSetForDistribution_resultsInRandom() throws Exception {
        StringBuilder ddl = new StringBuilder();

        JDBCResultSet mockDCResults = mockResults(mockMonitor);
        Mockito.when(mockDCResults.next()).thenReturn(false, true);
        Mockito.when(mockDCResults.getString(1)).thenReturn("x");

        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);

        table = new GreenplumTable(mockSchema, mockResults);

        table.appendTableModifiers(mockMonitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED RANDOMLY", ddl.toString());
    }

    @Test
    public void appendTableModifiers_whenServerVersion9_andIsReplicated_resultsInReplicated() throws Exception {
        StringBuilder ddl = new StringBuilder();

        JDBCResultSet mockDCResults = mockResults(mockMonitor);
        Mockito.when(mockDCResults.next()).thenReturn(false, true);
        Mockito.when(mockDCResults.getString(1)).thenReturn("r");

        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);

        table = new GreenplumTable(mockSchema, mockResults);

        table.appendTableModifiers(mockMonitor, ddl);

        Assert.assertEquals("\nDISTRIBUTED REPLICATED", ddl.toString());
    }

    private JDBCResultSet mockResults(DBRProgressMonitor monitor) throws SQLException {
        JDBCSession mockSession = Mockito.mock(JDBCSession.class);
        JDBCStatement mockStatement = Mockito.mock(JDBCStatement.class);
        JDBCResultSet mockDCResults = Mockito.mock(JDBCResultSet.class);

        Mockito.when(mockContext.openSession(Mockito.eq(monitor), Mockito.eq(DBCExecutionPurpose.META),
                Mockito.anyString())).thenReturn(mockSession);
        Mockito.when(mockSession.createStatement()).thenReturn(mockStatement);
        Mockito.when(mockStatement.executeQuery(Mockito.anyString())).thenReturn(mockDCResults);

        return mockDCResults;
    }

    private List<PostgreTableColumn> createMockColumns(String... columns) {
        return IntStream.range(0, columns.length)
                .mapToObj(i -> {
                    String columnName = columns[i];
                    PostgreTableColumn mockColumn = Mockito.mock(PostgreTableColumn.class);
                    Mockito.when(mockColumn.getOrdinalPosition()).thenReturn(i + 1);
                    Mockito.when(mockColumn.getDataSource()).thenReturn(mockDataSource);
                    Mockito.when(mockColumn.getName()).thenReturn(columnName);
                    return mockColumn;
                }).collect(Collectors.toList());
    }
}