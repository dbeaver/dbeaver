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
package org.jkiss.dbeaver.model.ai.prompts;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIPromptBuilder;
import org.jkiss.dbeaver.model.ai.AISqlJoinRule;
import org.jkiss.dbeaver.model.ai.impl.AIPromptUtils;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class AIGenerateSqlPromptBuilder extends AIPromptBuilder {

    @NotNull
    public static AIGenerateSqlPromptBuilder create(DBSLogicalDataSource dataSource) {
        AIGenerateSqlPromptBuilder builder = new AIGenerateSqlPromptBuilder();
        builder
            .addContexts(AIPromptUtils.describeDataSourceInfo(dataSource))
            .addInstructions(AIPromptUtils.createDatabaseInstructions(dataSource))
            .addGoals(
                "Help users write SQL queries.",
                "Provide information about SQL syntax, functions, and best practices.",
                "Assist with database design and data modeling.",
                "Answer questions about database concepts and technologies.",
                "Provide information about database performance tuning and optimization."
            )
            .addOutputFormats(
                "Place any explanation or comments before the SQL code block.",
                "Provide the SQL query in a fenced Markdown code block."
            );

        addJoinInstructions(builder);

        return builder;
    }

    public static void addJoinInstructions(AIPromptBuilder builder) {
        AISqlJoinRule joinRule = CommonUtils.valueOf(
            AISqlJoinRule.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.AI_JOIN_RULE),
            AISqlJoinRule.DEFAULT
        );

        String joinHint = switch (joinRule) {
            case JOIN -> "Use joins only.";
            case SUB_QUERY -> "Use sub‑queries only.";
            default -> "Joins and sub‑queries are allowed.";
        };

        builder.addInstructions(joinHint);
    }

    @Override
    public String build() {
        return super.build();
    }
}
