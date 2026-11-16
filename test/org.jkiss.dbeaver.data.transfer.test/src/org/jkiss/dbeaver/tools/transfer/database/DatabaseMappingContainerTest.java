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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

public class DatabaseMappingContainerTest {

    @Test
    public void savedViewTargetIsNotResolved() throws Exception {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSObjectContainer container = Mockito.mock(DBSObjectContainer.class);
        DBSView view = Mockito.mock(DBSView.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSDataContainer source = Mockito.mock(DBSDataContainer.class);
        DBRRunnableContext context = Mockito.mock(DBRRunnableContext.class);

        settings.setContainer(container);
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("my_view"))).thenReturn(view);

        DatabaseMappingContainer mapping = Mockito.spy(new DatabaseMappingContainer(settings, source));
        Mockito.doReturn(Collections.emptyList()).when(mapping).getAttributeMappings(Mockito.any());

        mapping.loadSettings(context, Map.of("targetName", "my_view", "mappingType", "existing"));

        Mockito.verify(container).getChild(Mockito.any(), Mockito.eq("my_view"));
        Assertions.assertNull(mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.create, mapping.getMappingType());
        Assertions.assertEquals("my_view", mapping.getTargetName());
    }

    @Test
    public void loadingInvalidSavedTargetClearsPreviousTarget() throws Exception {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSObjectContainer container = Mockito.mock(DBSObjectContainer.class);
        DBSView view = Mockito.mock(DBSView.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSTable table = Mockito.mock(DBSTable.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSDataContainer source = Mockito.mock(DBSDataContainer.class);
        DBRRunnableContext context = Mockito.mock(DBRRunnableContext.class);

        settings.setContainer(container);
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("my_view"))).thenReturn(view);

        DatabaseMappingContainer mapping = Mockito.spy(new DatabaseMappingContainer(settings, source));
        Mockito.doReturn(Collections.emptyList()).when(mapping).getAttributeMappings(Mockito.any());
        mapping.setTarget((DBSDataManipulator) table);

        mapping.loadSettings(context, Map.of("targetName", "my_view", "mappingType", "existing"));

        Assertions.assertNull(mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.create, mapping.getMappingType());
    }

    @Test
    public void refreshDoesNotBindViewAsTarget() throws Exception {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSObjectContainer container = Mockito.mock(DBSObjectContainer.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        DBSView view = Mockito.mock(DBSView.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DatabaseMappingContainer mapping = Mockito.mock(DatabaseMappingContainer.class);

        settings.setContainer(container);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.getUnquotedIdentifier("my_view")).thenReturn("my_view");
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("my_view"))).thenReturn(view);
        Mockito.when(mapping.getMappingType()).thenReturn(DatabaseMappingType.existing);
        Mockito.when(mapping.getTargetName()).thenReturn("my_view");
        Mockito.when(mapping.getAttributeMappings(Mockito.any())).thenReturn(Collections.emptyList());

        Assertions.assertThrows(
            DBCException.class,
            () -> DatabaseTransferUtils.refreshDatabaseMappings(
                Mockito.mock(DBRProgressMonitor.class),
                settings,
                mapping,
                true));
    }

    @Test
    public void refreshBindsValidTableAsTarget() throws Exception {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSObjectContainer container = Mockito.mock(DBSObjectContainer.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        DBSTable table = Mockito.mock(DBSTable.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DatabaseMappingContainer mapping = Mockito.mock(DatabaseMappingContainer.class);

        settings.setContainer(container);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.getUnquotedIdentifier("my_table")).thenReturn("my_table");
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("my_table"))).thenReturn(table);
        Mockito.when(table.getEntityType()).thenReturn(DBSEntityType.TABLE);
        Mockito.when(mapping.getMappingType()).thenReturn(DatabaseMappingType.existing);
        Mockito.when(mapping.getTargetName()).thenReturn("my_table");
        Mockito.when(mapping.getAttributeMappings(Mockito.any())).thenReturn(Collections.emptyList());

        Assertions.assertDoesNotThrow(
            () -> DatabaseTransferUtils.refreshDatabaseMappings(
                Mockito.mock(DBRProgressMonitor.class),
                settings,
                mapping,
                true));
        Mockito.verify(mapping).setTarget((DBSDataManipulator) table);
    }
}
