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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PostgreDatabaseBackupSelectionTest extends DBeaverUnitTest {
    private final PostgreDatabase database = Mockito.mock(PostgreDatabase.class);
    private final PostgreSchema publicSchema = Mockito.mock(PostgreSchema.class);
    private final PostgreSchema otherSchema = Mockito.mock(PostgreSchema.class);
    private final PostgreTableBase firstTable = Mockito.mock(PostgreTableBase.class);
    private final PostgreTableBase secondTable = Mockito.mock(PostgreTableBase.class);
    private final PostgreDatabaseBackupSelection selection = new PostgreDatabaseBackupSelection();

    @TempDir
    Path temporaryDirectory;

    @Test
    void completeSchemaSelectionDoesNotRequireLoadingTables() throws IOException {
        selection.selectSchema(publicSchema, true);

        Assertions.assertTrue(selection.isCompleteBackup(List.of(publicSchema)));
        Assertions.assertTrue(selection.isTableSelected(publicSchema, firstTable));
        Assertions.assertTrue(selection.isTableSelected(publicSchema, secondTable));
        List<PostgreDatabaseBackupInfo> objects = selection.getExportObjects(database, true);
        Assertions.assertEquals(1, objects.size());
        Assertions.assertEquals(List.of(publicSchema), objects.getFirst().getSchemas());
        Assertions.assertNull(objects.getFirst().getTables());
        Mockito.verifyNoInteractions(publicSchema, firstTable, secondTable);
        List<String> command = getCommand(objects.getFirst(), true);
        Assertions.assertFalse(command.contains("-n"));
        Assertions.assertFalse(command.contains("-t"));
    }

    @Test
    void deselectingTableDisablesCompleteBackupForSingleSchema() throws IOException {
        selection.selectSchema(publicSchema, true);
        selection.selectTables(publicSchema, List.of(firstTable), false);

        Assertions.assertFalse(selection.isCompleteBackup(List.of(publicSchema)));
        Assertions.assertTrue(selection.isSchemaSelected(publicSchema));
        Assertions.assertFalse(selection.isTableSelected(publicSchema, secondTable));
        List<PostgreDatabaseBackupInfo> objects = selection.getExportObjects(database, false);
        Assertions.assertEquals(1, objects.size());
        Assertions.assertEquals(List.of(firstTable), objects.getFirst().getTables());
        Mockito.when(firstTable.getFullyQualifiedName(DBPEvaluationContext.DDL)).thenReturn("\"public\".\"first\"");
        List<String> command = getCommand(objects.getFirst(), false);
        Assertions.assertFalse(command.contains("-n"));
        Assertions.assertEquals(
            List.of("-t", PostgreNativeToolHandler.escapeCLIIdentifier("\"public\".\"first\"")),
            command.subList(command.indexOf("-t"), command.size())
        );
    }

    @Test
    void partialSelectionInAnySchemaDisablesCompleteBackup() {
        List<PostgreSchema> schemas = List.of(publicSchema, otherSchema);
        selection.selectSchemas(schemas, true);
        selection.selectTables(publicSchema, List.of(firstTable), false);

        Assertions.assertFalse(selection.isCompleteBackup(schemas));
        List<PostgreDatabaseBackupInfo> objects = selection.getExportObjects(database, false);
        Assertions.assertEquals(2, objects.size());
        Assertions.assertEquals(List.of(publicSchema), objects.get(0).getSchemas());
        Assertions.assertEquals(List.of(firstTable), objects.get(0).getTables());
        Assertions.assertEquals(List.of(otherSchema), objects.get(1).getSchemas());
        Assertions.assertNull(objects.get(1).getTables());
    }

    @Test
    void selectingAllSchemasClearsPartialSelectionsInEverySchema() {
        List<PostgreSchema> schemas = List.of(publicSchema, otherSchema);
        selection.selectTables(publicSchema, List.of(firstTable), false);
        selection.selectTables(otherSchema, List.of(secondTable), false);
        selection.selectSchemas(schemas, true);

        Assertions.assertTrue(selection.isCompleteBackup(schemas));
        List<PostgreDatabaseBackupInfo> objects = selection.getExportObjects(database, true);
        Assertions.assertEquals(1, objects.size());
        Assertions.assertEquals(schemas, objects.getFirst().getSchemas());
        Assertions.assertNull(objects.getFirst().getTables());
    }

    @Test
    void selectingNoSchemasClearsPreviouslySelectedTables() {
        List<PostgreSchema> schemas = List.of(publicSchema, otherSchema);
        selection.selectTables(publicSchema, List.of(firstTable), false);
        selection.selectSchema(otherSchema, true);
        selection.selectSchemas(schemas, false);

        Assertions.assertFalse(selection.isCompleteBackup(schemas));
        Assertions.assertFalse(selection.isTableSelected(publicSchema, firstTable));
        Assertions.assertFalse(selection.isTableSelected(otherSchema, secondTable));
        Assertions.assertTrue(selection.getExportObjects(database, false).isEmpty());
    }

    @Test
    void selectingAllTablesRestoresCompleteBackup() {
        selection.selectTables(publicSchema, List.of(firstTable), false);
        selection.selectTables(publicSchema, List.of(firstTable, secondTable), true);

        Assertions.assertTrue(selection.isCompleteBackup(List.of(publicSchema)));
        Assertions.assertNull(selection.getExportObjects(database, true).getFirst().getTables());
    }

    @Test
    void selectingNoTablesUnchecksTheirSchema() {
        selection.selectSchema(publicSchema, true);
        selection.selectTables(publicSchema, List.of(), false);

        Assertions.assertFalse(selection.isSchemaSelected(publicSchema));
        Assertions.assertFalse(selection.isCompleteBackup(List.of(publicSchema)));
        Assertions.assertTrue(selection.getExportObjects(database, false).isEmpty());
    }

    @Test
    void excludingSchemaKeepsSchemaFilter() throws IOException {
        List<PostgreSchema> schemas = List.of(publicSchema, otherSchema);
        selection.selectSchemas(schemas, true);
        selection.selectSchema(otherSchema, false);

        Assertions.assertFalse(selection.isCompleteBackup(schemas));
        List<PostgreDatabaseBackupInfo> objects = selection.getExportObjects(database, false);
        Assertions.assertEquals(1, objects.size());
        Assertions.assertEquals(List.of(publicSchema), objects.getFirst().getSchemas());
        Assertions.assertNull(objects.getFirst().getTables());
        PostgreDataSource dataSource = Mockito.mock(PostgreDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        Mockito.when(publicSchema.getDataSource()).thenReturn(dataSource);
        Mockito.when(publicSchema.getName()).thenReturn("public");
        List<String> command = getCommand(objects.getFirst(), false);
        Assertions.assertTrue(command.contains("-n"));
        Assertions.assertFalse(command.contains("-t"));
    }

    @Test
    void emptySelectionIsNotCompleteBackup() {
        Assertions.assertFalse(selection.isCompleteBackup(List.of()));
        Assertions.assertTrue(selection.getExportObjects(database, true).isEmpty());
    }

    @Test
    void partialSchemaDumpsHaveDistinctFileNames() {
        selection.selectTables(publicSchema, List.of(firstTable), false);
        selection.selectSchema(otherSchema, true);
        PostgreDatabaseBackupSettings settings = new PostgreDatabaseBackupSettings();
        settings.setExportObjects(selection.getExportObjects(database, false));
        settings.setOutputFolderPattern(temporaryDirectory.toString());
        settings.setOutputFilePattern("dump.sql");

        Assertions.assertEquals(
            temporaryDirectory.resolve("dump-1.sql"),
            Path.of(settings.getOutputFile(settings.getExportObjects().get(0)))
        );
        Assertions.assertEquals(
            temporaryDirectory.resolve("dump-2.sql"),
            Path.of(settings.getOutputFile(settings.getExportObjects().get(1)))
        );
    }

    @Test
    void partialSchemaDumpsHaveDistinctDirectoryNames() {
        selection.selectTables(publicSchema, List.of(firstTable), false);
        selection.selectSchema(otherSchema, true);
        PostgreDatabaseBackupSettings settings = new PostgreDatabaseBackupSettings();
        settings.setExportObjects(selection.getExportObjects(database, false));
        settings.setFormat(PostgreBackupRestoreSettings.ExportFormat.DIRECTORY);
        settings.setOutputFolderPattern(temporaryDirectory.toString());
        settings.setOutputFilePattern("dump");

        Assertions.assertEquals(
            temporaryDirectory.resolve("dump-1"),
            Path.of(settings.getOutputFile(settings.getExportObjects().get(0)))
        );
        Assertions.assertEquals(
            temporaryDirectory.resolve("dump-2"),
            Path.of(settings.getOutputFile(settings.getExportObjects().get(1)))
        );
    }

    @Test
    void completeBackupKeepsConfiguredFileName() {
        selection.selectSchemas(List.of(publicSchema, otherSchema), true);
        PostgreDatabaseBackupSettings settings = new PostgreDatabaseBackupSettings();
        settings.setExportObjects(selection.getExportObjects(database, true));
        settings.setOutputFolderPattern(temporaryDirectory.toString());
        settings.setOutputFilePattern("dump.sql");

        Assertions.assertEquals(
            temporaryDirectory.resolve("dump.sql"),
            Path.of(settings.getOutputFile(settings.getExportObjects().getFirst()))
        );
    }

    @NotNull
    private List<String> getCommand(@NotNull PostgreDatabaseBackupInfo info, boolean completeBackup) throws IOException {
        Files.createFile(temporaryDirectory.resolve(RuntimeUtils.getNativeBinaryName("pg_dump")));
        DBPNativeClientLocation clientHome = Mockito.mock(DBPNativeClientLocation.class);
        Mockito.when(clientHome.getPath()).thenReturn(temporaryDirectory.toFile());
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        PostgreDatabaseBackupSettings settings = Mockito.mock(PostgreDatabaseBackupSettings.class);
        Mockito.when(settings.getClientHome()).thenReturn(clientHome);
        Mockito.when(settings.getDataSourceContainer()).thenReturn(container);
        Mockito.when(settings.getFormat()).thenReturn(PostgreBackupRestoreSettings.ExportFormat.CUSTOM);
        Mockito.when(settings.getOutputFile(info)).thenReturn(temporaryDirectory.resolve("dump.backup").toString());
        Mockito.when(settings.getExportObjects()).thenReturn(List.of(info));
        Mockito.when(settings.isFullSchemaBackup()).thenReturn(completeBackup);
        List<String> command = new ArrayList<>();
        new PostgreDatabaseBackupHandler().fillProcessParameters(settings, info, command);
        return command;
    }
}
