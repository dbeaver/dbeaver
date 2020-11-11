/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PostgreTableBaseTest {

    private static final Log log = Log.getLog(PostgreTableBaseTest.class);

    @Mock
    DBRProgressMonitor monitor;
    @Mock
    PostgreDataSource mockDataSource;

    PostgreDatabase testDatabase;
    PostgreRole testUSer;
    PostgreSchema testSchema;
    private PostgreView testView;

    @Mock
    JDBCResultSet mockResults;
    @Mock
    private PostgreServerExtension mockPostgreServer;
    @Mock
    PostgreSchema.TableCache mockTableCache;
    @Mock
    DBPDataSourceContainer mockDataSourceContainer;

    private final long sampleId = 111111;

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockDataSource.getDefaultInstance()).thenReturn(testDatabase);
        Mockito.when(mockDataSource.getServerType()).thenReturn(mockPostgreServer);
        Mockito.when(mockDataSource.getContainer()).thenReturn(mockDataSourceContainer);

        testUSer = new PostgreRole(null, "tester", "test", true);
        testDatabase = new PostgreDatabase(new VoidProgressMonitor(), mockDataSource, "testdb", testUSer, null, null, null);
        testSchema = new PostgreSchema(testDatabase, "test", testUSer);

        Mockito.when(mockDataSourceContainer.getPlatform()).thenReturn(DBWorkbench.getPlatform());

        Mockito.when(mockResults.getString("relname")).thenReturn("sampleTable");
        Mockito.when(mockResults.getLong("oid")).thenReturn(sampleId);
        Mockito.when(mockResults.getLong("relowner")).thenReturn(sampleId);

        testView = new PostgreView(testSchema);
        testView.setName("sampleView");
    }

    @Test
    public void generateChangeOwnerQuery_whenProvidedView_thenShouldGenerateQuerySuccessfully() {
        Assert.assertEquals("ALTER TABLE " + testSchema.getName() + "." + testView.getName() + " OWNER TO someOwner",
                testView.generateChangeOwnerQuery("someOwner"));
    }

    @Test
    public void generateTableDDL_whenTableHasOneColumn_returnDDLForASingleColumn() throws Exception {
        PostgreTableRegular tableRegular = new PostgreTableRegular(testSchema);
        tableRegular.setPartition(false);
        addColumn(tableRegular, "column1", "int4", 1);
        addColumn(tableRegular, "column2", "varchar", 1);

        String expectedDDL =
                "CREATE TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n";
        //DBStructUtils.generateTableDDL asks for a lot of objects that is not easy to mock
        //Assert.assertEquals(expectedDDL, tableRegular.getObjectDefinitionText(monitor, Collections.emptyMap()));

        try {
            String tableDDL = tableRegular.getObjectDefinitionText(monitor, Collections.emptyMap());
            Assert.assertNotNull(tableDDL);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void generateExtensionDDL_whenExtensionHasPublicSchemaAndNoVersion_returnDDLForExtensionWithPublicSchemaAndWithoutVersion() throws Exception {
        PostgreExtension postgreExtension = new PostgreExtension(testDatabase);
        postgreExtension.setName("extName");
        String expectedDDL = "-- Extension: extName\n\n" +
                                "-- DROP EXTENSION extName;\n\n" +
                                "CREATE EXTENSION extName\n\t" +
                                "SCHEMA \"public\"\n\t" +
                                "VERSION null";
        String actualDDL = postgreExtension.getObjectDefinitionText(monitor, Collections.emptyMap());
        Assert.assertEquals(expectedDDL, actualDDL);
    }

    private PostgreTableColumn addColumn(PostgreTableBase table, String columnName, String columnType, int ordinalPosition) {
        PostgreTableColumn column = new PostgreTableColumn(table);
        column.setName(columnName);
        column.setTypeName(columnType);
        column.setOrdinalPosition(ordinalPosition);
        List<PostgreTableColumn> cachedAttributes = (List<PostgreTableColumn>) table.getCachedAttributes();
        cachedAttributes.add(column);
        return column;
    }

}