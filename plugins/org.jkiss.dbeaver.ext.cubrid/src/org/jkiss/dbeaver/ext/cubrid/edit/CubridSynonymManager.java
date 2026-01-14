/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.ext.cubrid.model.CubridSynonym;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUser;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class CubridSynonymManager extends SQLObjectEditor<GenericSynonym, GenericStructContainer>
    implements DBEObjectRenamer<GenericSynonym> {

    public static final String BASE_SYNONYM_NAME = "new_synonym";

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GenericSynonym> getObjectsCache(GenericSynonym object) {
        DBSObject parentObject = object.getParentObject();
        if (parentObject instanceof GenericObjectContainer container) {
            return container.getSynonymCache();
        }
        return null;
    }

    @Override
    protected CubridSynonym createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @Nullable Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new CubridSynonym((GenericStructContainer) container, BASE_SYNONYM_NAME);
    }

    public String buildStatement(
        @NotNull NestedObjectCommand command,
        boolean isCreate
    ) {
        StringBuilder query = new StringBuilder();
        CubridSynonym synonym = (CubridSynonym) command.getObject();
        query.append(isCreate ? "CREATE SYNONYM " : "ALTER SYNONYM ");
        query.append(synonym.getFullyQualifiedName(DBPEvaluationContext.DDL));
        query.append(" FOR ").append(DBUtils.getQuotedIdentifier(synonym.getDataSource(), CommonUtils.notEmpty(synonym.getTargetObject())));
        if ((!synonym.isPersisted() && synonym.getDescription() != null) || command.hasProperty("description")) {
            query.append(" COMMENT ").append(SQLUtils.quoteString(synonym, CommonUtils.notEmpty(synonym.getDescription())));
        }
        return query.toString();
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create Synonym", buildStatement(command, true)));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Modify Serial", buildStatement(command, false)));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        CubridSynonym synonym = (CubridSynonym) command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Drop Synonym",
            "DROP SYNONYM " + synonym.getFullyQualifiedName(DBPEvaluationContext.DDL)
        ));
    }

    @Override
    protected void addObjectRenameActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectRenameCommand command,
        @NotNull Map<String, Object> options
    ) {
        CubridSynonym synonym = (CubridSynonym) command.getObject();
        String schemaName = DBUtils.getQuotedIdentifier(synonym.getOwner()) + ".";
        actions.add(new SQLDatabasePersistAction(
            "Rename Synonym",
            "RENAME SYNONYM " + schemaName + DBUtils.getQuotedIdentifier(synonym.getDataSource(), command.getOldName())
            + " TO " + schemaName + DBUtils.getQuotedIdentifier(synonym.getDataSource(), command.getNewName())
        ));
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull GenericSynonym object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        if (!((CubridDataSource) object.getDataSource()).isShard()) {
            processObjectRename(commandContext, object, options, newName);
        }
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        CubridUser user = (CubridUser) container;
        CubridDataSource dataSource = (CubridDataSource) user.getDataSource();
        return !dataSource.isShard();
    }

    @Override
    public boolean canEditObject(GenericSynonym object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }

    @Override
    public boolean canDeleteObject(GenericSynonym object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }

    @Override
    public boolean canRenameObject(GenericSynonym object) {
        return !((CubridDataSource) object.getDataSource()).isShard();
    }
}
