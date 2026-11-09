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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AIChatContextSettingsEdit {
    private final AIChatSession session;
    private final AIChatConversation conversation;
    private final List<DatabaseEdit> databases;

    public AIChatContextSettingsEdit(@NotNull AIChatSession session, @NotNull AIChatConversation conversation) {
        this.session = session;
        this.conversation = conversation;
        databases = conversation.getDataSources().stream()
            .map(container -> new DatabaseEdit(session, conversation, container)).toList();
    }

    @NotNull
    public List<DatabaseEdit> getDatabases() {
        return databases;
    }

    public void apply(@NotNull DBRProgressMonitor monitor) throws DBException {
        synchronized (conversation.getPersistenceLock()) {
            applyChanges(monitor);
        }
    }

    private void applyChanges(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (conversation.isActive()) {
            throw new DBException("Cannot change database context while the conversation is active");
        }
        List<DatabaseEdit> changedDatabases = databases.stream().filter(DatabaseEdit::isChanged).toList();
        for (DatabaseEdit database : changedDatabases) {
            if (conversation.getDataSource(database.container.getId()) != database.container) {
                throw new DBException("The conversation connection has changed");
            }
            if (database.settings.getScope() == null) {
                throw new DBException("Scope must be specified for " + database.container.getName());
            }
            if (database.settings.getScope() == AIDatabaseScope.CUSTOM
                && ArrayUtils.isEmpty(database.settings.getCustomObjectIds())) {
                throw new DBException("Select database objects for " + database.container.getName() + " before applying custom scope");
            }
        }
        if (changedDatabases.isEmpty()) {
            return;
        }
        Map<DBPDataSourceContainer, AIContextSettingsChatConversation> originalOverrides = new LinkedHashMap<>();
        Map<DBPDataSourceContainer, AIContextSettingsDataSource> originalDefaults = new LinkedHashMap<>();
        try {
            for (DatabaseEdit database : changedDatabases) {
                DBPDataSourceContainer container = database.container;
                AIContextSettingsDataSource connectionSettings = new AIContextSettingsDataSource(container);
                originalOverrides.put(container, conversation.getCustomSettings(container));
                if (database.connectionLevel) {
                    AIContextSettingsDataSource applied = new AIContextSettingsDataSource(container);
                    applied.setScope(database.settings.getScope());
                    String[] objects = database.settings.getCustomObjectIds();
                    applied.setCustomObjectIds(database.settings.getScope() == AIDatabaseScope.CUSTOM && objects != null
                        ? objects.clone() : null);
                    originalDefaults.put(container, connectionSettings);
                    saveConnectionSettings(applied);
                    conversation.setCustomSettings(container, null);
                } else {
                    AIContextSettingsChatConversation applied = new AIContextSettingsChatConversation(session, conversation, container);
                    applied.loadSettingsFromMap(database.settings.saveSettingsToMap());
                    applied.setConversationScoped(true);
                    applied.setMetaTransferConfirmed(connectionSettings.isMetaTransferConfirmed());
                    applied.setMcpEnabled(connectionSettings.isMcpEnabled());
                    conversation.setCustomSettings(container, applied);
                }
            }
            session.saveConversationSettings(monitor, conversation);
        } catch (DBException | RuntimeException | Error failure) {
            restoreSettings(originalOverrides, originalDefaults, failure);
            throw failure;
        }
    }

    private void restoreSettings(
        @NotNull Map<DBPDataSourceContainer, AIContextSettingsChatConversation> originalOverrides,
        @NotNull Map<DBPDataSourceContainer, AIContextSettingsDataSource> originalDefaults,
        @NotNull Throwable failure
    ) {
        originalOverrides.forEach(conversation::setCustomSettings);
        for (var entry : originalDefaults.entrySet()) {
            try {
                AIContextSettingsDataSource current = new AIContextSettingsDataSource(entry.getKey());
                current.setScope(entry.getValue().getScope());
                String[] objects = entry.getValue().getCustomObjectIds();
                current.setCustomObjectIds(objects == null ? null : objects.clone());
                saveConnectionSettings(current);
            } catch (DBException | RuntimeException | Error rollbackFailure) {
                if (rollbackFailure != failure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
        }
    }

    private static void saveConnectionSettings(@NotNull AIContextSettingsDataSource settings) throws DBException {
        DBPDataSourceContainer container = settings.getDataSourceContainer();
        // apply needs the persistence result that AIContextSettingsDataSource.saveSettings discards
        container.setExtension(AIContextSettingsDataSource.AI_DS_EXTENSION, settings.saveSettingsToMap());
        if (!container.persistConfiguration()) {
            throw new DBException("Cannot save database context for connection '" + container.getName() + "'",
                container.getRegistry().getLastError());
        }
    }

    public static class DatabaseEdit {
        private final DBPDataSourceContainer container;
        private final AIContextSettingsChatConversation settings;
        private final AIDatabaseScope originalScope;
        private final String[] originalObjects;
        private final boolean originalConnectionLevel;
        private boolean connectionLevel;

        private DatabaseEdit(
            @NotNull AIChatSession session,
            @NotNull AIChatConversation conversation,
            @NotNull DBPDataSourceContainer container
        ) {
            this.container = container;
            AIContextSettings current = session.getConversationSettings(conversation, container);
            settings = new AIContextSettingsChatConversation(session, conversation, container);
            settings.loadSettingsFromMap(current.saveSettingsToMap());
            originalScope = current.getScope();
            String[] objects = current.getCustomObjectIds();
            originalObjects = objects == null ? null : objects.clone();
            connectionLevel = !(current instanceof AIContextSettingsChatConversation);
            originalConnectionLevel = connectionLevel;
        }

        @NotNull
        public DBPDataSourceContainer getContainer() {
            return container;
        }

        @NotNull
        public AIContextSettingsChatConversation getSettings() {
            return settings;
        }

        public void initializeScope(@Nullable DBCExecutionContext executionContext) {
            AIUtils.updateScopeSettingsIfNeeded(settings, container, executionContext);
        }

        public boolean isConnectionLevel() {
            return connectionLevel;
        }

        public void setConnectionLevel(boolean connectionLevel) {
            this.connectionLevel = connectionLevel;
        }

        public boolean isChanged() {
            return connectionLevel != originalConnectionLevel || originalScope != settings.getScope()
                || !Arrays.equals(originalObjects, settings.getCustomObjectIds());
        }
    }
}
