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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

public class DatabaseTransferConsumerTargetTest extends DBeaverUnitTest {
    @Test
    public void newTableRetainsTargetSchemaAndRequestedName() {
        DBSObjectContainer schema = Mockito.mock(DBSObjectContainer.class);
        DatabaseTransferConsumer consumer = new DatabaseTransferConsumer(schema, "album_copy");
        Assertions.assertSame(schema, consumer.getTargetObjectContainer());
        Assertions.assertSame(schema, consumer.getDatabaseObject());
        Assertions.assertEquals("album_copy", consumer.getTargetTableName());
        Assertions.assertNull(consumer.getTargetObject());
    }

    @Test
    public void newTableKeepsQuotedMappingName() {
        DBSObjectContainer schema = Mockito.mock(DBSObjectContainer.class);
        DatabaseTransferConsumer consumer = new DatabaseTransferConsumer(schema, "\"AlbumCopy\"");
        Assertions.assertEquals("\"AlbumCopy\"", consumer.getTargetTableName());
    }

    @Test
    public void newTableConnectionIsAvailableBeforeSettingsAreLoaded() {
        DBSObjectContainer schema = Mockito.mock(DBSObjectContainer.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBPDataSourceContainer connection = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(schema.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(connection);
        Mockito.when(connection.isConnected()).thenReturn(true);
        DataTransferSettings transfer = Mockito.mock(DataTransferSettings.class);
        Mockito.when(transfer.getDataPipes()).thenReturn(List.of(
            new DataTransferPipe(null, new DatabaseTransferConsumer(schema, "album"))));

        Assertions.assertSame(connection, DatabaseConsumerSettings.getDataSourceContainer(transfer));
        Assertions.assertTrue(DatabaseConsumerSettings.getDataSourceContainer(transfer).isConnected());
        Mockito.verify(transfer, Mockito.never()).getNodeSettingsMap(Mockito.any());
    }

    @Test
    public void existingTableConnectionTakesPrecedenceOverSavedConnection() {
        DBSDataManipulator table = Mockito.mock(DBSDataManipulator.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBPDataSourceContainer connection = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(table.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(connection);
        DataTransferSettings transfer = Mockito.mock(DataTransferSettings.class);
        Mockito.when(transfer.getDataPipes()).thenReturn(List.of(
            new DataTransferPipe(null, new DatabaseTransferConsumer(table))));
        Mockito.when(transfer.getNodeSettingsMap(Mockito.any())).thenReturn(Map.of("entityId", "previous-connection/table"));

        Assertions.assertSame(connection, DatabaseConsumerSettings.getDataSourceContainer(transfer));
        Mockito.verify(transfer, Mockito.never()).getNodeSettingsMap(Mockito.any());
    }

    @Test
    public void explicitTargetDoesNotRestorePreviousExportsMapping() {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSDataContainer source = Mockito.mock(DBSDataContainer.class);
        DBRRunnableContext runnableContext = Mockito.mock(DBRRunnableContext.class);
        DataTransferSettings transferSettings = Mockito.mock(DataTransferSettings.class);
        Mockito.when(transferSettings.getDataPipes()).thenReturn(List.of());
        Map<String, Object> previousMapping = Map.of("targetName", "old_table");
        settings.loadSettings(runnableContext, transferSettings, Map.of("mappings", Map.of("source", previousMapping)));
        DatabaseMappingContainer mapping = Mockito.mock(DatabaseMappingContainer.class);
        try (var dbUtils = Mockito.mockStatic(DBUtils.class)) {
            dbUtils.when(() -> DBUtils.getObjectFullId(source)).thenReturn("source");
            settings.addDataMappings(runnableContext, source, mapping, false);
            Mockito.verify(mapping, Mockito.never()).loadSettings(Mockito.any(), Mockito.anyMap());
            Assertions.assertSame(mapping, settings.getDataMapping(source));
            settings.addDataMappings(runnableContext, source, mapping);
            Mockito.verify(mapping).loadSettings(runnableContext, previousMapping);
        }
    }

    @Test
    public void existingTargetStillUsesTheTable() {
        DBSDataManipulator table = Mockito.mock(DBSDataManipulator.class);
        DatabaseTransferConsumer consumer = new DatabaseTransferConsumer(table);
        Assertions.assertSame(table, consumer.getDatabaseObject());
        Assertions.assertSame(table, consumer.getTargetObject());
        Assertions.assertNull(consumer.getTargetTableName());
    }
}
