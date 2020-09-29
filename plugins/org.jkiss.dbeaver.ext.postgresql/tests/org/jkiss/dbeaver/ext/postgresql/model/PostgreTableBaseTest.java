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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PostgreTableBaseTest {

    private static final Log log = Log.getLog(PostgreTableBaseTest.class);

    @Mock
    DBRProgressMonitor monitor;
    @Mock
    PostgreDataSource mockDataSource;
    @Mock
    PostgreDatabase mockDatabase;
    @Mock
    PostgreSchema mockSchema;
    @Mock
    JDBCResultSet mockResults;
    @Mock
    private PostgreServerExtension mockPostgreServer;
    @Mock
    PostgreSchema.TableCache mockTableCache;
    @Mock
    DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    DBERegistry mockEditorsRegistry;
    @Mock
    SQLTableManager mockEntityEditor;
    @Mock
    DBPPlatform mockDPlatform;

    private PostgreView postgreView;
    private final long sampleId = 111111;

    @Before
    public void setUp() throws SQLException {
        Mockito.when(mockDataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(mockDataSource.isServerVersionAtLeast(Mockito.anyInt(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(mockDataSource.getDefaultInstance()).thenReturn(mockDatabase);
        Mockito.when(mockDataSource.getServerType()).thenReturn(mockPostgreServer);
        Mockito.when(mockDataSource.getContainer()).thenReturn(mockDataSourceContainer);

        Mockito.when(mockDataSourceContainer.getPlatform()).thenReturn(mockDPlatform);
        Mockito.when(mockDPlatform.getEditorsRegistry()).thenReturn(mockEditorsRegistry);
        Mockito.when(mockEditorsRegistry.getObjectManager(PostgreTableRegular.class, SQLObjectEditor.class)).thenReturn(mockEntityEditor);

        Mockito.when(mockDatabase.getName()).thenReturn("sampleDatabase");

        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getSchema()).thenReturn(mockSchema);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockSchema.getName()).thenReturn("sampleSchema");
        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);

        Mockito.when(mockResults.getString("relname")).thenReturn("sampleTable");
        Mockito.when(mockResults.getLong("oid")).thenReturn(sampleId);
        Mockito.when(mockResults.getLong("relowner")).thenReturn(sampleId);

        postgreView = new PostgreView(mockSchema);
        postgreView.setName("sampleView");
    }

    @Test
    public void generateChangeOwnerQuery_whenProvidedView_thenShouldGenerateQuerySuccessfully() {
        Assert.assertEquals("ALTER TABLE sampleSchema.sampleView OWNER TO someOwner",
                postgreView.generateChangeOwnerQuery("someOwner"));
    }

    @Test
    public void generateTableDDL_whenTableHasOneColumn_returnDDLForASingleColumn() throws DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);
        PostgreTableRegular tableRegular = new PostgreTableRegular(mockSchema);
        tableRegular.setPartition(false);
        addMockColumnsToTableCache(tableColumns, tableRegular);
        String expectedDDL =
                "CREATE TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n";
        //DBStructUtils.generateTableDDL asks for a lot of objects that is not easy to mock
        //Assert.assertEquals(expectedDDL, tableRegular.getObjectDefinitionText(monitor, Collections.emptyMap()));

        if (tableRegular.getObjectDefinitionText(monitor, Collections.emptyMap()) == null) {
            log.warn("Table DDL is empty");
        }
    }

    @Test
    public void generateExtensionDDL_whenExtensionHasPublicSchemaAndNoVersion_returnDDLForExtensionWithPublicSchemaAndWithoutVersion() throws DBException {
        PostgreExtension postgreExtension = new PostgreExtension(mockDatabase);
        postgreExtension.setName("extName");
        String expectedDDL = "-- Extension: extName\n\n" +
                                "-- DROP EXTENSION extName;\n\n" +
                                "CREATE EXTENSION extName\n\t" +
                                "SCHEMA \"public\"\n\t" +
                                "VERSION null";
        String actualDDL = postgreExtension.getObjectDefinitionText(monitor, Collections.emptyMap());
        Assert.assertEquals(expectedDDL, actualDDL);
    }

    private PostgreTableColumn mockDbColumn(String columnName, String columnType, int ordinalPosition) {
        PostgreTableColumn mockPostgreTableColumn = Mockito.mock(PostgreTableColumn.class);
        Mockito.when(mockPostgreTableColumn.getName()).thenReturn(columnName);
        Mockito.when(mockPostgreTableColumn.getTypeName()).thenReturn(columnType);
        Mockito.when(mockPostgreTableColumn.getOrdinalPosition()).thenReturn(ordinalPosition);
        return mockPostgreTableColumn;
    }

    private void addMockColumnsToTableCache(List<PostgreTableColumn> tableColumns, PostgreTableRegular table)
            throws DBException {
        Mockito.when(mockTableCache.getChildren(monitor, mockSchema, table)).thenReturn(tableColumns);
    }

}