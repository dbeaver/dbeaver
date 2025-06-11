/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Control command handler
 */
public class SQLCommandAI implements SQLControlCommandHandler {
    private static final DBCOutputSeverity AI_OUTPUT_SEVERITY = new DBCOutputSeverity() {
        @NotNull
        @Override
        public String getName() {
            return "AI";
        }

        @Override
        public boolean isForced() {
            return true;
        }
    };

    @NotNull
    @Override
    public SQLControlResult handleCommand(@NotNull DBRProgressMonitor monitor, @NotNull SQLControlCommand command, @NotNull SQLScriptContext scriptContext) throws DBException {
        DBPDataSource dataSource = command.getDataSource();
        if (dataSource == null) {
            throw new DBException("Not connected to database");
        }

        String prompt = command.getParameter();
        if (CommonUtils.isEmptyTrimmed(prompt)) {
            throw new DBException("Empty AI prompt");
        }

        AIBaseFeatures.SQL_AI_COMMAND.use();

        final DBSLogicalDataSource lDataSource = new DBSLogicalDataSource(
            command.getDataSourceContainer(), "AI logical wrapper", null);

        DBPDataSourceContainer dataSourceContainer = lDataSource.getDataSourceContainer();
        AICompletionSettings completionSettings = new AICompletionSettings(dataSourceContainer);
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
        AIDatabaseScope scope = completionSettings.getScope();
        AIDatabaseContext.Builder contextBuilder = new AIDatabaseContext.Builder(lDataSource)
            .setScope(scope);
        DBCExecutionContext executionContext = scriptContext.getExecutionContext();
        if (executionContext != null) {
            contextBuilder.setExecutionContext(executionContext);
        }
        if (scope == AIDatabaseScope.CUSTOM) {
            contextBuilder.setCustomEntities(
                AITextUtils.loadCustomEntities(
                    monitor,
                    dataSource,
                    Arrays.stream(completionSettings.getCustomObjectIds()).collect(Collectors.toSet()))
            );
        }
        final AIDatabaseContext aiContext = contextBuilder.build();

        AICommandResult result = AIAssistantRegistry.getInstance()
            .createAssistant(dataSourceContainer.getProject().getWorkspace())
            .command(monitor, new AICommandRequest(prompt, aiContext));

        if (result.sql() == null) {
            if (!CommonUtils.isEmpty(result.message())) {
                throw new DBException(result.message());
            }
            throw new DBException("Empty AI response for '" + prompt + "'");
        }

        SQLDialect dialect = SQLUtils.getDialectFromObject(dataSource);
        if (!result.sql().contains("\n") && SQLUtils.isCommentLine(dialect, result.sql())) {
            throw new DBException(result.sql());
        }

        scriptContext.getOutputWriter().println(AI_OUTPUT_SEVERITY, prompt + " ==> " + result.sql() + "\n");

        return SQLControlResult.transform(new SQLQuery(dataSource, result.sql()));
    }
}
