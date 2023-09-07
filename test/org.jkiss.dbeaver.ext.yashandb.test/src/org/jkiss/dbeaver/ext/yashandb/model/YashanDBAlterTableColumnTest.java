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
import org.jkiss.dbeaver.ext.yashandb.edit.YashanDBTableColumnManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
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
public class YashanDBAlterTableColumnTest {

    @Mock
    private DBRProgressMonitor monitor;

    private YashanDBDataSource testDataSource;
    private YashanDBTableBase yashanDBTableBase;
    private YashanDBTableColumn testColumnVarchar;
    private YashanDBTableColumn testColumnNumber;
    private YashanDBTableColumn testColumnChar;
    private YashanDBExecutionContext executionContext;
    private DBEObjectMaker<YashanDBTableColumn, YashanDBTableBase> objectMaker;

    @Mock
    private DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    private JDBCRemoteInstance mockRemoteInstance;

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Before
    public void setUp() throws DBException {
        DBPPlatform dbpPlatform = DBWorkbench.getPlatform();
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(dbpPlatform.getDataSourceProviderRegistry().findDriver("yashandb"));

        testDataSource = new YashanDBDataSource(mockDataSourceContainer);

        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(testDataSource);

        executionContext = new YashanDBExecutionContext(mockRemoteInstance, "Test");
        YashanDBSchema testSchema = new YashanDBSchema(testDataSource, -1, "TEST_SCHEMA");

        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(dbpPlatform.getPreferenceStore());

        objectMaker = YashanDBTestUtils.getManagerForClass(YashanDBTableColumn.class);

        yashanDBTableBase = new YashanDBTable(testSchema, "TEST_TABLE");
        testColumnVarchar = YashanDBTestUtils.addColumn(yashanDBTableBase, "COLUMN1", "VARCHAR", 1);
        testColumnVarchar.setMaxLength(100);
        testColumnNumber = YashanDBTestUtils.addColumn(yashanDBTableBase, "COLUMN2", "NUMBER", 2);
        testColumnNumber.setPrecision(38);
        testColumnNumber.setScale(0);
        testColumnChar = YashanDBTestUtils.addColumn(yashanDBTableBase, "COLUMN3", "CHAR", 3);
    }

    @Test
    public void generateAlterTableAddColumnStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.createNewObject(monitor, commandContext, yashanDBTableBase, null, Collections.emptyMap());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE ADD COLUMN4 INTEGER;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableSetColumnCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnVarchar, testColumnVarchar);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "comment", "Test comment");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(100);"+lineBreak
        		+ "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN1 IS 'Test comment';" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableSetNotNullConditionStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnVarchar, testColumnVarchar);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "required", true);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(100) NOT NULL;" + lineBreak
        		+"COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN1 IS '';"+lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableSetStringDefaultValueStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnVarchar, testColumnVarchar);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "defaultValue", "'Test value'");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL =
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(100) DEFAULT 'Test value';\r\n"
            + "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN1 IS '';\r\n";
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableSetNumericDefaultValueStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnNumber, testColumnNumber);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "defaultValue", "42");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);
        String expectedDDL =
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN2 NUMBER(38,0) DEFAULT 42;" + lineBreak
            +"COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN2 IS '';"+lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableChangeMaxLengthByColumnWithDefaultValueStatement() throws Exception {
        testColumnVarchar.setDefaultValue("'Test value'");
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnVarchar, testColumnVarchar);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "maxLength", 50);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL =
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(50) DEFAULT 'Test value';\r\n"
            + "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN1 IS '';\r\n";
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableChangeMaxLengthByColumnWithoutDefaultValueStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnChar, testColumnChar);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "maxLength", 33);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);
        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN3 CHAR(33);"+lineBreak
        		+ "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN3 IS '';"+lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableAlterNumericColumnChangePrecisionStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnNumber, testColumnNumber);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "precision", 22);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN2 NUMBER(22,0);\r\n"
        		+ "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN2 IS '';\r\n"
        		+ "";
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableAlterNumericColumnWithDefaultValueChangeScaleStatement() throws Exception {
        testColumnNumber.setDefaultValue("42");
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnNumber, testColumnNumber);
        pse.collectProperties();
        pse.setPropertyValue(monitor, "scale", 17);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL =
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN2 NUMBER(38,17) DEFAULT 42;\r\n"
            + "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN2 IS '';\r\n";
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableRenameColumnStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        if (objectMaker instanceof YashanDBTableColumnManager) {
            ((YashanDBTableColumnManager) objectMaker).renameObject(commandContext, testColumnChar, Collections.emptyMap(), "COLUMN33");
        }

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE RENAME COLUMN COLUMN3 TO COLUMN33;" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableDropColumnStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.deleteObject(commandContext, testColumnVarchar, Collections.emptyMap());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE DROP COLUMN COLUMN1;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }
}