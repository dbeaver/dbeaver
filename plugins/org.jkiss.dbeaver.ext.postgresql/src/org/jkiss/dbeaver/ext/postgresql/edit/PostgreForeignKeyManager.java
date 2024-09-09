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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeignKey;
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

import java.util.List;
import java.util.Map;

/**
 * Postgre foreign key manager
 */
public class PostgreForeignKeyManager extends SQLForeignKeyManager<PostgreTableForeignKey, PostgreTableBase> implements DBEObjectRenamer<PostgreTableForeignKey> {

    @Override
    public boolean canRenameObject(PostgreTableForeignKey object) {
        return object.getDataSource().getServerType().supportsKeyAndIndexRename();
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTableForeignKey> getObjectsCache(PostgreTableForeignKey object)
    {
        final PostgreTableBase parent = object.getParentObject();
        if (parent instanceof PostgreTable) {
            return ((PostgreTable) parent).getForeignKeyCache();
        }
        return null;
    }

    @Override
    protected PostgreTableForeignKey createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object from, @NotNull Map<String, Object> options)
    {
        PostgreTableBase table = (PostgreTableBase) container;
        final PostgreTableForeignKey foreignKey = new PostgreTableForeignKey(
            table,
            null,
            DBSForeignKeyModifyRule.NO_ACTION,
            DBSForeignKeyModifyRule.NO_ACTION);
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }

    @Override
    public StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, PostgreTableBase owner, DBECommandAbstract<PostgreTableForeignKey> command, Map<String, Object> options) {
        PostgreTableForeignKey fk = command.getObject();
        /*if (fk.isPersisted()) {
            try {
                String constrDDL = fk.getObjectDefinitionText(
                    monitor,
                    Collections.singletonMap(DBPScriptObject.OPTION_EMBEDDED_SOURCE, true));
                if (!CommonUtils.isEmpty(constrDDL)) {
                    return new StringBuilder(constrDDL);
                }
            } catch (DBException e) {
                log.warn("Can't extract FK DDL", e);
            }
        }*/
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
    protected void appendUpdateDeleteRule(PostgreTableForeignKey foreignKey, StringBuilder decl) {
        if (foreignKey.getMatchType().equals(PostgreTableForeignKey.MatchType.f)) {
            //Foreign key match types: f = full, p = partial (not implemented yet), s = simple (u == s in old PG versions - default value)
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
    protected String getDropForeignKeyPattern(PostgreTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreTableForeignKey object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        PostgreTableForeignKey foreignKey = command.getObject();
        PostgreDataSource dataSource = foreignKey.getDataSource();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename constraint",
                        "ALTER TABLE " + foreignKey.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                                " RENAME CONSTRAINT " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + //$NON-NLS-1$
                                " TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<PostgreTableForeignKey, PropertyHandler> command, @NotNull Map<String, Object> options) throws DBException {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            PostgreConstraintManager.addConstraintCommentAction(actions, command.getObject());
        }
    }
}
