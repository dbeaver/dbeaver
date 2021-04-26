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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class PostgreToolTableTriggerDisable extends PostgreToolWithStatus<PostgreTrigger, PostgreToolTableTriggerSettings> {
    @Override
    public PostgreToolTableTriggerSettings createToolSettings() {
        return new PostgreToolTableTriggerSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, PostgreToolTableTriggerSettings settings, List<DBEPersistAction> queries, PostgreTrigger object) throws DBCException {
        String sql = "ALTER TABLE " + object.getTable() + " DISABLE TRIGGER " + DBUtils.getQuotedIdentifier(object);
        queries.add(new SQLDatabasePersistAction(sql));
    }

    public boolean needsRefreshOnFinish() {
        return true;
    }
}
