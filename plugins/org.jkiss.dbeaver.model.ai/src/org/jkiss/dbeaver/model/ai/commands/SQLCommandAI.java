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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.completion.*;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLControlCommand;
import org.jkiss.dbeaver.model.sql.SQLControlCommandHandler;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Control command handler
 */
public class SQLCommandAI implements SQLControlCommandHandler {

    @Override
    public boolean handleCommand(SQLControlCommand command, SQLScriptContext scriptContext) throws DBException {
        try {
            new AbstractJob("Execute prompt") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        performCompletion(monitor, command, scriptContext);
                    } catch (Exception e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("AI error", "Cannot generate completion", e);
            return false;
        }

        return true;
    }

    private static void performCompletion(
        DBRProgressMonitor monitor,
        SQLControlCommand command,
        SQLScriptContext scriptContext
    ) throws DBException {
        AISettings aiSettings = AISettingsRegistry.getInstance().getSettings();
        DAICompletionEngine<?> engine = AIEngineRegistry.getInstance().getCompletionEngine(
            aiSettings.getActiveEngine());

        String prompt = command.getParameter();

        IAIFormatter formatter = AIFormatterRegistry.getInstance().getFormatter(AIConstants.CORE_FORMATTER);

        final DBSLogicalDataSource dataSource = new DBSLogicalDataSource(
            command.getDataSourceContainer(), "AI logical wrapper", null);

        final DAICompletionContext aiContext = new DAICompletionContext.Builder()
            .setScope(DAICompletionScope.CURRENT_SCHEMA)
            .setDataSource(dataSource)
            .setExecutionContext(scriptContext.getExecutionContext())
            .build();

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

        if (messageChunks.length == 0) {
            return;
        }

        String finalSQL = null;
        for (MessageChunk chunk : messageChunks) {
            if (chunk instanceof MessageChunk.Code code) {
                finalSQL = code.text();
            }
        }

        System.out.println(finalSQL);
    }

}
