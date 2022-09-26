/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.edit.OracleTableManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.utils.StandardConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OracleBaseTableTest {

    @Mock
    private DBRProgressMonitor monitor;

    private OracleDataSource testDataSource;
    private OracleSchema testSchema;
    private OracleTable oracleTable;
    private OracleExecutionContext executionContext;
    private DBEObjectMaker<OracleTable, OracleSchema> objectMaker;

    @Mock
    private DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    private JDBCRemoteInstance mockRemoteInstance;

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Before
    public void setUp() throws DBException {
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("oracle"));

        testDataSource = new OracleDataSource(mockDataSourceContainer);

        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(testDataSource);

        executionContext = new OracleExecutionContext(mockRemoteInstance, "Test");
        testSchema = new OracleSchema(testDataSource, -1, "TEST_SCHEMA");

        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(DBWorkbench.getPlatform().getPreferenceStore());

        objectMaker = OracleTestUtils.getManagerForClass(OracleTable.class);

        oracleTable = new OracleTable(testSchema, "TEST_TABLE");
        OracleTableColumn tableColumn = OracleTestUtils.addColumn(oracleTable, "COLUMN1", "VARCHAR", 1);
        tableColumn.setMaxLength(100);
        OracleTableColumn tableColumn1 = OracleTestUtils.addColumn(oracleTable, "COLUMN2", "NUMBER", 2);
        tableColumn1.setPrecision(38);
        tableColumn1.setScale(4);
        OracleTableColumn tableColumn2 = OracleTestUtils.addColumn(oracleTable, "COLUMN3", "CHAR", 3);
        tableColumn2.setMaxLength(13);
    }

    @Test
    public void generateCreateTableWithTwoColumnsStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        OracleTable newObject = objectMaker.createNewObject(monitor, commandContext, testSchema, null, Collections.emptyMap());
        DBEObjectMaker objectManager = OracleTestUtils.getManagerForClass(OracleTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE TEST_SCHEMA.NEWTABLE (" + lineBreak +
                "\tCOLUMN1 INTEGER NULL," + lineBreak +
                "\tCOLUMN2 INTEGER NULL" + lineBreak +
                ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateTableWithTwoColumnsOneNullableStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        OracleTable newObject = objectMaker.createNewObject(
            monitor,
            commandContext,
            testSchema,
            null,
            Collections.emptyMap());
        DBEObjectMaker objectManager = OracleTestUtils.getManagerForClass(OracleTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        final DBSObject newColumn =
            objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        if (newColumn instanceof OracleTableColumn) {
            ((OracleTableColumn) newColumn).setRequired(true);
        }
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(
            testDataSource,
            actions.toArray(new DBEPersistAction[0]),
            false);

        String expectedDDL = "CREATE TABLE TEST_SCHEMA.NEWTABLE (" + lineBreak +
            "\tCOLUMN1 INTEGER NULL," + lineBreak +
            "\tCOLUMN2 INTEGER NOT NULL" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateTableWithTwoColumnsAndPrimaryKeyStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        OracleTable newObject = objectMaker.createNewObject(monitor, commandContext, testSchema, null, Collections.emptyMap());
        DBEObjectMaker<OracleTableColumn, OracleTableBase> objectManager = OracleTestUtils.getManagerForClass(OracleTableColumn.class);
        OracleTableColumn column1 = objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        DBEObjectMaker<OracleTableConstraint, OracleTableBase> constraintManager = OracleTestUtils.getManagerForClass(OracleTableConstraint.class);
        OracleTableConstraint constraint = constraintManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        constraint.setName("NEWTABLE_PK");
        constraint.setConstraintType(DBSEntityConstraintType.PRIMARY_KEY);
        OracleTableConstraintColumn constraintColumn = new OracleTableConstraintColumn(constraint, column1, 1);
        constraint.setColumns(Collections.singletonList(constraintColumn));

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE TEST_SCHEMA.NEWTABLE (" + lineBreak +
                "\tCOLUMN1 INTEGER NULL," + lineBreak +
                "\tCOLUMN2 INTEGER NULL," + lineBreak +
                "\tCONSTRAINT NEWTABLE_PK PRIMARY KEY (COLUMN1)" + lineBreak +
                ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateTableWithTwoColumnsWithCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        OracleTable newObject = objectMaker.createNewObject(monitor, commandContext, testSchema, null, Collections.emptyMap());
        DBEObjectMaker<OracleTableColumn, OracleTableBase> objectManager = OracleTestUtils.getManagerForClass(OracleTableColumn.class);
        OracleTableColumn column1 = objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        column1.setComment("Test comment 1");
        OracleTableColumn column2 = objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        column2.setComment("Test comment 2");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE TEST_SCHEMA.NEWTABLE (" + lineBreak +
                "\tCOLUMN1 INTEGER NULL," + lineBreak +
                "\tCOLUMN2 INTEGER NULL" + lineBreak +
                ");" + lineBreak +
                "COMMENT ON COLUMN TEST_SCHEMA.NEWTABLE.COLUMN1 IS 'Test comment 1';" + lineBreak +
                "COMMENT ON COLUMN TEST_SCHEMA.NEWTABLE.COLUMN2 IS 'Test comment 2';" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableRenameStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        if (objectMaker instanceof OracleTableManager) {
            ((OracleTableManager) objectMaker).renameObject(commandContext, oracleTable, Collections.emptyMap(), "NEW_TEST_TABLE");
        }

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE RENAME TO NEW_TEST_TABLE;" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateTableCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, oracleTable, oracleTable);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "comment", "Test comment");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "COMMENT ON TABLE TEST_SCHEMA.TEST_TABLE IS 'Test comment';" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateDropTableStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.deleteObject(commandContext, oracleTable, Collections.emptyMap());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "DROP TABLE TEST_SCHEMA.TEST_TABLE;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

}
