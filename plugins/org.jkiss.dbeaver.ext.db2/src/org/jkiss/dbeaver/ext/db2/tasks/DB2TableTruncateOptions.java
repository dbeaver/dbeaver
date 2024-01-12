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
package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.DB2Messages;

public enum DB2TableTruncateOptions {

    dropStorage(DB2Messages.dialog_table_tools_truncate_drop_storage,"DROP STORAGE"), //$NON-NLS-1$
    reuseStorage(DB2Messages.dialog_table_tools_truncate_reuse_storage, "REUSE STORAGE"), //$NON-NLS-1$

    ignoreDeleteTriggers(DB2Messages.dialog_table_tools_truncate_ignore_delete_triggers, "IGNORE DELETE TRIGGERS"), //$NON-NLS-1$
    restrictWhenDeleteTriggers(DB2Messages.dialog_table_tools_truncate_restrict_when_delete_triggers, "RESTRICT WHEN DELETE TRIGGERS"); //$NON-NLS-1$

    private final String desc, ddlString;

    DB2TableTruncateOptions(String desc, String ddl) {
        this.desc = desc;
        this.ddlString = ddl;
    }

    public static DB2TableTruncateOptions getOption(String description){
        if (description != null) {
            for (DB2TableTruncateOptions option : DB2TableTruncateOptions.values()) {
                if (option.desc.equals(description)){
                    return option;
                }
            }
        }
        return null;
    }

    @NotNull
    public String getDesc() {
        return desc;
    }

    @NotNull
    public String getDdlString() {
        return ddlString;
    }
}
