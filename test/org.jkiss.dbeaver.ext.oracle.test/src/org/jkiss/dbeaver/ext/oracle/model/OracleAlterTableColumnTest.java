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

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.edit.OracleTableColumnManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
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

import java.sql.Types;
import java.util.Collections;
import java.util.List;

public class OracleAlterTableColumnTest extends DBeaverUnitTest {

    @Mock
    private DBRProgressMonitor monitor;

    private OracleDataSource testDataSource;
    private OracleTableBase oracleTableBase;
    private OracleTableColumn testColumnVarchar;
    private OracleTableColumn testColumnNumber;
    private OracleTableColumn testColumnChar;
    private OracleExecutionContext executionContext;
    private DBEObjectMaker<OracleTableColumn, OracleTableBase> objectMaker;

    @Mock
    private DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    private JDBCRemoteInstance mockRemoteInstance;
    @Mock
    private DBPConnectionConfiguration mockConnectionConfiguration;

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Before
    public void setUp() throws DBException {
        DBPPlatform dbpPlatform = DBWorkbench.getPlatform();
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(dbpPlatform.getDataSourceProviderRegistry().findDriver("oracle"));
        Mockito.when(mockDataSourceContainer.getConnectionConfiguration()).thenReturn(mockConnectionConfiguration);

        testDataSource = new OracleDataSource(mockDataSourceContainer);

        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(testDataSource);

        executionContext = new OracleExecutionContext(mockRemoteInstance, "Test");
        OracleSchema testSchema = new OracleSchema(testDataSource, -1, "TEST_SCHEMA");

        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(dbpPlatform.getPreferenceStore());

        objectMaker = OracleTestUtils.getManagerForClass(OracleTableColumn.class);

        oracleTableBase = new OracleTable(testSchema, "TEST_TABLE");
        testColumnVarchar = OracleTestUtils.addColumn(oracleTableBase, "COLUMN1", "VARCHAR", 1);
        testColumnVarchar.setMaxLength(100);
        testColumnVarchar.setPersisted(true);
        testColumnNumber = OracleTestUtils.addColumn(oracleTableBase, "COLUMN2", "NUMBER", 2);
        testColumnNumber.setPrecision(38);
        testColumnNumber.setScale(0);
        testColumnNumber.setPersisted(true);
        testColumnChar = OracleTestUtils.addColumn(oracleTableBase, "COLUMN3", "CHAR", 3);
        testColumnChar.setPersisted(true);
    }

    @Test
    public void generateAlterTableAddColumnStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        OracleTableColumn newColumn = objectMaker.createNewObject(
            monitor,
            commandContext,
            oracleTableBase,
            null,
            Collections.emptyMap()
        );
        newColumn.setTypeName("INTEGER");
        newColumn.setValueType(Types.INTEGER);
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE ADD COLUMN4 INTEGER NULL;" + lineBreak;

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

        String expectedDDL = "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN1 IS 'Test comment';" + lineBreak;
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

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(100) NOT NULL;" + lineBreak;
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
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(100) DEFAULT 'Test value';" + lineBreak;
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
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN2 NUMBER(38,0) DEFAULT 42;" + lineBreak;
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
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(50) DEFAULT 'Test value';" + lineBreak;
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

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN3 CHAR(33);" + lineBreak;
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

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN2 NUMBER(22,0);" + lineBreak;
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
            "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN2 NUMBER(38,17) DEFAULT 42;" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableSetNullConditionStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        PropertySourceEditable pse = new PropertySourceEditable(commandContext, testColumnVarchar, testColumnVarchar);
        pse.collectProperties();
        testColumnVarchar.setRequired(true);
        pse.setPropertyValue(monitor, "required", false);

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);

        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE MODIFY COLUMN1 VARCHAR(100) NULL;" + lineBreak;
        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableRenameColumnStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        if (objectMaker instanceof OracleTableColumnManager) {
            ((OracleTableColumnManager) objectMaker).renameObject(commandContext, testColumnChar, Collections.emptyMap(), "COLUMN33");
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