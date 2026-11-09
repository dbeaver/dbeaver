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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIContextSettings;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.utils.ArrayUtils;

final class AIChatScopeUtils {
    private AIChatScopeUtils() {
    }

    @NotNull
    static String getTitle(
        @NotNull AIContextSettings settings,
        @NotNull AIDatabaseScope scope,
        @NotNull DBPDataSourceContainer dsContainer,
        @Nullable DBCExecutionContextDefaults<?, ?> contextDefaults
    ) {
        String title = scope.getTitle();
        return switch (scope) {
            case CURRENT_SCHEMA -> {
                DBSSchema defaultSchema = contextDefaults == null ? null : contextDefaults.getDefaultSchema();
                yield title + " (" + (defaultSchema == null ?
                    AIChatMessagesUI.ai_chat_context_menu_no_schema_selected :
                    DBUtils.getObjectFullName(defaultSchema, DBPEvaluationContext.UI)) + ")";
            }
            case CURRENT_DATABASE -> {
                DBSCatalog defaultDatabase = contextDefaults == null ? null : contextDefaults.getDefaultCatalog();
                yield title + " (" + (defaultDatabase == null ?
                    AIChatMessagesUI.ai_chat_context_menu_no_database_selected :
                    DBUtils.getObjectFullName(defaultDatabase, DBPEvaluationContext.UI)) + ")";
            }
            case CURRENT_DATASOURCE -> NLS.bind(
                AIChatMessagesUI.ai_chat_context_menu_all_objects, dsContainer.getName());
            case CUSTOM -> title + " ..." + (settings.getScope() == scope ?
                "(" + (ArrayUtils.isEmpty(settings.getCustomObjectIds()) ?
                    AIChatMessagesUI.ai_chat_context_menu_empty : settings.getCustomObjectIds().length) + ")" : "");
            default -> title;
        };
    }
}
