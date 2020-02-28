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
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablePhysical;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Map;

/**
 * Oracle index manager
 */
public class OracleIndexManager extends SQLIndexManager<OracleTableIndex, OracleTablePhysical> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleTableIndex> getObjectsCache(OracleTableIndex object)
    {
        return object.getParentObject().getSchema().indexCache;
    }

    @Override
    protected OracleTableIndex createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        OracleTablePhysical table = (OracleTablePhysical) container;

        return new OracleTableIndex(
            table.getSchema(),
            table,
            "INDEX",
            true,
            DBSIndexType.UNKNOWN);
    }

    @Override
    protected String getDropIndexPattern(OracleTableIndex index)
    {
        return "DROP INDEX " + PATTERN_ITEM_INDEX; //$NON-NLS-1$ //$NON-NLS-2$
    }

}
