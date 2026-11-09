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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

/**
 * AI chat conversation settings.
 */
public class AIContextSettingsChatConversation extends AIContextSettings {

    private final AIChatSession session;
    private final AIChatConversation conversation;
    private final DBPDataSourceContainer dataSourceContainer;

    public AIContextSettingsChatConversation(
        @NotNull AIChatSession session,
        @NotNull AIChatConversation conversation
    ) {
        this(session, conversation, conversation.getDataSource());
    }

    public AIContextSettingsChatConversation(
        @NotNull AIChatSession session,
        @NotNull AIChatConversation conversation,
        @Nullable DBPDataSourceContainer dataSourceContainer
    ) {
        this.session = session;
        this.conversation = conversation;
        this.dataSourceContainer = dataSourceContainer;
        setConversationScoped(true);
    }

    public boolean isConversationScoped() {
        return Boolean.TRUE.equals(settings.conversationScope);
    }

    public void setConversationScoped(boolean conversationScoped) {
        settings.conversationScope = conversationScoped;
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @Override
    public void saveSettings() throws DBException {
        DBPDataSourceContainer dataSource = getDataSourceContainer();
        if (dataSource != null) {
            AIContextSettingsDataSource dsSettings = new AIContextSettingsDataSource(dataSource);
            if (dsSettings.isMetaTransferConfirmed() != this.isMetaTransferConfirmed()) {
                // Update DS settings
                dsSettings.setMetaTransferConfirmed(this.isMetaTransferConfirmed());
                dsSettings.saveSettings();
            }
        }
        session.saveConversationSettings(new VoidProgressMonitor(), this.conversation);
    }

    // Set meta transfer confirm from datasource config
    public void loadDataSourceDefaults() {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer != null) {
            AIContextSettingsDataSource dsSettings = new AIContextSettingsDataSource(dataSourceContainer);
            setMetaTransferConfirmed(dsSettings.isMetaTransferConfirmed());
            if (getScope() == null && dsSettings.getScope() != null) {
                setScope(dsSettings.getScope());
                String[] customObjectIds = dsSettings.getCustomObjectIds();
                setCustomObjectIds(customObjectIds == null ? null : customObjectIds.clone());
            }
        }
    }
}
