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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreClass;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;

public class PostgreServerGreenplumTest extends DBeaverUnitTest {
    @Mock
    GreenplumDataSource mockDataSource;

    @Mock
    PostgreSchema mockSchema;

    @Mock
    JDBCResultSet mockResults;

    @Mock
    DBRProgressMonitor monitor;

    @InjectMocks
    PostgreServerGreenplum server;

    @Before
    public void setup() throws SQLException {
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDataSource.isServerVersionAtLeast(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(false);
        Mockito.when(mockDataSource.getServerType()).thenReturn(server);
        Mockito.when(mockResults.getString("fmttype")).thenReturn("c");
        Mockito.when(mockResults.getString("urilocation")).thenReturn("gpfdist://filehost:8081/*.txt");
    }

    @Test
    public void createRelationOfClass_whenTableIsNotAGreenplumTable_returnsInstanceOfPostgresTableBase() {
        Assert.assertEquals(GreenplumTable.class,
                server.createRelationOfClass(mockSchema, PostgreClass.RelKind.p, mockResults).getClass());
    }

    @Test
    public void createRelationOfClass_whenTableTypeIsRegularAndTableIsANonExternalGreenplumTable_returnsInstanceOfGreenplumTable()
            throws SQLException {
        Mockito.when(mockResults.getBoolean("is_ext_table")).thenReturn(false);
        Assert.assertEquals(GreenplumTable.class,
                server.createRelationOfClass(mockSchema, PostgreClass.RelKind.r, mockResults).getClass());
    }

    @Test
    public void createRelationOfClass_whenTableTypeIsRegularAndTableIsAnExternalGreenplumTable_returnsInstanceOfGreenplumExternalTable()
            throws SQLException {
        Mockito.when(mockResults.getBoolean("is_ext_table")).thenReturn(true);
        Assert.assertEquals(GreenplumExternalTable.class,
                server.createRelationOfClass(mockSchema, PostgreClass.RelKind.r, mockResults).getClass());
    }

    @Test
    public void readTableDDL_whenTableIsNotAnInstanceOfGreenplumExternalTable_delegatesDDLcreationToParentClass()
            throws DBException {
        String expectedDelegatedResultFromParentClass = null;
        GreenplumTable table = Mockito.mock(GreenplumTable.class);
        Assert.assertEquals(expectedDelegatedResultFromParentClass, server.readTableDDL(monitor, table));
    }

    @Test
    public void readTableDDL_whenTableIsAnInstanceOfGreenplumExternalTable_delegatesToGpGenerateDDL()
            throws DBException {
        GreenplumExternalTable table = Mockito.mock(GreenplumExternalTable.class);
        server.readTableDDL(monitor, table);
        Mockito.verify(table).generateDDL(monitor);
    }

    @Test
    public void configureDialect_shouldContainGreenplumSpecificKeywords() {
        PostgreDialect dialect = new PostgreDialect();

        server.configureDialect(dialect);

        Assert.assertTrue(!dialect.getMatchedKeywords("DISTRIBUTED").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("SEGMENT").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("REJECT").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("FORMAT").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("MASTER").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("WEB").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("WRITABLE").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("READABLE").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("ERRORS").isEmpty());
        Assert.assertTrue(!dialect.getMatchedKeywords("LOG").isEmpty());
    }
}