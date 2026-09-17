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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.MonitorRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.DataTransferState;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseConsumerSettingsTest extends DBeaverUnitTest {
    private DBPProject project;
    private DBPDataSourceContainer dataSourceContainer;
    private DBSObjectContainer schema;
    private DBSTable target;
    private DBSEntityAttribute targetColumn;
    private StreamEntityMapping source;

    @BeforeEach
    public void setUp() throws DBException {
        project = Mockito.mock(DBPProject.class, Mockito.RETURNS_DEEP_STUBS);
        dataSourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class, Mockito.withSettings().extraInterfaces(DBSObjectContainer.class));
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(preferences.getBoolean(ModelPreferences.META_CASE_SENSITIVE)).thenReturn(true);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(dataSourceContainer.getId()).thenReturn("target-database");
        Mockito.when(dataSourceContainer.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        Mockito.when(project.getDataSourceRegistry().getDataSource("target-database")).thenReturn(dataSourceContainer);

        schema = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(schema.getName()).thenReturn("target_schema");
        Mockito.when(schema.getParentObject()).thenReturn(dataSource);
        Mockito.when(schema.getDataSource()).thenReturn(dataSource);
        Mockito.when(((DBSObjectContainer) dataSource).getChild(Mockito.any(), Mockito.eq("target_schema"))).thenReturn(schema);

        target = Mockito.mock(DBSTable.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        Mockito.when(target.getName()).thenReturn("target_table");
        Mockito.when(target.getParentObject()).thenReturn(schema);
        Mockito.when(target.getDataSource()).thenReturn(dataSource);
        Mockito.when(schema.getChild(Mockito.any(), Mockito.eq("target_table"))).thenReturn(target);
        targetColumn = Mockito.mock(DBSEntityAttribute.class);
        Mockito.when(targetColumn.getName()).thenReturn("renamed_column");
        Mockito.doReturn(List.of(targetColumn)).when(target).getAttributes(Mockito.any());

        source = new StreamEntityMapping(Path.of("source.csv"));
        source.getStreamColumns().add(new StreamDataImporterColumnInfo(source, 0, "source_column", "VARCHAR", 100, DBPDataKind.STRING));
        source.getStreamColumns().add(new StreamDataImporterColumnInfo(source, 1, "ignored_column", "VARCHAR", 100, DBPDataKind.STRING));
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, false",
        "false, true, false",
        "true, false, false",
        "true, true, false",
        "false, false, true"
    })
    public void restoreAndSaveImportMappings(boolean autoReconnect, boolean connected, boolean taskRunning) throws DBException {
        Mockito.when(dataSourceContainer.isConnected()).thenReturn(connected);
        Mockito.when(dataSourceContainer.connect(Mockito.any(), Mockito.eq(true), Mockito.eq(true))).thenAnswer(invocation -> {
            Mockito.when(dataSourceContainer.isConnected()).thenReturn(true);
            return true;
        });
        DBPPreferenceStore preferences = DTActivator.getDefault().getPreferences();
        boolean defaultReconnect = preferences.isDefault(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE);
        boolean previousReconnect = preferences.getBoolean(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE);
        try {
            preferences.setValue(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE, autoReconnect);
            Map<String, Object> configuration = getSavedConfiguration();

            DatabaseConsumerSettings settings = restoreMappings(configuration, taskRunning);
            Map<String, Object> saved = new LinkedHashMap<>();
            settings.saveSettings(saved);
            Assertions.assertEquals(configuration.get("entityId"), saved.get("entityId"));

            DatabaseConsumerSettings restored = restoreMappings(saved, false);
            Map<String, Object> savedAgain = new LinkedHashMap<>();
            restored.saveSettings(savedAgain);
            Assertions.assertEquals(saved, savedAgain);
            Mockito.verify(dataSourceContainer, Mockito.times(connected ? 0 : 1))
                .connect(Mockito.any(), Mockito.eq(true), Mockito.eq(true));
        } finally {
            if (defaultReconnect) {
                preferences.setToDefault(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE);
            } else {
                preferences.setValue(DTConstants.PREF_RECONNECT_TO_LAST_DATABASE, previousReconnect);
            }
        }
    }

    @Test
    public void switchingExportFormatsLoadsDeferredMappingsAndPreservesEdits() throws DBException {
        DataTransferRegistry registry = DataTransferRegistry.getInstance();
        var databaseConsumer = registry.getNodeByType(DatabaseTransferConsumer.class);
        var streamConsumer = registry.getNodeByType(StreamTransferConsumer.class);
        DatabaseTransferProducer producer = new DatabaseTransferProducer(source);
        DataTransferSettings transferSettings = new DataTransferSettings(
            List.of(producer),
            null,
            project,
            Map.of(
                DTConstants.PROP_CONSUMER_TYPE, databaseConsumer.getId(),
                DatabaseTransferConsumer.class.getSimpleName(), getSavedConfiguration(),
                StreamTransferConsumer.class.getSimpleName(), Map.of("outputFilePattern", "saved_pattern"),
                DatabaseTransferProducer.class.getSimpleName(), Map.of("fetchSize", 123)
            ),
            new DataTransferState(),
            true,
            true,
            false,
            false
        );
        Assertions.assertFalse(transferSettings.isNodeSettingsLoaded());

        transferSettings.selectConsumer(streamConsumer, null, true);
        transferSettings.loadNodeSettings(monitor);
        Assertions.assertTrue(transferSettings.isNodeSettingsLoaded());
        Mockito.verify(dataSourceContainer, Mockito.never()).connect(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        StreamConsumerSettings streamSettings = (StreamConsumerSettings) transferSettings.getNodeSettings(streamConsumer);
        Assertions.assertEquals("saved_pattern", streamSettings.getOutputFilePattern());
        streamSettings.setOutputFilePattern("edited_pattern");
        DatabaseProducerSettings producerSettings = (DatabaseProducerSettings) transferSettings.getNodeSettings(producer);
        Assertions.assertEquals(123, producerSettings.getFetchSize());
        producerSettings.setFetchSize(456);

        transferSettings.selectConsumer(databaseConsumer, null, true);
        Assertions.assertFalse(transferSettings.isNodeSettingsLoaded());
        transferSettings.loadNodeSettings(monitor);
        DatabaseConsumerSettings databaseSettings = (DatabaseConsumerSettings) transferSettings.getNodeSettings(databaseConsumer);
        assertMappings(databaseSettings, (DatabaseTransferConsumer) transferSettings.getDataPipes().getFirst().getConsumer());
        Assertions.assertEquals(456, producerSettings.getFetchSize());
        databaseSettings.setCommitAfterRows(321);

        transferSettings.selectConsumer(streamConsumer, null, true);
        transferSettings.loadNodeSettings(monitor);
        Assertions.assertEquals("edited_pattern", streamSettings.getOutputFilePattern());
        transferSettings.selectConsumer(databaseConsumer, null, true);
        transferSettings.loadNodeSettings(monitor);
        Assertions.assertEquals(321, databaseSettings.getCommitAfterRows());
        assertMappings(databaseSettings, (DatabaseTransferConsumer) transferSettings.getDataPipes().getFirst().getConsumer());
        Assertions.assertFalse(transferSettings.getState().hasErrors());
    }

    @NotNull
    private Map<String, Object> getSavedConfiguration() {
        return Map.of(
            "entityId", "target-database/target_schema",
            "mappings", Map.of(DBUtils.getObjectFullId(source), Map.of(
                "targetName", "target_table",
                "mappingType", "existing",
                "attributes", Map.of(
                    "source_column", Map.of("targetName", "renamed_column", "mappingType", "existing"),
                    "ignored_column", Map.of("mappingType", "skip")
                )
            ))
        );
    }

    @NotNull
    private DatabaseConsumerSettings restoreMappings(@NotNull Map<String, Object> configuration, boolean taskRunning) throws DBException {
        DatabaseTransferConsumer consumer = new DatabaseTransferConsumer();
        DataTransferSettings transferSettings = new DataTransferSettings(
            List.of(new StreamTransferProducer(source)),
            List.of(consumer),
            project,
            Map.of(DTConstants.PROP_PRODUCER_TYPE, StreamTransferProducer.NODE_ID),
            new DataTransferState(),
            true,
            false,
            true,
            taskRunning
        );
        DatabaseConsumerSettings settings = (DatabaseConsumerSettings) transferSettings.getNodeSettings(consumer);
        settings.loadSettings(new MonitorRunnableContext(monitor), transferSettings, configuration);
        transferSettings.getDataPipes().getFirst().initPipe(transferSettings, 0, 1);

        Assertions.assertFalse(transferSettings.getState().hasErrors());
        assertMappings(settings, consumer);
        return settings;
    }

    private void assertMappings(@NotNull DatabaseConsumerSettings settings, @NotNull DatabaseTransferConsumer consumer) {
        Assertions.assertSame(schema, settings.getContainer());
        Assertions.assertSame(target, consumer.getTargetObject());
        DatabaseMappingContainer mapping = settings.getDataMapping(source);
        Assertions.assertNotNull(mapping);
        Assertions.assertEquals(DatabaseMappingType.existing, mapping.getMappingType());
        DatabaseMappingAttribute columnMapping = mapping.getAttributeMapping(source.getStreamColumns().getFirst());
        Assertions.assertNotNull(columnMapping);
        Assertions.assertSame(targetColumn, columnMapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.existing, columnMapping.getMappingType());
        DatabaseMappingAttribute skippedColumn = mapping.getAttributeMapping(source.getStreamColumns().get(1));
        Assertions.assertNotNull(skippedColumn);
        Assertions.assertEquals(DatabaseMappingType.skip, skippedColumn.getMappingType());
    }
}
