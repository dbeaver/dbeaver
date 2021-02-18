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
package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.DB2Messages;

public enum DB2RunstatsOptions {

    colsAllAndDistribution(DB2Messages.dialog_table_tools_runstats_cols_all_and_distribution,"ON ALL COLUMNS WITH DISTRIBUTION ON ALL COLUMNS"), //$NON-NLS-1$
    colsAll(DB2Messages.dialog_table_tools_runstats_cols_all, "ON ALL COLUMNS"), //$NON-NLS-1$
    colsNo(DB2Messages.dialog_table_tools_runstats_cols_no, ""), //$NON-NLS-1$

    indexesDetailed(DB2Messages.dialog_table_tools_runstats_indexes_detailed, "AND SAMPLED DETAILED INDEXES ALL"), //$NON-NLS-1$
    indexesAll(DB2Messages.dialog_table_tools_runstats_indexes_all, "AND INDEXES ALL"), //$NON-NLS-1$
    indexesNo(DB2Messages.dialog_table_tools_runstats_indexes_no, "");  //$NON-NLS-1$

    private final String desc, ddlString;

    DB2RunstatsOptions(String desc, String ddl) {
        this.desc = desc;
        this.ddlString = ddl;
    }

    public static DB2RunstatsOptions getOption(String description){
        if (description != null) {
            for (DB2RunstatsOptions option : DB2RunstatsOptions.values()) {
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
