/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.commands;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.completion.*;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Control command handler
 */
public class SQLCommandAI implements SQLControlCommandHandler {

    private static final Log log = Log.getLog(SQLCommandAI.class);

    @NotNull
    @Override
    public SQLControlResult handleCommand(@NotNull DBRProgressMonitor monitor, @NotNull SQLControlCommand command, @NotNull SQLScriptContext scriptContext) throws DBException {
        if (command.getDataSource() == null) {
            throw new DBException("Not connected to database");
        }
        AISettings aiSettings = AISettingsRegistry.getInstance().getSettings();
        if (aiSettings.isAiDisabled()) {
            throw new DBException("AI services are disabled");
        }
        DAICompletionEngine<?> engine = AIEngineRegistry.getInstance().getCompletionEngine(
            aiSettings.getActiveEngine());

        String prompt = command.getParameter();
        if (CommonUtils.isEmptyTrimmed(prompt)) {
            throw new DBException("Empty AI prompt");
        }

        IAIFormatter formatter = AIFormatterRegistry.getInstance().getFormatter(AIConstants.CORE_FORMATTER);

        final DBSLogicalDataSource dataSource = new DBSLogicalDataSource(
            command.getDataSourceContainer(), "AI logical wrapper", null);

        DBPDataSourceContainer dataSourceContainer = dataSource.getDataSourceContainer();
        DAICompletionSettings completionSettings = new DAICompletionSettings(dataSourceContainer);
        if (!DBWorkbench.getPlatform().getApplication().isHeadlessMode() && !completionSettings.isMetaTransferConfirmed()) {
            if (DBWorkbench.getPlatformUI().confirmAction("Do you confirm AI usage",
                "Do you confirm AI usage for '" + dataSourceContainer.getName() + "'?"
            )) {
                completionSettings.setMetaTransferConfirmed(true);
                completionSettings.saveSettings();
            } else {
                throw new DBException("AI services restricted for '" + dataSourceContainer.getName() + "'");
            }
        }
        DAICompletionScope scope = completionSettings.getScope();
        DAICompletionContext.Builder contextBuilder = new DAICompletionContext.Builder()
            .setScope(scope)
            .setDataSource(dataSource)
            .setExecutionContext(scriptContext.getExecutionContext());
        if (scope == DAICompletionScope.CUSTOM) {
            contextBuilder.setCustomEntities(
                AITextUtils.loadCustomEntities(
                    monitor,
                    command.getDataSource(),
                    Arrays.stream(completionSettings.getCustomObjectIds()).collect(Collectors.toSet()))
            );
        }
        final DAICompletionContext aiContext = contextBuilder.build();

        DAICompletionSession aiSession = new DAICompletionSession();
        aiSession.add(new DAICompletionMessage(DAICompletionMessage.Role.USER, prompt));

        List<DAICompletionResponse> responses = engine.performSessionCompletion(
            monitor,
            aiContext,
            aiSession,
            formatter,
            true);

        DAICompletionResponse response = responses.get(0);
        MessageChunk[] messageChunks = AITextUtils.splitIntoChunks(
            CommonUtils.notEmpty(response.getResultCompletion()));

        String finalSQL = null;
        StringBuilder messages = new StringBuilder();
        for (MessageChunk chunk : messageChunks) {
            if (chunk instanceof MessageChunk.Code code) {
                finalSQL = code.text();
            } else if (chunk instanceof MessageChunk.Text text) {
                messages.append(text.text());
            }
        }
        if (finalSQL == null) {
            if (!messages.isEmpty()) {
                throw new DBException(messages.toString());
            }
            throw new DBException("Empty AI completion for '" + prompt + "'");
        }

        scriptContext.getOutputWriter().println(AIOutputSeverity.PROMPT, prompt + " ==> " + finalSQL + "\n");
        return SQLControlResult.transform(
            new SQLQuery(command.getDataSource(), finalSQL));
    }

}
