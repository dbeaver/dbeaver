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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class DB2ToolTableTruncate extends DB2ToolWithStatus<DB2TableBase, DB2ToolTableTruncateSettings>{
    private static final Log log = Log.getLog(DB2ToolTableTruncate.class);

    @NotNull
    @Override
    public DB2ToolTableTruncateSettings createToolSettings() {
        return new DB2ToolTableTruncateSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, DB2ToolTableTruncateSettings settings, List<DBEPersistAction> queries, DB2TableBase object) throws DBCException {
        String sql = "TRUNCATE TABLE";
        sql += " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        sql += " " + settings.getStorageOption();
        sql += " " + settings.getTriggerOption();
//        if (settings.isStorageDropping()) {
//            sql += " DROP STORAGE";
//        } else if (settings.isStorageReusing()) {
//            sql += " REUSE STORAGE";
//        }
//        if (settings.isTriggersDeleting()) {
//            sql += " IGNORE DELETE TRIGGERS";
//        } else if (settings.isTriggersRestricting()) {
//            sql += " RESTRICT WHEN DELETE TRIGGERS";
//        }
        sql += " CONTINUE IDENTITY IMMEDIATE";
        queries.add(new SQLDatabasePersistAction(sql));
    }

}
