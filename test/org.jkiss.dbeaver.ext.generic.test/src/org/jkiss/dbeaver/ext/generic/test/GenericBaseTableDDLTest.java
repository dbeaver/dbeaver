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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class GenericBaseTableDDLTest extends DBeaverUnitTest {

    private GenericDataSource dataSource;
    private GenericExecutionContext executionContext;
    private DBEObjectMaker<GenericTableBase, GenericStructContainer> objectMaker;
    private GenericSchema genericSchema;
    private GenericTable genericTable;

    @Mock
    private JDBCRemoteInstance mockRemoteInstance;

    @Before
    public void setUp() throws DBException {
        // We do not have generic driver, so use SQLite one.
        DBPDataSourceContainer mockDataSourceContainer = configureTestContainer("sqlite_jdbc");
        dataSource = new GenericDataSource(monitor, new GenericMetaModel(), mockDataSourceContainer, new GenericSQLDialect());
        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(dataSource);
        executionContext = new GenericExecutionContext(mockRemoteInstance, "Test");
        GenericCatalog catalog = new GenericCatalog(dataSource, "CATALOG_GENERIC");
        genericSchema = new GenericSchema(dataSource, catalog, "SCHEMA_GENERIC");
        genericTable = new GenericTable(
            genericSchema,
            "TABLE_GENERIC",
            "CATALOG_GENERIC",
            "SCHEMA_GENERIC");

        objectMaker = getManagerForClass(GenericTable.class);
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
            genericSchema, 
            null, 
            Collections.emptyMap());
        DBEObjectMaker objectManager = getManagerForClass(GenericTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext, 
            executionContext, 
            Collections.emptyMap(), 
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE CATALOG_GENERIC.SCHEMA_GENERIC.NewTable (" + lineBreak +
            "\tColumn1 INTEGER," + lineBreak +
            "\tColumn2 INTEGER" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateNewTableWithTwoColumnsOneNullableStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            genericSchema,
            null,
            Collections.emptyMap());
        DBEObjectMaker<GenericTableColumn, GenericTableBase> objectManager = getManagerForClass(GenericTableColumn.class);
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        final DBSObject newColumn =
            objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        if (newColumn instanceof GenericTableColumn) {
            ((GenericTableColumn) newColumn).setRequired(true);
        }
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

        String expectedDDL = "CREATE TABLE CATALOG_GENERIC.SCHEMA_GENERIC.NewTable (" + lineBreak +
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
            genericSchema,
            null,
            Collections.emptyMap());
        DBEObjectMaker<GenericTableColumn, GenericTableBase> objectManager = getManagerForClass(GenericTableColumn.class);
        GenericTableColumn column1 = objectManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        objectManager.createNewObject(monitor, commandContext, table, null, Collections.emptyMap());
        DBEObjectMaker<GenericUniqueKey, GenericTableBase> constraintManager = getManagerForClass(GenericUniqueKey.class);
        GenericUniqueKey constraint = constraintManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        constraint.setName("NEWTABLE_PK");
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

        String expectedDDL = "CREATE TABLE CATALOG_GENERIC.SCHEMA_GENERIC.NewTable (" + lineBreak +
            "\tColumn1 INTEGER," + lineBreak +
            "\tColumn2 INTEGER," + lineBreak +
            "\tCONSTRAINT NEWTABLE_PK PRIMARY KEY (Column1)" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreateNewTableWithTwoColumnsWithCommentStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        GenericTableBase table = objectMaker.createNewObject(
            monitor,
            commandContext,
            genericSchema,
            null,
            Collections.emptyMap());
        DBEObjectMaker<GenericTableColumn, GenericTableBase> objectManager = getManagerForClass(GenericTableColumn.class);
        GenericTableColumn column1 = objectManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        column1.setDescription("Test comment 1");
        GenericTableColumn column2 = objectManager.createNewObject(
            monitor,
            commandContext,
            table,
            null,
            Collections.emptyMap());
        column2.setDescription("Test comment 2");

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "CREATE TABLE CATALOG_GENERIC.SCHEMA_GENERIC.NewTable (" + lineBreak +
            "\tColumn1 INTEGER," + lineBreak +
            "\tColumn2 INTEGER" + lineBreak +
            ");" + lineBreak +
            "COMMENT ON COLUMN CATALOG_GENERIC.SCHEMA_GENERIC.NewTable.Column1 IS 'Test comment 1';" + lineBreak +
            "COMMENT ON COLUMN CATALOG_GENERIC.SCHEMA_GENERIC.NewTable.Column2 IS 'Test comment 2';" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateDropTableStatement() throws Exception {
        TestCommandContext commandContext = new TestCommandContext(executionContext, false);

        objectMaker.deleteObject(commandContext, genericTable, Collections.emptyMap());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null);
        String script = SQLUtils.generateScript(dataSource, actions.toArray(new DBEPersistAction[0]), false);

        String expectedDDL = "DROP TABLE CATALOG_GENERIC.TABLE_GENERIC;" + lineBreak;

        Assert.assertEquals(script, expectedDDL);
    }

    @Test
    public void generateCreatePersistedTableWith3ColumnsDDL() throws Exception {
        GenericTableColumn tableColumn = addColumn(genericTable, "COLUMN1", "VARCHAR", 1);
        tableColumn.setMaxLength(100);
        GenericTableColumn tableColumn1 = addColumn(genericTable, "COLUMN2", "NUMBER", 2);
        tableColumn1.setPrecision(38);
        GenericTableColumn tableColumn2 = addColumn(genericTable, "COLUMN3", "CHAR", 3);
        tableColumn2.setMaxLength(13);

        String tableDDL = genericTable.getObjectDefinitionText(monitor, Collections.emptyMap());

        String expectedDDL = "-- Drop table" + lineBreak +
            lineBreak +
            "-- DROP TABLE CATALOG_GENERIC.TABLE_GENERIC;" + lineBreak +
            lineBreak +
            "CREATE TABLE CATALOG_GENERIC.TABLE_GENERIC (" + lineBreak +
            "\tCOLUMN1 VARCHAR(100)," + lineBreak +
            "\tCOLUMN2 NUMBER," + lineBreak +
            "\tCOLUMN3 CHAR(13)" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(tableDDL, expectedDDL);
    }

    @Test
    public void generateCreatePersistedTableWith2ColumnsDDL() throws Exception {
        GenericTable genericTable2 = new GenericTable(
            genericSchema,
            "TABLE_GENERIC2",
            "CATALOG_GENERIC",
            "SCHEMA_GENERIC");
        GenericTableColumn tableColumn = addColumn(genericTable2, "COLUMN1", "VARCHAR", 1);
        tableColumn.setMaxLength(42);
        GenericTableColumn tableColumn1 = addColumn(genericTable2, "COLUMN2", "BIGINT", 2);
        tableColumn1.setPrecision(4);

        String tableDDL = genericTable2.getObjectDefinitionText(monitor, Collections.emptyMap());

        String expectedDDL = "-- Drop table" + lineBreak +
            lineBreak +
            "-- DROP TABLE CATALOG_GENERIC.TABLE_GENERIC2;" + lineBreak +
            lineBreak +
            "CREATE TABLE CATALOG_GENERIC.TABLE_GENERIC2 (" + lineBreak +
            "\tCOLUMN1 VARCHAR(42)," + lineBreak +
            "\tCOLUMN2 BIGINT" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(tableDDL, expectedDDL);
    }

    @Test
    public void generateCreatePersistedTableWith3RequiredColumnsDDL() throws Exception {
        GenericTable genericTable3 = new GenericTable(
            genericSchema,
            "TABLE_GENERIC3",
            "CATALOG_GENERIC",
            "SCHEMA_GENERIC");
        GenericTableColumn tableColumn = addColumn(genericTable3, "COLUMN1", "DATE", 1);
        tableColumn.setRequired(true);
        GenericTableColumn tableColumn1 = addColumn(genericTable3, "COLUMN2", "BOOLEAN", 2);
        tableColumn1.setRequired(true);
        GenericTableColumn tableColumn2 = addColumn(genericTable3, "COLUMN3", "BLOB", 3);
        tableColumn2.setRequired(true);

        String tableDDL = genericTable3.getObjectDefinitionText(monitor, Collections.emptyMap());

        String expectedDDL = "-- Drop table" + lineBreak +
            lineBreak +
            "-- DROP TABLE CATALOG_GENERIC.TABLE_GENERIC3;" + lineBreak +
            lineBreak +
            "CREATE TABLE CATALOG_GENERIC.TABLE_GENERIC3 (" + lineBreak +
            "\tCOLUMN1 DATE NOT NULL," + lineBreak +
            "\tCOLUMN2 BOOLEAN NOT NULL," + lineBreak +
            "\tCOLUMN3 BLOB NOT NULL" + lineBreak +
            ");" + lineBreak;

        Assert.assertEquals(tableDDL, expectedDDL);
    }

    private GenericTableColumn addColumn(
        GenericTableBase table,
        String columnName,
        String columnType,
        int ordinalPosition
    ) throws DBException {
        GenericTableColumn column = new GenericTableColumn(table);
        column.setName(columnName);
        column.setTypeName(columnType);
        column.setOrdinalPosition(ordinalPosition);
        List<GenericTableColumn> cachedAttributes = (List<GenericTableColumn>) table.getCachedAttributes();
        cachedAttributes.add(column);
        return column;
    }
}
