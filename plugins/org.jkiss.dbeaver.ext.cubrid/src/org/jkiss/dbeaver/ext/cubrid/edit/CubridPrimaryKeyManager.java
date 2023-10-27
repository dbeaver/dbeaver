/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

/**
 * Cubrid constraint manager
 */
public class CubridPrimaryKeyManager extends SQLConstraintManager<CubridUniqueKey, CubridTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CubridUniqueKey> getObjectsCache(CubridUniqueKey object)
    {
        return object.getParentObject().getContainer().getConstraintKeysCache();
    }

    @Override
    public boolean canCreateObject(Object container) {
        return (container instanceof CubridTable)
            && (!(((CubridTable) container).getDataSource().getInfo() instanceof CubridDataSourceInfo) || ((CubridDataSourceInfo) ((CubridTable) container).getDataSource().getInfo()).supportsTableConstraints())
            && CubridUtils.canAlterTable((CubridTable) container);
    }

    @Override
    public boolean canEditObject(CubridUniqueKey object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(CubridUniqueKey object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    protected CubridUniqueKey createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        CubridTableBase tableBase = (CubridTableBase)container;
        return tableBase.getDataSource().getMetaModel().createConstraintImpl(
            tableBase,
            CubridConstants.BASE_CONSTRAINT_NAME,
            DBSEntityConstraintType.PRIMARY_KEY,
            null,
            false);
    }

    @Override
    protected boolean isLegacyConstraintsSyntax(CubridTableBase owner) {
        return CubridUtils.isLegacySQLDialect(owner);
    }
}
