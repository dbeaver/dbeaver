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
package org.jkiss.dbeaver.model.ai.prompt;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AISqlJoinRule;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.impl.AIPromptUtils;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSourceSupplier;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class AIPromptGenerateSql extends AIPromptAbstract {

    public static final String SQL_GENERATOR_ID = "sql";

    @NotNull
    @Override
    public String generatorId() {
        return SQL_GENERATOR_ID;
    }

    public static void addSqlGenerateInstructions(
        @NotNull DBSLogicalDataSourceSupplier dsSupplier,
        @NotNull AIPromptAbstract builder
    ) {
        DBSLogicalDataSource dataSource = dsSupplier.get();
        builder.addInstructions(AIPromptUtils.createGeneralRulesInstructions());
        if (dataSource != null) {
            builder
                .addContexts(AIPromptUtils.describeDataSourceInfo(dataSource))
                .addInstructions(AIPromptUtils.createGenerateQueryInstructions(dataSource));
        }
        builder.addOutputFormats(AIPromptUtils.SQL_OUTPUT_FORMATS);

        addJoinInstructions(builder);
    }

    public static void addJoinInstructions(@NotNull AIPromptAbstract builder) {
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
    protected void initializePrompt(@Nullable AIDatabaseContext context) {
        addSqlGenerateInstructions(() -> context == null ? null : context.getDataSource(), this);
    }
}
