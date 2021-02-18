package org.jkiss.dbeaver.ext.postgresql.tasks;
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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreMaterializedView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class PostgreToolMViewRefresh extends PostgreToolWithStatus<PostgreMaterializedView, PostgreToolMViewRefreshSettings>  {
    @NotNull
    @Override
    public PostgreToolMViewRefreshSettings createToolSettings() {
        return new PostgreToolMViewRefreshSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, PostgreToolMViewRefreshSettings settings, List<DBEPersistAction> queries, PostgreMaterializedView object) throws DBCException {
        String sql = "REFRESH MATERIALIZED VIEW " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ";
        boolean isWithData = settings.isWithData();
        if (isWithData) {
            sql += "WITH DATA";
        }
        else {
            sql += "WITH NO DATA";
        }
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
