/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Generic foreign manager
 */
public class GenericForeignKeyManager extends SQLForeignKeyManager<GenericTableForeignKey, GenericTableBase> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableForeignKey> getObjectsCache(GenericTableForeignKey object)
    {
        return object.getParentObject().getContainer().getForeignKeysCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return (container instanceof GenericTable)
            && ((GenericTable) container).getDataSource().getInfo().supportsReferentialIntegrity()
            && GenericUtils.canAlterTable((GenericTable) container);
    }

    @Override
    public boolean canEditObject(GenericTableForeignKey object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    public boolean canDeleteObject(@NotNull GenericTableForeignKey object) {
        return GenericUtils.canAlterTable(object);
    }

    @Override
    protected GenericTableForeignKey createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object from, @NotNull Map<String, Object> options) {
        GenericTableBase tableBase = (GenericTableBase)container;
        GenericTableForeignKey foreignKey = tableBase.getDataSource().getMetaModel().createTableForeignKeyImpl(
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
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, GenericTableBase owner, DBECommandAbstract<GenericTableForeignKey> command, Map<String, Object> options) {
        if (options.get(DBPScriptObject.OPTION_COMPOSITE_OBJECT) instanceof DBSEntity &&
            !owner.getDataSource().getMetaModel().supportNestedForeignKeys()
        ) {
            return null;
        }
        return super.getNestedDeclaration(monitor, owner, command, options);
    }

    @Override
    protected boolean isLegacyForeignKeySyntax(GenericTableBase owner) {
        return GenericUtils.isLegacySQLDialect(owner);
    }

    @Override
    protected void appendUpdateDeleteRule(GenericTableForeignKey foreignKey, StringBuilder decl) {
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
    protected boolean isFKConstraintDuplicated(GenericTableBase owner) {
        return owner.getDataSource().getMetaModel().isFKConstraintWordDuplicated();
    }
}
