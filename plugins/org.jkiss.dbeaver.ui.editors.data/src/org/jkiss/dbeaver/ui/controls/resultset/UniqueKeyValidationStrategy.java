/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

public enum UniqueKeyValidationStrategy implements DBPNamedObject {
    USE_ALL_COLUMNS(ResultSetMessages.key_validation_strategy_use_all_columns),
    DISABLE_EDITING(ResultSetMessages.key_validation_strategy_disable_editing),
    PROMPT(ResultSetMessages.key_validation_strategy_prompt);

    private final String name;

    UniqueKeyValidationStrategy(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public static UniqueKeyValidationStrategy of(@NotNull DBPPreferenceStore store) {
        final String value = store.getString(ResultSetPreferences.RS_EDIT_KEY_VALIDATION_STRATEGY);

        // Backward compatibility
        if (CommonUtils.isEmpty(value)) {
            if (store.getBoolean(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS)) {
                return USE_ALL_COLUMNS;
            } else {
                return PROMPT;
            }
        }

        return CommonUtils.valueOf(UniqueKeyValidationStrategy.class, value, PROMPT);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }
}
