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
package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTable;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableForeignKey;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

/**
 * Kingbase foreign key manager
 */
public class KingbaseForeignKeyManager extends SQLForeignKeyManager<KingbaseTableForeignKey, KingbaseTableBase> implements DBEObjectRenamer<KingbaseTableForeignKey> {

    @Override
    public boolean canRenameObject(KingbaseTableForeignKey object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, KingbaseTableForeignKey> getObjectsCache(KingbaseTableForeignKey object)
    {
        final KingbaseTableBase parent = object.getParentObject();
        if (parent instanceof KingbaseTable) {
            return ((KingbaseTable) parent).getForeignKeyCache();
        }
        return null;
    }

    @Override
    protected KingbaseTableForeignKey createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object from, @NotNull Map<String, Object> options)
    {
        KingbaseTableBase table = (KingbaseTableBase) container;
        final KingbaseTableForeignKey foreignKey = new KingbaseTableForeignKey(
            table,
            null,
            DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyModifyRule.NO_ACTION);
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, KingbaseTableBase owner, DBECommandAbstract<KingbaseTableForeignKey> command, Map<String, Object> options) {
        KingbaseTableForeignKey fk = command.getObject();
        
        StringBuilder sql = super.getNestedDeclaration(monitor, owner, command, options);
        if (fk.isDeferrable()) {
            sql.append(" DEFERRABLE");
        }
        if (fk.isDeferred()) {
            sql.append(" INITIALLY DEFERRED");
        }

        return sql;
    }

    @Override
    protected void appendUpdateDeleteRule(KingbaseTableForeignKey foreignKey, StringBuilder decl) {
        if (foreignKey.getMatchType().equals(KingbaseTableForeignKey.MatchType.f)) {
            decl.append(" MATCH FULL");
        }
        super.appendUpdateDeleteRule(foreignKey, decl);
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException
    {
        addObjectDeleteActions(monitor, executionContext, actionList, new ObjectDeleteCommand(command.getObject(), command.getTitle()), options);
        addObjectCreateActions(monitor, executionContext, actionList, makeCreateCommand(command.getObject(), options), options);
    }

    @Override
    protected String getDropForeignKeyPattern(KingbaseTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseTableForeignKey object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        KingbaseTableForeignKey foreignKey = command.getObject();
        KingbaseDataSource dataSource = foreignKey.getDataSource();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename constraint",
                        "ALTER TABLE " + foreignKey.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                                " RENAME CONSTRAINT " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + //$NON-NLS-1$
                                " TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<KingbaseTableForeignKey, PropertyHandler> command, @NotNull Map<String, Object> options) throws DBException {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            KingbaseConstraintManager.addConstraintCommentAction(actions, command.getObject());
        }
    }
}
