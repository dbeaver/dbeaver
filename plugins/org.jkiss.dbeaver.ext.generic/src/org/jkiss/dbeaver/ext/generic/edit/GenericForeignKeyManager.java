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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

/**
 * Generic foreign manager
 */
public class GenericForeignKeyManager extends SQLForeignKeyManager<GenericTableForeignKey, GenericTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableForeignKey> getObjectsCache(GenericTableForeignKey object)
    {
        return object.getParentObject().getContainer().getForeignKeysCache();
    }

    @Override
    public boolean canCreateObject(GenericTable parent) {
        return parent.getDataSource().getSQLDialect().supportsAlterTableConstraint();
    }

    @Override
    public boolean canDeleteObject(GenericTableForeignKey object) {
        return object.getDataSource().getSQLDialect().supportsAlterTableConstraint();
    }

    @Override
    protected GenericTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final GenericTable table, Object from) {
        GenericTableForeignKey foreignKey = new GenericTableForeignKey(
            table,
            null,
            null,
            null,
            DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyDeferability.NOT_DEFERRABLE,
            false);
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }

    @Override
    protected boolean isLegacyForeignKeySyntax(GenericTable owner) {
        return GenericUtils.isLegacySQLDialect(owner);
    }
}
