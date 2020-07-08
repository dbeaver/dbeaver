/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

public enum DB2RunstatsOptions {

    COLS_ALL_AND_DISTRIBUTION(DB2Messages.dialog_table_tools_runstats_cols_all_and_distribution,"ON ALL COLUMNS WITH DISTRIBUTION ON ALL COLUMNS"), //$NON-NLS-1$
    COLS_ALL(DB2Messages.dialog_table_tools_runstats_cols_all, "ON ALL COLUMNS"), //$NON-NLS-1$
    COLS_NO(DB2Messages.dialog_table_tools_runstats_cols_no, ""), //$NON-NLS-1$

    INDEXES_DETAILED(DB2Messages.dialog_table_tools_runstats_indexes_detailed, "AND SAMPLED DETAILED INDEXES ALL"), //$NON-NLS-1$
    INDEXES_ALL(DB2Messages.dialog_table_tools_runstats_indexes_all, "AND INDEXES ALL"), //$NON-NLS-1$
    INDEXES_NO(DB2Messages.dialog_table_tools_runstats_indexes_no, "");  //$NON-NLS-1$

    private final String desc, ddlString;

    DB2RunstatsOptions(String desc, String ddl) {
        this.desc = desc;
        this.ddlString = ddl;
    }

    public static DB2RunstatsOptions getOption(String desc){
        if (desc != null) {
            if (desc.equals(COLS_ALL_AND_DISTRIBUTION.desc)){
                return COLS_ALL_AND_DISTRIBUTION;
            }
            if (desc.equals(COLS_ALL.desc)){
                return COLS_ALL;
            }
            if (desc.equals(COLS_NO.desc)){
                return COLS_NO;
            }
            if (desc.equals(INDEXES_DETAILED.desc)){
                return INDEXES_DETAILED;
            }
            if (desc.equals(INDEXES_ALL.desc)){
                return INDEXES_ALL;
            }
            if (desc.equals(INDEXES_NO.desc)){
                return INDEXES_NO;
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
