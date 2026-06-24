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
public class AIChatConversationSettings extends AIContextSettings {

    private final AIChatSession session;
    private final AIChatConversation conversation;

    public AIChatConversationSettings(
        @NotNull AIChatSession session,
        @NotNull AIChatConversation conversation
    ) {
        this.session = session;
        this.conversation = conversation;
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return conversation.getDataSource();
    }

    @Override
    public void saveSettings() throws DBException {
        DBPDataSourceContainer dataSource = this.conversation.getDataSource();
        if (dataSource != null) {
            AICompletionSettings dsSettings = new AICompletionSettings(dataSource);
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
            AICompletionSettings dsSettings = new AICompletionSettings(dataSourceContainer);
            setMetaTransferConfirmed(dsSettings.isMetaTransferConfirmed());
        }
    }
}
