/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectStatus;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

import java.util.Map;

/**
 * Oracle foreign key manager
 */
public class OracleForeignKeyManager extends SQLForeignKeyManager<OracleTableForeignKey, OracleTableBase> {


    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableForeignKey> getObjectsCache(OracleTableForeignKey object)
    {
        return object.getParentObject().getSchema().foreignKeyCache;
    }

    @Override
    protected OracleTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
        OracleTableBase table = (OracleTableBase) container;

        return new OracleTableForeignKey(
            table,
            "",
            OracleObjectStatus.ENABLED,
            null,
            DBSForeignKeyModifyRule.NO_ACTION);
    }

}
