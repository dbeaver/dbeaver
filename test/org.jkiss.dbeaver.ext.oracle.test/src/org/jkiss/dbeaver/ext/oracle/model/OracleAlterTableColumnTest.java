/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.edit.OracleTableColumnManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.StandardConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OracleAlterTableColumnTest {

    private static final Log log = Log.getLog(OracleAlterTableColumnTest.class);

    @Mock
    private DBRProgressMonitor monitor;

    private OracleDataSource testDataSource;
    private OracleSchema testSchema;
    private OracleExecutionContext executionContext;
    private DBEObjectMaker<OracleTableColumn, OracleTableBase> objectMaker;

    @Mock
    private DBPDataSourceContainer mockDataSourceContainer;
    @Mock
    private JDBCRemoteInstance mockRemoteInstance;

    private final String lineBreak = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);

    @Before
    public void setUp() {
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("oracle"));
        Mockito.when(mockDataSourceContainer.getPlatform()).thenReturn(DBWorkbench.getPlatform());

        testDataSource = new OracleDataSource(mockDataSourceContainer);

        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(testDataSource);

        executionContext = new OracleExecutionContext(mockRemoteInstance, "Test");

        testSchema = new OracleSchema(testDataSource, -1, "TEST_SCHEMA");

        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(DBWorkbench.getPlatform().getPreferenceStore());

        objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(OracleTableColumn.class, DBEObjectMaker.class);
    }

    @Test
    public void generateAlterColumnStatementAddOneComment() {
        OracleTableBase oracleTableBase = new OracleTable(testSchema, "TEST_TABLE");

        OracleTableColumn column1 = addColumn(oracleTableBase, "COLUMN1", "NUMERIC", 1);
        column1.setComment("Test comment");
        List<DBEPersistAction> actions = new ArrayList<>();

        OracleTableColumnManager.addColumnCommentAction(monitor, actions, column1);

        String expectedDDL =
                "COMMENT ON COLUMN TEST_SCHEMA.TEST_TABLE.COLUMN1 IS 'Test comment'";

        Assert.assertEquals(actions.get(0).getScript(), expectedDDL);
    }

    @Test
    public void generateAlterTableDropColumnStatement() throws Exception {
        OracleTableBase oracleTableBase = new OracleTable(testSchema, "TEST_TABLE");

        OracleTableColumn column1 = addColumn(oracleTableBase, "COLUMN1", "VARCHAR", 1);

        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.deleteObject(commandContext, column1, Collections.emptyMap());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE DROP COLUMN COLUMN1;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateAlterTableAddColumnStatement() throws Exception {
        OracleTableBase oracleTableBase = new OracleTable(testSchema, "TEST_TABLE");

        addColumn(oracleTableBase, "COLUMN1", "VARCHAR", 1);

        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.createNewObject(monitor, commandContext, oracleTableBase, null, Collections.emptyMap());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, commandContext, executionContext, Collections.emptyMap(), null);
        String script = SQLUtils.generateScript(testDataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "ALTER TABLE TEST_SCHEMA.TEST_TABLE ADD Column2 INTEGER;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    private OracleTableColumn addColumn(OracleTableBase table, String columnName, String columnType, int ordinalPosition) {
        OracleTableColumn column = new OracleTableColumn(table);
        column.setName(columnName);
        column.setTypeName(columnType);
        column.setOrdinalPosition(ordinalPosition);
        List<OracleTableColumn> cachedAttributes = (List<OracleTableColumn>) table.getCachedAttributes();
        cachedAttributes.add(column);
        return column;
    }
}