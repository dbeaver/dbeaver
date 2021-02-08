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
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

import static org.jkiss.utils.CommonUtils.getLineSeparator;

public class DB2RunstatsTool extends DB2ToolWithStatus<DB2TableBase, DB2RunstatsToolSettings>{

    @NotNull
    @Override
    public DB2RunstatsToolSettings createToolSettings() {
        return new DB2RunstatsToolSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, DB2RunstatsToolSettings settings, List<DBEPersistAction> queries, DB2TableBase object) throws DBCException {
        String sql = "CALL SYSPROC.ADMIN_CMD('"; //$NON-NLS-1$
        sql += "RUNSTATS ON TABLE "; //$NON-NLS-1$
        sql += object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        sql += getLineSeparator();
        sql += DB2RunstatsOptions.getOption(settings.getColumnStat()).getDdlString();
        if (!DB2RunstatsOptions.getOption(settings.getIndexStat()).getDdlString().equals("")) { //$NON-NLS-1$
            sql += " " + DB2RunstatsOptions.getOption(settings.getIndexStat()).getDdlString(); //$NON-NLS-1$
        }
        if (settings.isTableSampling()) {
            sql += String.format(" TABLESAMPLE SYSTEM(%d)", settings.getSamplePercent()); //$NON-NLS-1$
        }
        sql += "')"; //$NON-NLS-1$
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
