/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * SQLTableAliasInsertMode
 * <p>
 * Controls how the table alias should be formatted during code completion.
 */
public enum SQLTableAliasInsertMode {
    NONE("N/A"),
    PLAIN("my_table mt"),
    EXTENDED("my_table AS mt");

    private final String text;

    SQLTableAliasInsertMode(String text) {
        this.text = text;
    }

    @NotNull
    public String getText() {
        return text;
    }

    @NotNull
    public static SQLTableAliasInsertMode fromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
        final String prop = preferenceStore.getString(SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS);

        // Backward compatibility
        if ("true".equals(prop)) {
            return PLAIN;
        } else if ("false".equals(prop)) {
            return NONE;
        } else {
            return CommonUtils.valueOf(SQLTableAliasInsertMode.class, prop, SQLTableAliasInsertMode.PLAIN);
        }
    }
}
