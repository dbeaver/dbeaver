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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class MySQLToolTableTruncate extends MySQLToolWithStatus<MySQLTableBase, MySQLToolTableTruncateSettings> {
    @NotNull
    @Override
    public MySQLToolTableTruncateSettings createToolSettings() {
        return new MySQLToolTableTruncateSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, MySQLToolTableTruncateSettings settings, List<DBEPersistAction> queries, MySQLTableBase object) throws DBCException {
        String sql = "";
        boolean force = settings.isForce();
        if (force) {
            queries.add(new SQLDatabasePersistAction("SET FOREIGN_KEY_CHECKS = 0"));
        }
        sql += "TRUNCATE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        queries.add(new SQLDatabasePersistAction(sql));
        if (force) {
            queries.add(new SQLDatabasePersistAction("SET FOREIGN_KEY_CHECKS = 1"));
        }

    }

    @Override
    public boolean isNeedConfirmation() {
        return true;
    }
}
