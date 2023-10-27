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
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableBase;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableForeignKey;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Cubrid foreign manager
 */
public class CubridForeignKeyManager extends SQLForeignKeyManager<CubridTableForeignKey, CubridTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, CubridTableForeignKey> getObjectsCache(CubridTableForeignKey object)
    {
        return object.getParentObject().getContainer().getForeignKeysCache();
    }

    @Override
    public boolean canCreateObject(Object container) {
        return (container instanceof CubridTable)
            && ((CubridTable) container).getDataSource().getInfo().supportsReferentialIntegrity()
            && CubridUtils.canAlterTable((CubridTable) container);
    }

    @Override
    public boolean canEditObject(CubridTableForeignKey object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(CubridTableForeignKey object) {
        return CubridUtils.canAlterTable(object);
    }

    @Override
    protected CubridTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options) {
        CubridTableBase tableBase = (CubridTableBase)container;
        CubridTableForeignKey foreignKey = tableBase.getDataSource().getMetaModel().createTableForeignKeyImpl(
            tableBase,
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
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, CubridTableBase owner, DBECommandAbstract<CubridTableForeignKey> command, Map<String, Object> options) {
        if (!owner.getDataSource().getMetaModel().supportNestedForeignKeys()) {
            return null;
        }
        return super.getNestedDeclaration(monitor, owner, command, options);
    }

    @Override
    protected boolean isLegacyForeignKeySyntax(CubridTableBase owner) {
        return CubridUtils.isLegacySQLDialect(owner);
    }

    @Override
    protected void appendUpdateDeleteRule(CubridTableForeignKey foreignKey, StringBuilder decl) {
        String onDeleteRule = foreignKey.getDataSource().getMetaModel().generateOnDeleteFK(foreignKey.getDeleteRule());
        if (!CommonUtils.isEmpty(onDeleteRule)) {
            decl.append(" ").append(onDeleteRule);
        }

        String onUpdateFK = foreignKey.getDataSource().getMetaModel().generateOnUpdateFK(foreignKey.getUpdateRule());
        if (!CommonUtils.isEmpty(onUpdateFK)) {
            decl.append(" ").append(onUpdateFK);
        }
    }

    @Override
    protected boolean isFKConstraintDuplicated(CubridTableBase owner) {
        return owner.getDataSource().getMetaModel().isFKConstraintWordDuplicated();
    }
}
