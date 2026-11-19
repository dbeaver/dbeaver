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

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseMappingContainerTest {

    @Test
    public void quotedSourceNameRequiresCasePreservation() {
        DBSDataContainer source = mockSource("version_A22", mockQuotingDataSource("version_A22", true));

        DatabaseMappingContainer container = new DatabaseMappingContainer(
            Mockito.mock(DatabaseConsumerSettings.class),
            source
        );

        Assertions.assertTrue(container.isSourceNameQuoted());
    }

    @Test
    public void unquotedSourceNameDoesNotRequireCasePreservation() {
        DBSDataContainer source = mockSource("version_a22", mockQuotingDataSource("version_a22", false));

        DatabaseMappingContainer container = new DatabaseMappingContainer(
            Mockito.mock(DatabaseConsumerSettings.class),
            source
        );

        Assertions.assertFalse(container.isSourceNameQuoted());
    }

    @Test
    public void quotedTargetNameIsResolvedWhenLoadingSavedMapping() throws Exception {
        DBSDataContainer source = mockSource("version_A22", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"version_A22\"")).thenReturn("version_A22");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("version_A22"))).thenReturn(target);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"version_A22\"",
            "mappingType", DatabaseMappingType.create.name()
        ));

        Assertions.assertSame(target, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.existing, mapping.getMappingType());
    }

    @Test
    public void rawQuotedTargetNameIsResolvedBeforeUnquoting() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"")).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator unquotedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(unquotedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"foo\"",
            "mappingType", DatabaseMappingType.existing.name()
        ));

        Assertions.assertSame(literalTarget, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.existing, mapping.getMappingType());
    }

    @Test
    public void refreshedCreateMappingResolvesRawQuotedTargetName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"")).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator unquotedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(unquotedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.setMappingType(DatabaseMappingType.create);
        mapping.setTargetName("\"foo\"");

        DatabaseTransferUtils.refreshDatabaseMappings(new VoidProgressMonitor(), settings, mapping, false);

        Assertions.assertSame(literalTarget, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.existing, mapping.getMappingType());
    }

    @Test
    public void nonManipulatorRawNameFallsBackToUnquotedTarget() throws Exception {
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"")).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSObject nonManipulator = Mockito.mock(DBSObject.class);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(nonManipulator);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(target);

        Assertions.assertSame(target, DatabaseTransferUtils.findChildByStoredName(
            targetContainer,
            new VoidProgressMonitor(),
            "\"foo\""));
    }

    @Test
    public void sourceCaseIsPreservedOnlyForUnchangedTargetNames() {
        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        DBSDataContainer source = mockSource("version_A22", mockQuotingDataSource("version_A22", true));

        DatabaseMappingContainer existing = new DatabaseMappingContainer(settings, source);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(target.getName()).thenReturn("version_A22");
        existing.setTarget(target);
        existing.setMappingType(DatabaseMappingType.existing);
        Assertions.assertTrue(existing.shouldPreserveSourceCase("version_A22"));
        Assertions.assertFalse(existing.shouldPreserveSourceCase("DESTINATION"));

        DatabaseMappingContainer create = new DatabaseMappingContainer(settings, source);
        create.setMappingType(DatabaseMappingType.create);
        create.setTargetName("\"version_A22\"");
        Assertions.assertTrue(create.shouldPreserveSourceCase("\"version_A22\""));
        Assertions.assertFalse(create.shouldPreserveSourceCase("DESTINATION"));

        DatabaseMappingContainer unquotedSource = new DatabaseMappingContainer(
            settings,
            mockSource("version_a22", mockQuotingDataSource("version_a22", false)));
        unquotedSource.setMappingType(DatabaseMappingType.create);
        unquotedSource.setTargetName("version_a22");
        Assertions.assertFalse(unquotedSource.shouldPreserveSourceCase("version_a22"));
    }

    @Test
    public void transformedNameKeepsCaseWhenCaseChangingIsSkipped() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBPDataSourceContainer dataSourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.getQuotedIdentifier("version_A22", true, false)).thenReturn("\"version_A22\"");

        Assertions.assertEquals(
            "\"version_A22\"",
            DatabaseTransferUtils.getTransformedName(dataSource, "version_A22", true)
        );
        Assertions.assertEquals(
            "\"version_A22\"",
            DatabaseTransferUtils.getTransformedTargetName(
                dataSource, "version_A22", DatabaseMappingType.create, true)
        );
        Assertions.assertEquals(
            "\"version_A22\"",
            DatabaseTransferUtils.getTransformedTargetName(
                dataSource, "version_A22", DatabaseMappingType.unspecified, true)
        );
    }

    @Test
    public void existingTargetNameIsNotTransformedForQuotedSource() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);

        Assertions.assertEquals(
            "version_A22",
            DatabaseTransferUtils.getTransformedTargetName(
                dataSource, "version_A22", DatabaseMappingType.existing, true)
        );
        Assertions.assertEquals(
            "version_A22",
            DatabaseTransferUtils.getTransformedTargetName(
                dataSource, "version_A22", DatabaseMappingType.recreate, true)
        );
    }

    @Test
    public void recreatedTableKeepsExistingTargetName() throws Exception {
        DBPDataSource dataSource = mockLowerCaseDataSource();
        DBSTable table = (DBSTable) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBPNamedObject2.class)
        );
        Mockito.when(table.getDataSource()).thenReturn(dataSource);

        Assertions.assertEquals(
            "version_A22",
            DatabaseTransferUtils.getTableFinalName("version_A22", DBSTable.class, table, false)
        );
        Mockito.verify((DBPNamedObject2) table).setName("version_A22");
    }

    @Test
    public void quotedTargetNameIsPersistedAndReturnedAsIs() {
        DBSDataContainer source = mockSource("version_A22", Mockito.mock(DBPDataSource.class));
        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.setTargetName("\"version_A22\"");

        Assertions.assertEquals("\"version_A22\"", mapping.getTargetName());
        Map<String, Object> saved = new LinkedHashMap<>();
        mapping.saveSettings(saved);
        Assertions.assertEquals("\"version_A22\"", saved.get("targetName"));
    }

    @Test
    public void newTableTargetNameIsQuotedOnlyOnce() throws Exception {
        assertNewTableTargetName("\"version_A22\"", "\"version_A22\"");
        assertNewTableTargetName("my table", "\"my table\"");
    }

    @Test
    public void autoAndDisplayTargetNamesMatchForQuotedSource() {
        DBPDataSource dataSource = mockBasicDataSource();

        String displayName = DatabaseTransferUtils.getTransformedTargetName(
            dataSource, "\"version_A22\"", DatabaseMappingType.unspecified, false);
        String autoName = DatabaseTransferUtils.getTransformedTargetName(
            dataSource, "version_A22", DatabaseMappingType.unspecified, true);

        Assertions.assertEquals("\"version_A22\"", displayName);
        Assertions.assertEquals(autoName, displayName);
    }

    private static void assertNewTableTargetName(String targetName, String expectedName) throws Exception {
        DBPDataSource dataSource = mockBasicDataSource();
        DBSTable table = (DBSTable) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBPNamedObject2.class)
        );
        Mockito.when(table.getDataSource()).thenReturn(dataSource);

        Assertions.assertEquals(
            expectedName,
            DatabaseTransferUtils.getTableFinalName(targetName, DBSTable.class, table, true)
        );
        Mockito.verify((DBPNamedObject2) table).setName(expectedName);
        // Stored names are already quoted, so the DDL formatter must not quote them again.
        Assertions.assertEquals(expectedName, DBUtils.getQuotedIdentifier(dataSource, expectedName));
        Assertions.assertEquals(
            "\"schema\"." + expectedName,
            DBUtils.getFullyQualifiedName(dataSource, "schema", expectedName)
        );
        // refreshDatabaseMappings and loadSettings unquote the persisted name before lookup.
        Assertions.assertEquals(
            DBUtils.getUnQuotedIdentifier(dataSource, targetName),
            DBUtils.getUnQuotedIdentifier(dataSource, expectedName)
        );
    }

    private static DBSDataContainer mockSource(String name, DBPDataSource dataSource) {
        DBSDataContainer source = (DBSDataContainer) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBSDataContainer.class)
        );
        Mockito.when(source.getName()).thenReturn(name);
        Mockito.when(source.getDataSource()).thenReturn(dataSource);
        return source;
    }

    private static DBPDataSource mockQuotingDataSource(String name, boolean mustBeQuoted) {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.mustBeQuoted(name, true)).thenReturn(mustBeQuoted);
        return dataSource;
    }

    private static DBPDataSource mockLowerCaseDataSource() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBPDataSourceContainer dataSourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(preferences.getBoolean(ModelPreferences.META_CASE_SENSITIVE)).thenReturn(false);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.storesUnquotedCase()).thenReturn(DBPIdentifierCase.LOWER);
        Mockito.when(dialect.supportsUnquotedMixedCase()).thenReturn(false);
        Mockito.when(dialect.getQuotedIdentifier("version_a22", true, false)).thenReturn("version_a22");
        return dataSource;
    }

    private static DBPDataSource mockBasicDataSource() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        DBPDataSourceContainer dataSourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        Mockito.when(dataSourceContainer.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(BasicSQLDialect.INSTANCE);
        return dataSource;
    }
}
