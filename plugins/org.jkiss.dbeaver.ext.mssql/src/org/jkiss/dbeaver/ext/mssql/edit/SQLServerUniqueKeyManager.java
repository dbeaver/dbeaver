/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableUniqueKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * SQL server unique constraint manager
 */
public class SQLServerUniqueKeyManager extends SQLConstraintManager<SQLServerTableUniqueKey, SQLServerTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerTableUniqueKey> getObjectsCache(SQLServerTableUniqueKey object)
    {
        return object.getParentObject().getContainer().getUniqueConstraintCache();
    }

    @Override
    protected SQLServerTableUniqueKey createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        SQLServerTable table = (SQLServerTable) container;
        return new SQLServerTableUniqueKey(
            table,
            "PK",
            null,
            DBSEntityConstraintType.INDEX,
            null,
            false);
    }

    @NotNull
    protected String getAddConstraintTypeClause(SQLServerTableUniqueKey constraint) {
        if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
            return "UNIQUE";
        } else {
            return super.getAddConstraintTypeClause(constraint);
        }
    }

}
