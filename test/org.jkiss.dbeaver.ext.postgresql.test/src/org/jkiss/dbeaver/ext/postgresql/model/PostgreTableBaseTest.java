/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.StandardConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PostgreTableBaseTest extends DBeaverUnitTest {

    @Mock
    DBRProgressMonitor monitor;

    private PostgreDataSource testDataSource;
    private PostgreDatabase testDatabase;
    private PostgreSchema testSchema;
    private PostgreView testView;
    private PostgreTableRegular testTableRegular;

    private PostgreExecutionContext postgreExecutionContext;

    @Mock
    JDBCResultSet mockResults;
    @Mock
    DBPDataSourceContainer mockDataSourceContainer;

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Before
    public void setUp() throws Exception {
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgresql"));

        testDataSource = new PostgreDataSource(mockDataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return major <= 10;
            }

            @Nullable
            @Override
            public PostgreDataType getLocalDataType(String typeName) {
                return super.getLocalDataType(typeName);
            }
        };

        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        testDatabase = testDataSource.createDatabaseImpl(monitor, "testdb", testUser, null, null, null);
        testSchema = new PostgreSchema(testDatabase, "test_schema", testUser);

        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(DBWorkbench.getPlatform().getPreferenceStore());

//        Mockito.when(mockResults.getString("relname")).thenReturn("sampleTable");
//        long sampleId = 111111;
//        Mockito.when(mockResults.getLong("oid")).thenReturn(sampleId);
//        Mockito.when(mockResults.getLong("relowner")).thenReturn(sampleId);

        postgreExecutionContext = new PostgreExecutionContext(testDatabase, "Test");

        // Test Regular table
        testTableRegular = new PostgreTableRegular(testSchema) {
            @Override
            public boolean isTablespaceSpecified() {
                return false;
            }
        };
        testTableRegular.setName("test_table_regular");
        testTableRegular.setPartition(false);
        testTableRegular.setPersisted(true);
        PostgreTestUtils.addColumn(testTableRegular, "column1", "int4", 1);

        // Test View
        testView = new PostgreView(testSchema);
        testView.setName("testView");
        testView.setPersisted(true);
    }

    // Tables DDL tests

    @Test
    public void generateTableDDL_whenTableHasOneColumn_returnDDLForASingleColumn() throws Exception {
        PostgreTableRegular tableRegular = new PostgreTableRegular(testSchema) {
            @Override
            public boolean isTablespaceSpecified() {
                return false;
            }
        };
        tableRegular.setName("test_table");
        tableRegular.setPartition(false);
        PostgreTestUtils.addColumn(tableRegular, "column1", "int4", 1);

        String expectedDDL =
                "CREATE TABLE test_schema.test_table (" + lineBreak +
                "\tcolumn1 int4 NULL" + lineBreak +
                ");" + lineBreak;

        String tableDDL = tableRegular.getObjectDefinitionText(monitor, Collections.emptyMap());
        Assert.assertEquals(expectedDDL, tableDDL);
    }

    @Test
    public void generateTableDDL_whenTableHasTwoColumns_returnDDLForTableWithTwoColumns() throws Exception {
        PostgreTableRegular tableRegular = new PostgreTableRegular(testSchema) {
            @Override
            public boolean isTablespaceSpecified() {
                return false;
            }
        };
        tableRegular.setName("test_table");
        tableRegular.setPartition(false);
        PostgreTestUtils.addColumn(tableRegular, "column1", "int4", 1);
        PostgreTestUtils.addColumn(tableRegular, "column2", "varchar", 2);

        String expectedDDL =
                "CREATE TABLE test_schema.test_table (" + lineBreak +
                "\tcolumn1 int4 NULL," + lineBreak +
                "\tcolumn2 varchar NULL" + lineBreak +
                ");" + lineBreak;

        String tableDDL = tableRegular.getObjectDefinitionText(monitor, Collections.emptyMap());
        Assert.assertEquals(expectedDDL, tableDDL);
    }

    // Generation table/view comment statement tests

    @Test
    public void generateBaseTableCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(postgreExecutionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testTableRegular, testTableRegular);
        pse.collectProperties();
        pse.setPropertyValue(monitor, DBConstants.PROP_ID_DESCRIPTION, "Test comment");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, postgreExecutionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "COMMENT ON TABLE test_schema.test_table_regular IS 'Test comment';" + lineBreak;
        Assert.assertEquals(expectedDDL, script);
    }

    @Test
    public void generateForeignTableCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(postgreExecutionContext, false);

        PostgreTableForeign tableForeign = new PostgreTableForeign(testSchema);
        tableForeign.setName("testForeignTable");
        tableForeign.setPersisted(true);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, tableForeign, tableForeign);
        pse.collectProperties();
        pse.setPropertyValue(monitor, DBConstants.PROP_ID_DESCRIPTION, "Test comment");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, postgreExecutionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "COMMENT ON FOREIGN TABLE test_schema.\"testForeignTable\" IS 'Test comment';" + lineBreak;
        Assert.assertEquals(expectedDDL, script);
    }

    @Test
    public void generateViewCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(postgreExecutionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testView, testView);
        pse.collectProperties();
        pse.setPropertyValue(monitor, DBConstants.PROP_ID_DESCRIPTION, "Test comment");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, postgreExecutionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "COMMENT ON VIEW test_schema.\"testView\" IS 'Test comment';" + lineBreak;
        Assert.assertEquals(expectedDDL, script);
    }

    @Test
    public void generateMaterializedViewCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(postgreExecutionContext, false);

        PostgreMaterializedView testMView = new PostgreMaterializedView(testSchema);
        testMView.setName("testMView");
        testMView.setPersisted(true);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testMView, testMView);
        pse.collectProperties();
        pse.setPropertyValue(monitor, DBConstants.PROP_ID_DESCRIPTION, "Test comment");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, postgreExecutionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "COMMENT ON MATERIALIZED VIEW test_schema.\"testMView\" IS 'Test comment';" + lineBreak;
        Assert.assertEquals(expectedDDL, script);
    }

    // Other tests

    @Test
    public void generateChangeOwnerQuery_whenProvidedView_thenShouldGenerateQuerySuccessfully() {
        Assert.assertEquals("ALTER TABLE " + testSchema.getName() + ".\"" + testView.getName() + "\" OWNER TO someOwner",
            testView.generateChangeOwnerQuery("someOwner", new HashMap<>()));
    }

    @Test
    public void generateExtensionDDL_whenExtensionHasPublicSchemaAndNoVersion_returnDDLForExtensionWithPublicSchemaAndWithoutVersion() throws Exception {
        PostgreExtension postgreExtension = new PostgreExtension(testDatabase);
        postgreExtension.setName("extName");
        String expectedDDL = "-- Extension: extName" + lineBreak + lineBreak +
                                "-- DROP EXTENSION extName;" + lineBreak + lineBreak +
                                "CREATE EXTENSION extName" + lineBreak + "\t" +
                                "SCHEMA \"public\"" + lineBreak + "\t" +
                                "VERSION null";
        String actualDDL = postgreExtension.getObjectDefinitionText(monitor, Collections.emptyMap());
        Assert.assertEquals(expectedDDL, actualDDL);
    }

}