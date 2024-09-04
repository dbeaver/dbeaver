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
package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKey;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class ExasolForeignKeyManager
    extends SQLForeignKeyManager<ExasolTableForeignKey, ExasolTable> implements DBEObjectRenamer<ExasolTableForeignKey> {

    @Override
    public DBSObjectCache<? extends DBSObject, ExasolTableForeignKey> getObjectsCache(
        ExasolTableForeignKey object) {
        final ExasolTable parent = object.getParentObject();
        return parent.getContainer().getAssociationCache();
    }

    @Override
    protected ExasolTableForeignKey createDatabaseObject(
        @NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context,
        Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {

        ExasolTable table = (ExasolTable) container;
        final ExasolTableForeignKey foreignKey = new ExasolTableForeignKey(
            table,
            null,
            true,
            "FK"
        );
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }

    @Override
    protected String getDropForeignKeyPattern(ExasolTableForeignKey constraint) {
        return "ALTER TABLE " + DBUtils.getObjectFullName(constraint.getTable(), DBPEvaluationContext.DDL) + " DROP CONSTRAINT "
            + DBUtils.getQuotedIdentifier(constraint)
            ;
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        final ExasolTableForeignKey key = command.getObject();

        try {
            actions.add(new SQLDatabasePersistAction("Create Foreign Key", ExasolUtils.getFKDdl(key, monitor)));
        } catch (DBException e) {
            log.error("Could not created DDL for Exasol FK: " + key.getFullyQualifiedName(DBPEvaluationContext.DDL));
            log.error(e.getMessage());
        }


    }


    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        final ExasolTableForeignKey key = command.getObject();

        actions.add(
            new SQLDatabasePersistAction(
                "Rename FK",
                "ALTER TABLE " + DBUtils.getObjectFullName(key.getTable(), DBPEvaluationContext.DDL) + " RENAME CONSTRAINT "
                    + DBUtils.getQuotedIdentifier(key.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(key.getDataSource(), command.getNewName())
            )
        );
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList,
                                          @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        final ExasolTableForeignKey constraint = command.getObject();

        if (command.getProperties().containsKey(DBConstants.PROP_ID_ENABLED)) {
            actionList.add(
                new SQLDatabasePersistAction("Alter FK",
                    "ALTER TABLE " + constraint.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                        " MODIFY CONSTRAINT " + constraint.getName() + " " +
                        (constraint.getEnabled() ? ExasolConstants.KEYWORD_ENABLE : ExasolConstants.KEYWORD_DISABLE)
                )
            );
        }
    }

    @Override
    protected void processObjectRename(DBECommandContext commandContext, ExasolTableForeignKey object, Map<String, Object> options, String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, options, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }


    @Override
    public void renameObject(@NotNull DBECommandContext commandContext,
                             @NotNull ExasolTableForeignKey object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

}
