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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeignKey;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
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
    protected PostgreTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
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
        if (fk.isPersisted() && !Boolean.TRUE.equals(options.get(DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES))) { // pg_get_constraintdef returns reference name without schema for initialized default schemas
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
        }
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
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            PostgreConstraintManager.addConstraintCommentAction(actionList, command.getObject());
        }
    }

    @Override
    protected String getDropForeignKeyPattern(PostgreTableForeignKey foreignKey)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP CONSTRAINT " + PATTERN_ITEM_CONSTRAINT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void renameObject(DBECommandContext commandContext, PostgreTableForeignKey object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        PostgreTableForeignKey foreignKey = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename constraint",
                        "ALTER TABLE " + foreignKey.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + //$NON-NLS-1$
                                " RENAME CONSTRAINT " + DBUtils.getQuotedIdentifier(foreignKey) + " TO " + DBUtils.getQuotedIdentifier(foreignKey.getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }
}
