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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatHistoryMapper;
import org.jkiss.dbeaver.model.ai.qm.QMAIContext;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AIChatContextSettingsEditTest extends DBeaverUnitTest {
    private final DBPDataSourceContainer primary = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPDataSourceContainer additional = Mockito.mock(DBPDataSourceContainer.class);
    private final Map<String, Object> primarySettings = new HashMap<>();
    private final Map<String, Object> additionalSettings = new HashMap<>();
    private MockedConstruction<BundlePreferenceStore> preferences;
    private AIChatConversation conversation;
    private AIChatSession session;

    @BeforeEach
    public void setUp() throws DBException {
        preferences = Mockito.mockConstruction(BundlePreferenceStore.class);
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getId()).thenReturn("project");
        configure(primary, "primary", project, primarySettings);
        configure(additional, "additional", project, additionalSettings);
        conversation = new AIChatConversation(UUID.randomUUID(), "Chat", Mockito.mock(AIPromptGenerator.class),
            List.of(), primary, 0, null);
        conversation.addDataSource(additional);
        session = Mockito.spy(new AIChatSession(Mockito.mock(DBPWorkspace.class), Mockito.mock(AIChatContextProvider.class),
            Mockito.mock(AIChatStorage.class), monitor -> "session", Mockito.mock(UserTokenQuotaService.class)));
        Mockito.doNothing().when(session).saveConversationSettings(Mockito.any(), Mockito.same(conversation));
    }

    @AfterEach
    public void tearDown() {
        preferences.close();
    }

    @Test
    public void pendingEditsDoNotChangeConnectionOrConversation() {
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().getFirst().getSettings().setScope(AIDatabaseScope.CUSTOM);
        edit.getDatabases().getFirst().getSettings().setCustomObjectIds(new String[]{"primary.table"});
        edit.getDatabases().getFirst().setConnectionLevel(false);
        Assertions.assertNull(conversation.getCustomSettings());
        Assertions.assertNull(conversation.getCustomSettings(additional));
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, new AIContextSettingsDataSource(primary).getScope());
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
        Mockito.verify(additional, Mockito.never()).persistConfiguration();
    }

    @Test
    public void applySavesBothConversationScopesAndKeepsThemIndependent() throws DBException {
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        AIChatContextSettingsEdit.DatabaseEdit first = edit.getDatabases().getFirst();
        first.getSettings().setScope(AIDatabaseScope.CUSTOM);
        first.getSettings().setCustomObjectIds(new String[]{"primary.table"});
        first.setConnectionLevel(false);
        AIChatContextSettingsEdit.DatabaseEdit second = edit.getDatabases().get(1);
        second.getSettings().setScope(AIDatabaseScope.CUSTOM);
        second.getSettings().setCustomObjectIds(new String[]{"additional.table"});
        second.setConnectionLevel(false);
        edit.apply(new VoidProgressMonitor());
        Assertions.assertSame(primary, conversation.getCustomSettings().getDataSourceContainer());
        Assertions.assertSame(additional, conversation.getCustomSettings(additional).getDataSourceContainer());
        Assertions.assertArrayEquals(new String[]{"primary.table"}, session.getConversationSettings(conversation).getCustomObjectIds());
        Assertions.assertArrayEquals(new String[]{"additional.table"},
            session.getConversationSettings(conversation, additional).getCustomObjectIds());
        Mockito.verify(session).saveConversationSettings(Mockito.any(), Mockito.same(conversation));
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
        Mockito.verify(additional, Mockito.never()).persistConfiguration();
    }

    @Test
    public void explicitConversationLevelIsRetainedWhenScopeEqualsConnectionDefault() throws DBException {
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().getFirst().setConnectionLevel(false);
        edit.apply(new VoidProgressMonitor());
        Assertions.assertInstanceOf(AIContextSettingsChatConversation.class, session.getConversationSettings(conversation));
    }

    @Test
    public void connectionLevelClearsOnlyThatConnectionsOverride() throws DBException {
        AIContextSettingsChatConversation first = new AIContextSettingsChatConversation(session, conversation);
        first.setScope(AIDatabaseScope.CUSTOM);
        first.setCustomObjectIds(new String[]{"primary.table"});
        conversation.setCustomSettings(first);
        AIContextSettingsChatConversation second = new AIContextSettingsChatConversation(session, conversation, additional);
        second.setScope(AIDatabaseScope.CUSTOM);
        second.setCustomObjectIds(new String[]{"additional.table"});
        conversation.setCustomSettings(additional, second);
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().get(1).setConnectionLevel(true);
        edit.apply(new VoidProgressMonitor());
        Assertions.assertSame(first, conversation.getCustomSettings());
        Assertions.assertNull(conversation.getCustomSettings(additional));
        Assertions.assertArrayEquals(new String[]{"additional.table"}, new AIContextSettingsDataSource(additional).getCustomObjectIds());
        Mockito.verify(additional).persistConfiguration();
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
    }

    @Test
    public void applyDoesNotRestoreRevokedMetadataConsent() throws DBException {
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().get(1).setConnectionLevel(false);
        additionalSettings.put("confirmed", false);
        edit.apply(new VoidProgressMonitor());
        Assertions.assertFalse(conversation.getCustomSettings(additional).isMetaTransferConfirmed());
        Assertions.assertFalse(new AIContextSettingsDataSource(additional).isMetaTransferConfirmed());
    }

    @Test
    public void emptyCustomScopesAreRejectedAtBothSettingsLevels() throws DBException {
        for (boolean connectionLevel : new boolean[]{true, false}) {
            for (String[] objects : new String[][]{null, new String[0]}) {
                AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
                AIChatContextSettingsEdit.DatabaseEdit database = edit.getDatabases().getFirst();
                database.getSettings().setScope(AIDatabaseScope.CUSTOM);
                database.getSettings().setCustomObjectIds(objects);
                database.setConnectionLevel(connectionLevel);
                Assertions.assertThrows(DBException.class, () -> edit.apply(new VoidProgressMonitor()));
            }
        }
        Assertions.assertNull(conversation.getCustomSettings());
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, new AIContextSettingsDataSource(primary).getScope());
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
        Mockito.verify(session, Mockito.never()).saveConversationSettings(Mockito.any(), Mockito.same(conversation));
    }

    @Test
    public void invalidAdditionalScopeRejectsBatchBeforeSavingPrimary() throws DBException {
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().getFirst().getSettings().setScope(AIDatabaseScope.CURRENT_DATABASE);
        edit.getDatabases().get(1).getSettings().setScope(AIDatabaseScope.CUSTOM);
        Assertions.assertThrows(DBException.class, () -> edit.apply(new VoidProgressMonitor()));
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, new AIContextSettingsDataSource(primary).getScope());
        Assertions.assertNull(conversation.getCustomSettings(additional));
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
        Mockito.verify(additional, Mockito.never()).persistConfiguration();
    }

    @Test
    public void unsetScopeUsesRequestDefaultAndRemainsPendingUntilApply() throws DBException {
        primarySettings.remove("scope");
        Mockito.when(primary.isConnected()).thenReturn(true);
        DBCExecutionContext execution = createExecutionContext(true);
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        AIChatContextSettingsEdit.DatabaseEdit database = edit.getDatabases().getFirst();
        database.initializeScope(execution);
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATABASE, database.getSettings().getScope());
        Assertions.assertTrue(database.isChanged());
        AIContextSettingsDataSource requestSettings = new AIContextSettingsDataSource(primary);
        Assertions.assertNull(requestSettings.getScope());
        AIUtils.updateScopeSettingsIfNeeded(requestSettings, primary, execution);
        Assertions.assertEquals(requestSettings.getScope(), database.getSettings().getScope());
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
        edit.apply(new VoidProgressMonitor());
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATABASE, new AIContextSettingsDataSource(primary).getScope());
    }

    @Test
    public void offlineDefaultIsResolvedOnlyAfterConnecting() {
        additionalSettings.remove("scope");
        AIChatContextSettingsEdit.DatabaseEdit database = new AIChatContextSettingsEdit(session, conversation).getDatabases().get(1);
        database.initializeScope(null);
        Assertions.assertNull(database.getSettings().getScope());
        Assertions.assertFalse(database.isChanged());
        Mockito.when(additional.isConnected()).thenReturn(true);
        database.initializeScope(createExecutionContext(true));
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATABASE, database.getSettings().getScope());
        Assertions.assertNull(new AIContextSettingsDataSource(additional).getScope());
    }

    @Test
    public void flatDatabaseDefaultIsAllObjects() {
        primarySettings.remove("scope");
        Mockito.when(primary.isConnected()).thenReturn(true);
        AIChatContextSettingsEdit.DatabaseEdit database = new AIChatContextSettingsEdit(session, conversation).getDatabases().getFirst();
        database.initializeScope(createExecutionContext(false));
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, database.getSettings().getScope());
    }

    @Test
    public void missingContextDefaultsKeepRequestDefault() {
        primarySettings.remove("scope");
        Mockito.when(primary.isConnected()).thenReturn(true);
        AIChatContextSettingsEdit.DatabaseEdit database = new AIChatContextSettingsEdit(session, conversation).getDatabases().getFirst();
        database.initializeScope(Mockito.mock(DBCExecutionContext.class));
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATABASE, database.getSettings().getScope());
    }

    @Test
    public void unchangedOfflineSettingsDoNotBlockOtherDatabaseEdits() throws DBException {
        additionalSettings.remove("scope");
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().getFirst().getSettings().setScope(AIDatabaseScope.CURRENT_DATABASE);
        edit.apply(new VoidProgressMonitor());
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATABASE, new AIContextSettingsDataSource(primary).getScope());
        Assertions.assertNull(new AIContextSettingsDataSource(additional).getScope());
        Mockito.verify(additional, Mockito.never()).persistConfiguration();
    }

    @Test
    public void initializingScopePreservesExplicitSelection() {
        Mockito.when(primary.isConnected()).thenReturn(true);
        AIChatContextSettingsEdit.DatabaseEdit database = new AIChatContextSettingsEdit(session, conversation).getDatabases().getFirst();
        database.getSettings().setScope(AIDatabaseScope.CUSTOM);
        database.getSettings().setCustomObjectIds(new String[]{"primary.table"});
        database.initializeScope(createExecutionContext(true));
        Assertions.assertEquals(AIDatabaseScope.CUSTOM, database.getSettings().getScope());
        Assertions.assertArrayEquals(new String[]{"primary.table"}, database.getSettings().getCustomObjectIds());
    }

    @Test
    public void changingSettingsLevelWithoutResolvedScopeIsRejected() {
        primarySettings.remove("scope");
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().getFirst().setConnectionLevel(false);
        Assertions.assertThrows(DBException.class, () -> edit.apply(new VoidProgressMonitor()));
        Assertions.assertNull(conversation.getCustomSettings());
    }

    @Test
    public void detachedConnectionRejectsBatchBeforeAnySettingsArePersisted() throws DBException {
        AIChatContextSettingsEdit edit = new AIChatContextSettingsEdit(session, conversation);
        edit.getDatabases().getFirst().getSettings().setScope(AIDatabaseScope.CUSTOM);
        edit.getDatabases().get(1).getSettings().setScope(AIDatabaseScope.CUSTOM);
        conversation.removeDataSource("additional");
        Assertions.assertThrows(DBException.class, () -> edit.apply(new VoidProgressMonitor()));
        Mockito.verify(primary, Mockito.never()).persistConfiguration();
        Mockito.verify(additional, Mockito.never()).persistConfiguration();
    }

    @Test
    public void scopeOverridesRoundTripAlongsideLegacyHistoryAndConnectionMembership() throws DBException {
        AIContextSettingsChatConversation settings = new AIContextSettingsChatConversation(session, conversation, additional);
        settings.setScope(AIDatabaseScope.CUSTOM);
        settings.setCustomObjectIds(new String[]{"additional.table"});
        conversation.setCustomSettings(additional, settings);
        QMAIContext saved = QMAIChatHistoryMapper.toQMAIChatHistory(
            conversation, session.getConversationSettings(conversation), null).getContext();
        QMAIContext restored = new QMAIContext(saved.getContextJson(), Set.of());
        AIContextSettingsChatConversation loaded = new AIContextSettingsChatConversation(session, conversation, additional);
        loaded.loadSettingsFromMap(restored.getConnectionSettings().get("additional"));
        Assertions.assertEquals(AIDatabaseScope.CUSTOM, loaded.getScope());
        Assertions.assertArrayEquals(new String[]{"additional.table"}, loaded.getCustomObjectIds());
        Assertions.assertEquals("additional", restored.getAdditionalDataSources().getFirst().dataSourceId());
        Assertions.assertTrue(new QMAIContext("{\"scope\":\"CURRENT_SCHEMA\"}", Set.of()).getConnectionSettings().isEmpty());
        conversation.removeDataSource("additional");
        Assertions.assertNull(conversation.getCustomSettings(additional));
    }

    @Test
    public void sessionRestoresAdditionalConversationScopeForTheCorrectDatabase() throws DBException {
        AIContextSettingsChatConversation settings = new AIContextSettingsChatConversation(session, conversation, additional);
        settings.setScope(AIDatabaseScope.CUSTOM);
        settings.setCustomObjectIds(new String[]{"additional.table"});
        conversation.setCustomSettings(additional, settings);
        var history = QMAIChatHistoryMapper.toQMAIChatHistory(
            conversation, session.getConversationSettings(conversation), null);
        DBPProject project = primary.getProject();
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        Mockito.when(workspace.getProjectById("project")).thenReturn(project);
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(registry.getDataSource("primary")).thenReturn(primary);
        Mockito.when(registry.getDataSource("additional")).thenReturn(additional);
        AIChatStorage storage = Mockito.mock(AIChatStorage.class);
        Mockito.when(storage.findConversationSummaries("session")).thenReturn(List.of(history));
        AIChatSession loadedSession = Mockito.spy(new AIChatSession(workspace, Mockito.mock(AIChatContextProvider.class), storage,
            monitor -> "session", Mockito.mock(UserTokenQuotaService.class)));
        AIAssistant assistant = Mockito.mock(AIAssistant.class);
        Mockito.doReturn(assistant).when(loadedSession).getAssistant();
        AIChatConversation loaded = new AIChatConversation(conversation.getId(), "Chat", conversation.getPromptGenerator(),
            List.of(), primary, 0, null);
        try (var mapper = Mockito.mockStatic(QMAIChatHistoryMapper.class)) {
            mapper.when(() -> QMAIChatHistoryMapper.toAIChatConversation(assistant, history, primary)).thenReturn(loaded);
            loadedSession.init(new VoidProgressMonitor());
        }
        Assertions.assertSame(loaded, loadedSession.getConversation(conversation.getId()));
        Assertions.assertSame(additional, loaded.getCustomSettings(additional).getDataSourceContainer());
        Assertions.assertEquals(AIDatabaseScope.CUSTOM, loadedSession.getConversationSettings(loaded, additional).getScope());
        Assertions.assertArrayEquals(new String[]{"additional.table"},
            loadedSession.getConversationSettings(loaded, additional).getCustomObjectIds());
        Assertions.assertEquals(AIDatabaseScope.CURRENT_DATASOURCE, loadedSession.getConversationSettings(loaded).getScope());
    }

    @NotNull
    private static DBCExecutionContext createExecutionContext(boolean catalogs) {
        DBCExecutionContext execution = Mockito.mock(DBCExecutionContext.class);
        DBCExecutionContextDefaults<?, ?> defaults = Mockito.mock(DBCExecutionContextDefaults.class);
        Mockito.when(execution.getContextDefaults()).thenReturn(defaults);
        Mockito.when(defaults.supportsCatalogChange()).thenReturn(catalogs);
        return execution;
    }

    private static void configure(
        @NotNull DBPDataSourceContainer container,
        @NotNull String id,
        @NotNull DBPProject project,
        @NotNull Map<String, Object> settings
    ) {
        settings.put("confirmed", true);
        settings.put("scope", "CURRENT_DATASOURCE");
        Mockito.when(container.getId()).thenReturn(id);
        Mockito.when(container.getProject()).thenReturn(project);
        Mockito.when(container.getExtension(AIContextSettingsDataSource.AI_DS_EXTENSION)).thenAnswer(invocation -> settings);
        Mockito.doAnswer(invocation -> {
            settings.clear();
            settings.putAll(invocation.getArgument(1));
            return null;
        }).when(container).setExtension(Mockito.eq(AIContextSettingsDataSource.AI_DS_EXTENSION), Mockito.any());
    }
}
