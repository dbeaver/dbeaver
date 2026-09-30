/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.timeplus;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSourceObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.timeplus.edit.TimeplusTableColumnManager;
import org.jkiss.dbeaver.ext.timeplus.edit.TimeplusTableManager;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusDataSource;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusMetaModel;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusTable;
import org.jkiss.dbeaver.ext.timeplus.model.TimeplusTableColumn;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class TimeplusStreamDDLTest extends DBeaverUnitTest {

    private TimeplusDataSource dataSource;
    private GenericDataSourceObjectContainer structContainer;
    private GenericExecutionContext executionContext;

    private JDBCRemoteInstance mockRemoteInstance;

    @BeforeEach
    public void setUp() throws DBException {
        monitor = Mockito.mock(DBRProgressMonitor.class);
        mockRemoteInstance = Mockito.mock(JDBCRemoteInstance.class);
        DBPDataSourceContainer container = configureTestContainer("timeplus_proton");
        dataSource = new TimeplusDataSource(monitor, new TimeplusMetaModel(), container);
        structContainer = new GenericDataSourceObjectContainer(dataSource);
        Mockito.when(mockRemoteInstance.getDataSource()).thenReturn(dataSource);
        executionContext = new GenericExecutionContext(mockRemoteInstance, "Test");
    }

    @Test
    public void generateCreateStreamStatement() throws Exception {
        Assertions.assertEquals(TimeplusTable.class, dataSource.getPrimaryChildType(monitor));
        Assertions.assertInstanceOf(TimeplusTableManager.class, getManagerForClass(TimeplusTable.class));
        Assertions.assertInstanceOf(TimeplusTableColumnManager.class, getManagerForClass(TimeplusTableColumn.class));

        TestCommandContext commandContext = new TestCommandContext(executionContext, false);
        DBEObjectMaker<GenericTableBase, GenericStructContainer> tableManager = getManagerForClass(TimeplusTable.class);
        GenericTableBase stream = tableManager.createNewObject(
            monitor,
            commandContext,
            structContainer,
            null,
            Collections.emptyMap()
        );
        stream.setDescription("Stream comment");

        DBEObjectMaker<GenericTableColumn, GenericTableBase> columnManager =
            getManagerForClass(TimeplusTableColumn.class);
        GenericTableColumn idColumn = columnManager.createNewObject(
            monitor,
            commandContext,
            stream,
            null,
            Collections.emptyMap()
        );
        idColumn.setName("event_id");
        idColumn.setTypeName("int64");

        GenericTableColumn nameColumn = columnManager.createNewObject(
            monitor,
            commandContext,
            stream,
            null,
            Collections.emptyMap()
        );
        nameColumn.setName("event_name");
        nameColumn.setTypeName("string");
        nameColumn.setDescription("Column comment");

        String script = generateScript(commandContext);
        String expected = "CREATE STREAM NewTable (" + lineBreak
            + "\tevent_id int64," + lineBreak
            + "\tevent_name string COMMENT 'Column comment'" + lineBreak
            + ") COMMENT 'Stream comment';" + lineBreak;
        Assertions.assertEquals(expected, script);
    }

    @Test
    public void generateAlterStreamAddColumnStatement() throws Exception {
        TimeplusTable stream = new TimeplusTable(structContainer, "users", "Stream", null);
        stream.setPersisted(true);

        TestCommandContext commandContext = new TestCommandContext(executionContext, false);
        DBEObjectMaker<GenericTableColumn, GenericTableBase> columnManager =
            getManagerForClass(TimeplusTableColumn.class);
        GenericTableColumn column = columnManager.createNewObject(
            monitor,
            commandContext,
            stream,
            null,
            Collections.emptyMap()
        );
        column.setName("AlbumId");
        column.setTypeName("int8");
        column.setDescription("Imported column");

        String expected = "ALTER STREAM users ADD COLUMN AlbumId int8 COMMENT 'Imported column';" + lineBreak;
        Assertions.assertEquals(expected, generateScript(commandContext));
    }

    @Test
    public void generateDropStreamStatement() throws Exception {
        TimeplusTable stream = new TimeplusTable(structContainer, "users", "Stream", null);
        stream.setPersisted(true);

        TestCommandContext commandContext = new TestCommandContext(executionContext, false);
        DBEObjectMaker<GenericTableBase, GenericStructContainer> tableManager = getManagerForClass(TimeplusTable.class);
        tableManager.deleteObject(commandContext, stream, Collections.emptyMap());

        String expected = "DROP STREAM users;" + lineBreak;
        Assertions.assertEquals(expected, generateScript(commandContext));
    }

    @SuppressWarnings("unchecked")
    private <OBJECT_TYPE extends DBSObject, CONTAINER_TYPE>
    DBEObjectMaker<OBJECT_TYPE, CONTAINER_TYPE> getManagerForClass(
        Class<?> objectClass
    ) {
        return DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(objectClass, DBEObjectMaker.class);
    }

    private String generateScript(TestCommandContext commandContext) throws DBException {
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(
            monitor,
            commandContext,
            executionContext,
            Collections.emptyMap(),
            null
        );
        return SQLUtils.generateScript(dataSource, actions.toArray(DBEPersistAction[]::new), false);
    }
}
