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
import java.util.List;

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
        if (conversation.isActive()) {
            throw new DBException("Cannot change database context while the conversation is active");
        }
        for (DatabaseEdit database : databases) {
            if (!database.isChanged()) {
                continue;
            }
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
        boolean changed = false;
        for (DatabaseEdit database : databases) {
            if (!database.isChanged()) {
                continue;
            }
            DBPDataSourceContainer container = conversation.getDataSource(database.container.getId());
            if (container != database.container) {
                throw new DBException("The conversation connection has changed");
            }
            AIContextSettingsDataSource connectionSettings = new AIContextSettingsDataSource(container);
            if (database.connectionLevel) {
                connectionSettings.setScope(database.settings.getScope());
                connectionSettings.setCustomObjectIds(database.settings.getScope() == AIDatabaseScope.CUSTOM
                    ? database.settings.getCustomObjectIds() : null);
                connectionSettings.saveSettings();
                conversation.setCustomSettings(container, null);
            } else {
                database.settings.setConversationScoped(true);
                database.settings.setMetaTransferConfirmed(connectionSettings.isMetaTransferConfirmed());
                database.settings.setMcpEnabled(connectionSettings.isMcpEnabled());
                conversation.setCustomSettings(container, database.settings);
            }
            changed = true;
        }
        if (changed) {
            session.saveConversationSettings(monitor, conversation);
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
