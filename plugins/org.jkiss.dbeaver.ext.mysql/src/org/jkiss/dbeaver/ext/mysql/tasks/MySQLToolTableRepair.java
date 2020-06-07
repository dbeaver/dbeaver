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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;

import java.util.List;

public class MySQLToolTableRepair extends SQLToolExecuteHandler<MySQLTableBase, MySQLToolTableRepairSettings> {
    @Override
    public MySQLToolTableRepairSettings createToolSettings() {
        return new MySQLToolTableRepairSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, MySQLToolTableRepairSettings settings, List<DBEPersistAction> queries, MySQLTableBase object) throws DBCException {
        String sql = "REPAIR TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        boolean isQuick = settings.isQuick();
        boolean isExtended = settings.isExtended();
        boolean useFRM = settings.isUseFRM();
        if (isQuick) sql += " QUICK";
        if (isExtended) sql += " EXTENDED";
        if (useFRM) sql += " USE_FRM";
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
