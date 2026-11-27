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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseMappingContainerTest extends DBeaverUnitTest {

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
        Mockito.when(targetDialect.getUnquotedIdentifier("\"version_A22\"", true)).thenReturn("version_A22");

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
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

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
            "mappingType", DatabaseMappingType.existing.name(),
            "targetNameIsRaw", true
        ));

        Assertions.assertSame(literalTarget, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.existing, mapping.getMappingType());
    }

    @Test
    public void refreshedCreateMappingDecodesEncodedTargetName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

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

        Assertions.assertSame(unquotedTarget, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.existing, mapping.getMappingType());
    }

    @Test
    public void refreshedRecreateMappingResolvesRawQuotedTargetName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(literalTarget.getName()).thenReturn("\"foo\"");
        DBSDataManipulator unquotedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(unquotedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.setMappingType(DatabaseMappingType.recreate);
        mapping.setTarget(literalTarget);

        DatabaseTransferUtils.refreshDatabaseMappings(new VoidProgressMonitor(), settings, mapping, false);

        Assertions.assertSame(literalTarget, mapping.getTarget());
    }

    @Test
    public void encodedCreateTargetNameDoesNotBindLiteralQuotedTable() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"foo\"",
            "mappingType", DatabaseMappingType.create.name()
        ));

        Assertions.assertNull(mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.create, mapping.getMappingType());
    }

    @Test
    public void newRecreateTaskWithEncodedTargetNameResolvesDecodedTable() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator decodedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(decodedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"foo\"",
            "mappingType", DatabaseMappingType.recreate.name(),
            "targetNameIsRaw", false
        ));

        Assertions.assertSame(decodedTarget, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.recreate, mapping.getMappingType());
    }

    @Test
    public void newRecreateTaskWithEncodedTargetNameDoesNotBindLiteralTable() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"foo\"",
            "mappingType", DatabaseMappingType.recreate.name(),
            "targetNameIsRaw", false
        ));

        Assertions.assertNull(mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.recreate, mapping.getMappingType());
    }

    @Test
    public void recreateMappingWithoutTargetRoundTripsEncodedName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator decodedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(decodedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer savedMapping = new DatabaseMappingContainer(settings, source);
        savedMapping.setMappingType(DatabaseMappingType.recreate);
        savedMapping.setTargetName("\"foo\"");
        Map<String, Object> saved = new LinkedHashMap<>();
        savedMapping.saveSettings(saved);

        Assertions.assertEquals("\"foo\"", saved.get("targetName"));
        Assertions.assertEquals(false, saved.get("targetNameIsRaw"));

        DatabaseMappingContainer loaded = new DatabaseMappingContainer(settings, source);
        loaded.loadSettings(Mockito.mock(DBRRunnableContext.class), saved);

        Assertions.assertSame(decodedTarget, loaded.getTarget());
        Assertions.assertEquals(DatabaseMappingType.recreate, loaded.getMappingType());
    }

    @Test
    public void legacyExistingTaskWithoutProvenanceResolvesRawName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator decodedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(decodedTarget);

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
    public void legacyRecreateTaskWithoutProvenanceResolvesRawName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator decodedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(decodedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"foo\"",
            "mappingType", DatabaseMappingType.recreate.name()
        ));

        Assertions.assertSame(literalTarget, mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.recreate, mapping.getMappingType());
    }

    @Test
    public void refreshedRecreateMappingWithNullTargetDecodesEncodedName() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator literalTarget = Mockito.mock(DBSDataManipulator.class);
        DBSDataManipulator decodedTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(literalTarget);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(decodedTarget);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.setMappingType(DatabaseMappingType.recreate);
        mapping.setTargetName("\"foo\"");

        DatabaseTransferUtils.refreshDatabaseMappings(new VoidProgressMonitor(), settings, mapping, false);

        Assertions.assertSame(decodedTarget, mapping.getTarget());
    }

    @Test
    public void encodedTargetNameWithEmbeddedQuotesIsResolved() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"a\"\"b\"", true)).thenReturn("a\"b");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("a\"b"))).thenReturn(target);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"a\"\"b\"",
            "mappingType", DatabaseMappingType.create.name()
        ));

        Assertions.assertSame(target, mapping.getTarget());
    }

    @Test
    public void nonManipulatorRawNameFallsBackToUnquotedTarget() throws Exception {
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSObject nonManipulator = Mockito.mock(DBSObject.class);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(nonManipulator);
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(target);

        Assertions.assertSame(target, DatabaseTransferUtils.findChildByStoredName(
            targetContainer,
            new VoidProgressMonitor(),
            "\"foo\"",
            true));
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
            DatabaseTransferUtils.getTableFinalName("version_A22", DBSTable.class, table, false, false)
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
        Assertions.assertEquals(false, saved.get("targetNameIsRaw"));
    }

    @Test
    public void savedExistingTargetUsesResolvedRawName() throws Exception {
        DBSDataContainer source = mockSource("version_A22", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("\"version_A22\"", true)).thenReturn("version_A22");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);
        DBSDataManipulator target = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(target.getName()).thenReturn("version_A22");
        Mockito.when(targetContainer.getChild(Mockito.any(), Mockito.eq("version_A22"))).thenReturn(target);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "\"version_A22\"",
            "mappingType", DatabaseMappingType.existing.name(),
            "targetNameIsRaw", true
        ));

        Map<String, Object> saved = new LinkedHashMap<>();
        mapping.saveSettings(saved);

        Assertions.assertEquals("version_A22", saved.get("targetName"));
        Assertions.assertEquals(true, saved.get("targetNameIsRaw"));
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

    @Test
    public void recreateWithoutTargetPreservesRawNameCase() throws Exception {
        DBPDataSource dataSource = mockLowerCaseDataSource();
        SQLDialect dialect = dataSource.getSQLDialect();
        Mockito.when(dialect.getQuotedIdentifier("version_A22", true, false)).thenReturn("\"version_A22\"");
        DBSTable table = (DBSTable) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBPNamedObject2.class)
        );
        Mockito.when(table.getDataSource()).thenReturn(dataSource);

        Assertions.assertEquals(
            "\"version_A22\"",
            DatabaseTransferUtils.getTableFinalName("version_A22", DBSTable.class, table, true, true)
        );
        Mockito.verify((DBPNamedObject2) table).setName("\"version_A22\"");
    }

    @Test
    public void loadedRawNameKeepsProvenanceWhenTargetIsMissing() throws Exception {
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DBPDataSource targetDataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect targetDialect = Mockito.mock(SQLDialect.class);
        Mockito.when(targetDataSource.getSQLDialect()).thenReturn(targetDialect);
        Mockito.when(targetDialect.getUnquotedIdentifier("version_A22", true)).thenReturn("version_A22");

        DBSObjectContainer targetContainer = Mockito.mock(DBSObjectContainer.class);
        Mockito.when(targetContainer.getDataSource()).thenReturn(targetDataSource);

        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        Mockito.when(settings.getContainer()).thenReturn(targetContainer);

        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        mapping.loadSettings(Mockito.mock(DBRRunnableContext.class), Map.of(
            "targetName", "version_A22",
            "mappingType", DatabaseMappingType.recreate.name(),
            "targetNameIsRaw", true
        ));
        Assertions.assertNull(mapping.getTarget());
        Assertions.assertTrue(mapping.isTargetNameRaw());

        Map<String, Object> saved = new LinkedHashMap<>();
        mapping.saveSettings(saved);

        Assertions.assertEquals("version_A22", saved.get("targetName"));
        Assertions.assertEquals(true, saved.get("targetNameIsRaw"));
    }

    @Test
    public void clearedTargetDoesNotOverrideConflictNameOnSave() {
        DatabaseConsumerSettings settings = Mockito.mock(DatabaseConsumerSettings.class);
        DBSDataContainer source = mockSource("source", Mockito.mock(DBPDataSource.class));
        DatabaseMappingContainer mapping = new DatabaseMappingContainer(settings, source);
        DBSDataManipulator oldTarget = Mockito.mock(DBSDataManipulator.class);
        Mockito.when(oldTarget.getName()).thenReturn("old_target");
        mapping.setTarget(oldTarget);
        mapping.setMappingType(DatabaseMappingType.existing);
        mapping.setTarget(null);
        mapping.setTargetName("conflict_name");
        mapping.setMappingType(DatabaseMappingType.unspecified);

        Map<String, Object> saved = new LinkedHashMap<>();
        mapping.saveSettings(saved);

        Assertions.assertEquals("conflict_name", saved.get("targetName"));
        Assertions.assertEquals(false, saved.get("targetNameIsRaw"));
    }

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
    public void savedRawViewIsNotRedirectedToUnquotedTable() throws Exception {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSObjectContainer container = Mockito.mock(DBSObjectContainer.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        DBSView view = Mockito.mock(DBSView.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSTable table = Mockito.mock(DBSTable.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSDataContainer source = Mockito.mock(DBSDataContainer.class);
        DBRRunnableContext context = Mockito.mock(DBRRunnableContext.class);

        settings.setContainer(container);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(view);
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(table);

        DatabaseMappingContainer mapping = Mockito.spy(new DatabaseMappingContainer(settings, source));
        Mockito.doReturn(Collections.emptyList()).when(mapping).getAttributeMappings(Mockito.any());

        mapping.loadSettings(context, Map.of(
            "targetName", "\"foo\"",
            "mappingType", "existing",
            "targetNameIsRaw", true));

        Assertions.assertNull(mapping.getTarget());
        Assertions.assertEquals(DatabaseMappingType.create, mapping.getMappingType());
        Assertions.assertTrue(mapping.isTargetNameRaw());

        Map<String, Object> saved = new LinkedHashMap<>();
        mapping.saveSettings(saved);

        Assertions.assertEquals("\"foo\"", saved.get("targetName"));
        Assertions.assertEquals(true, saved.get("targetNameIsRaw"));
    }

    @Test
    public void demotedRawViewTargetIsNotRedirectedOnRefresh() throws Exception {
        DatabaseConsumerSettings settings = new DatabaseConsumerSettings();
        DBSObjectContainer container = Mockito.mock(DBSObjectContainer.class);
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        DBSView view = Mockito.mock(DBSView.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSTable table = Mockito.mock(DBSTable.class, Mockito.withSettings().extraInterfaces(DBSDataManipulator.class));
        DBSDataContainer source = Mockito.mock(DBSDataContainer.class);
        DBRRunnableContext context = Mockito.mock(DBRRunnableContext.class);

        settings.setContainer(container);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        Mockito.when(dialect.getUnquotedIdentifier("\"foo\"", true)).thenReturn("foo");
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("\"foo\""))).thenReturn(view);
        Mockito.when(container.getChild(Mockito.any(), Mockito.eq("foo"))).thenReturn(table);

        DatabaseMappingContainer mapping = Mockito.spy(new DatabaseMappingContainer(settings, source));
        Mockito.doReturn(Collections.emptyList()).when(mapping).getAttributeMappings(Mockito.any());

        mapping.loadSettings(context, Map.of(
            "targetName", "\"foo\"",
            "mappingType", "existing",
            "targetNameIsRaw", true));

        Assertions.assertEquals(DatabaseMappingType.create, mapping.getMappingType());
        Assertions.assertThrows(
            DBCException.class,
            () -> DatabaseTransferUtils.refreshDatabaseMappings(new VoidProgressMonitor(), settings, mapping, false));
        Assertions.assertNull(mapping.getTarget());
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
        Mockito.when(dialect.getUnquotedIdentifier("my_view", true)).thenReturn("my_view");
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
        Mockito.when(dialect.getUnquotedIdentifier("my_table", true)).thenReturn("my_table");
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

    private static void assertNewTableTargetName(String targetName, String expectedName) throws Exception {
        DBPDataSource dataSource = mockBasicDataSource();
        DBSTable table = (DBSTable) Mockito.mock(
            DBSTable.class,
            Mockito.withSettings().extraInterfaces(DBPNamedObject2.class)
        );
        Mockito.when(table.getDataSource()).thenReturn(dataSource);

        Assertions.assertEquals(
            expectedName,
            DatabaseTransferUtils.getTableFinalName(targetName, DBSTable.class, table, true, false)
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
