/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.edit.YashanDBTableManager;
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
public class YashanDBBaseTableTest {

    @Mock
    private DBRProgressMonitor monitor;

    private YashanDBDataSource testDataSource;
    private YashanDBSchema testSchema;
    private YashanDBTable yashanDBTable;
    private YashanDBExecutionContext executionContext;
    private DBEObjectMaker<YashanDBTable, YashanDBSchema> objectMaker;

    @Mock
    private DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    private JDBCRemoteInstance mockRemoteInstance;

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Before
    public void setUp() throws DBException {
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("yashandb"));

        testDataSource = new YashanDBDataSource(mockDataSourceContainer);

        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(testDataSource);

        executionContext = new YashanDBExecutionContext(mockRemoteInstance, "Test");
        testSchema = new YashanDBSchema(testDataSource, -1, "TEST_SCHEMA");

        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(DBWorkbench.getPlatform().getPreferenceStore());

        objectMaker = YashanDBTestUtils.getManagerForClass(YashanDBTable.class);

        yashanDBTable = new YashanDBTable(testSchema, "TEST_TABLE");
        YashanDBTableColumn tableColumn = YashanDBTestUtils.addColumn(yashanDBTable, "COLUMN1", "VARCHAR", 1);
        tableColumn.setMaxLength(100);
        YashanDBTableColumn tableColumn1 = YashanDBTestUtils.addColumn(yashanDBTable, "COLUMN2", "NUMBER", 2);
        tableColumn1.setPrecision(38);
        tableColumn1.setScale(4);
        YashanDBTableColumn tableColumn2 = YashanDBTestUtils.addColumn(yashanDBTable, "COLUMN3", "CHAR", 3);
        tableColumn2.setMaxLength(13);
    }

    @Test
    public void generateCreateTableWithTwoColumnsStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        YashanDBTable newObject = objectMaker.createNewObject(monitor, commandContext, testSchema, null, Collections.emptyMap());
        DBEObjectMaker objectManager = YashanDBTestUtils.getManagerForClass(YashanDBTableColumn.class);
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

        YashanDBTable newObject = objectMaker.createNewObject(
            monitor,
            commandContext,
            testSchema,
            null,
            Collections.emptyMap());
        DBEObjectMaker objectManager = YashanDBTestUtils.getManagerForClass(YashanDBTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        final DBSObject newColumn =
            objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        if (newColumn instanceof YashanDBTableColumn) {
            ((YashanDBTableColumn) newColumn).setRequired(true);
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

        YashanDBTable newObject = objectMaker.createNewObject(monitor, commandContext, testSchema, null, Collections.emptyMap());
        DBEObjectMaker<YashanDBTableColumn, YashanDBTableBase> objectManager = YashanDBTestUtils.getManagerForClass(YashanDBTableColumn.class);
        YashanDBTableColumn column1 = objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        DBEObjectMaker<YashanDBTableConstraint, YashanDBTableBase> constraintManager = YashanDBTestUtils.getManagerForClass(YashanDBTableConstraint.class);
        YashanDBTableConstraint constraint = constraintManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        constraint.setName("NEWTABLE_PK");
        constraint.setConstraintType(DBSEntityConstraintType.PRIMARY_KEY);
        YashanDBTableConstraintColumn constraintColumn = new YashanDBTableConstraintColumn(constraint, column1, 1);
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

        YashanDBTable newObject = objectMaker.createNewObject(monitor, commandContext, testSchema, null, Collections.emptyMap());
        DBEObjectMaker<YashanDBTableColumn, YashanDBTableBase> objectManager = YashanDBTestUtils.getManagerForClass(YashanDBTableColumn.class);
        YashanDBTableColumn column1 = objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
        column1.setComment("Test comment 1");
        YashanDBTableColumn column2 = objectManager.createNewObject(monitor, commandContext, newObject, null, Collections.emptyMap());
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

        if (objectMaker instanceof YashanDBTableManager) {
            ((YashanDBTableManager) objectMaker).renameObject(commandContext, yashanDBTable, Collections.emptyMap(), "NEW_TEST_TABLE");
        }

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE RENAME TO NEW_TEST_TABLE;" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateTableCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, yashanDBTable, yashanDBTable);
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

        objectMaker.deleteObject(commandContext, yashanDBTable, Collections.emptyMap());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "DROP TABLE TEST_SCHEMA.TEST_TABLE;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

}
