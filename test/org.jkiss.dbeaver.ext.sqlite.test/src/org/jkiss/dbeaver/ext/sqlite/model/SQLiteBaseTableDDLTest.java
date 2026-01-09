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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class SQLiteBaseTableDDLTest extends DBeaverUnitTest {

    @Mock
    private JDBCRemoteInstance mockRemoteInstance;

    private GenericDataSource dataSource;
    private GenericExecutionContext executionContext;
    private GenericDataSourceObjectContainer container;
    private DBEObjectMaker<GenericTableBase, GenericStructContainer> objectMaker;
    private SQLiteTable table;

    @Before
    public void setUp() throws DBException {
        DBPDataSourceContainer mockDataSourceContainer = configureTestContainer("sqlite_jdbc");
        dataSource = new GenericDataSource(monitor, new SQLiteMetaModel(), mockDataSourceContainer, new SQLiteSQLDialect());
        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(dataSource);
        executionContext = new GenericExecutionContext(mockRemoteInstance, "Test");
        container = new GenericDataSourceObjectContainer(dataSource);
        table = new SQLiteTable(
            container,
            "Table_SQLite",
            "TABLE",
            null);

        objectMaker = getManagerForClass(SQLiteTable.class);
    }

    private DBEObjectMaker getManagerForClass(Class<?> objectClass) {
        return DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(objectClass, DBEObjectMaker.class);
    }

    @Test
    public void generateCreateNewTableWithTwoColumnsStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            container,
            null,
            Collections.emptyMap());
        DBEObjectMaker objectManager = getManagerForClass(SQLiteTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE NewTable (" + lineBreak +
            "\tColumn1 INTEGER," + lineBreak +
            "\tColumn2 INTEGER" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateNewTableWithTwoColumnsThenDropColumn() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            container,
            null,
            Collections.emptyMap());
        DBEObjectMaker<SQLiteTableColumn, SQLiteTable> objectManager = getManagerForClass(SQLiteTableColumn.class);
        SQLiteTableColumn column1 = objectManager.createNewObject(
            monitor,
                commandContext,
                table,
                null,
                Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        objectManager.deleteObject(commandContext, column1, Collections.emptyMap());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE NewTable (" + lineBreak +
                "\tColumn2 INTEGER" + lineBreak +
                ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }
    
    @Test
    public void generateCreateNewTableWithTwoColumnsOneNotNullOneNullableStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            container,
            null,
            Collections.emptyMap());
        DBEObjectMaker<SQLiteTableColumn, SQLiteTable> objectManager = getManagerForClass(SQLiteTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        final SQLiteTableColumn newColumn =
            objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        newColumn.setRequired(true);
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(
            dataSource,
            actions.toArray(new DBEPersistAction[0]),
            false);

        String expectedDDL = "CREATE TABLE NewTable (" + lineBreak +
            "\tColumn1 INTEGER," + lineBreak +
            "\tColumn2 INTEGER NOT NULL" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateNewTableWithTwoColumnsAndPrimaryKeyStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            container,
            null,
            Collections.emptyMap());
        DBEObjectMaker<SQLiteTableColumn, SQLiteTable> objectManager = getManagerForClass(SQLiteTableColumn.class);
        SQLiteTableColumn column1 = objectManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        column1.setRequired(true);
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        DBEObjectMaker<GenericUniqueKey, SQLiteTable> constraintManager = getManagerForClass(GenericUniqueKey.class);
        GenericUniqueKey constraint = constraintManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        constraint.setName("NewTable_PK");
        constraint.setConstraintType(DBSEntityConstraintType.PRIMARY_KEY);
        GenericTableConstraintColumn constraintColumn = new GenericTableConstraintColumn(constraint, column1, 1);
        constraint.setAttributeReferences(Collections.singletonList(constraintColumn));

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE NewTable (" + lineBreak +
            "\tColumn1 INTEGER NOT NULL," + lineBreak +
            "\tColumn2 INTEGER," + lineBreak +
            "\tCONSTRAINT NewTable_PK PRIMARY KEY (Column1)" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateNewTableWithTwoColumnsWithDefaultValuesStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            container,
            null,
            Collections.emptyMap());
        DBEObjectMaker<SQLiteTableColumn, SQLiteTable> objectManager = getManagerForClass(SQLiteTableColumn.class);
        SQLiteTableColumn column1 = objectManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        column1.setDefaultValue("'Default Value'");
        column1.setFullTypeName("TEXT");
        SQLiteTableColumn column2 = objectManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        column2.setDefaultValue("42");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE NewTable (" + lineBreak +
            "\tColumn1 TEXT DEFAULT ('Default Value')," + lineBreak +
            "\tColumn2 INTEGER DEFAULT (42)" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateStatementCreateNewTableWithTwoColumnsOneAutoIncrementAllQuoted() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            container,
            null,
            Collections.emptyMap());
        table.setName("Table_SQLite_&#@*_bad_symbols");
        DBEObjectMaker<SQLiteTableColumn, SQLiteTable> objectManager = getManagerForClass(SQLiteTableColumn.class);
        SQLiteTableColumn newColumn = objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        newColumn.setAutoIncrement(true);
        newColumn.setName("Column1_?>|(!_bas_symbols");
        SQLiteTableColumn newColumn2 = objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        newColumn2.setRequired(true);
        newColumn2.setName("Column2_#$%^_bas_symbols");
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(
            dataSource,
            actions.toArray(new DBEPersistAction[0]),
            false);

        String expectedDDL = "CREATE TABLE \"Table_SQLite_&#@*_bad_symbols\" (" + lineBreak +
            "\t\"Column1_?>|(!_bas_symbols\" INTEGER PRIMARY KEY AUTOINCREMENT," + lineBreak +
            "\t\"Column2_#$%^_bas_symbols\" INTEGER NOT NULL" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateDropTableStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.deleteObject(commandContext, table, Collections.emptyMap());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "DROP TABLE Table_SQLite;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }
}
