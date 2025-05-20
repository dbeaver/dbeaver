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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSequence;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Postgre sequence manager
 */
public class PostgreSequenceManager extends SQLObjectEditor<PostgreTableBase, PostgreSchema> implements DBEObjectRenamer<PostgreTableBase> {

    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTableBase> getObjectsCache(PostgreTableBase object) {
        return object.getContainer().getSchema().getTableCache();
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Sequence name cannot be empty");
        }
    }

    @Override
    protected PostgreSequence createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, final Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        PostgreSchema schema = (PostgreSchema) container;
        return schema.getDataSource().getServerType().createSequence(schema);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction(
            "Create sequence",
            "CREATE SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)
        ));
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        final PostgreSequence sequence = (PostgreSequence) command.getObject();
        final String sequenceName = sequence.getFullyQualifiedName(DBPEvaluationContext.DDL);
        final StringBuilder sequenceOptions = new StringBuilder();
        addSequenceOptions(sequenceOptions, command.getProperties());

        if (sequenceOptions.length() > 0) {
            actions.add(new SQLDatabasePersistAction(
                "Alter sequence",
                "ALTER SEQUENCE " + sequenceName + sequenceOptions
            ));
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop sequence", "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void addSequenceOptions(StringBuilder ddl, Map<Object, Object> options) {
        if (options.containsKey("incrementBy")) {
            ddl.append("\n\tINCREMENT BY ").append(options.get("incrementBy"));
        }
        if (options.containsKey("minValue")) {
            ddl.append("\n\tMINVALUE ").append(options.get("minValue"));
        }
        if (options.containsKey("maxValue")) {
            ddl.append("\n\tMAXVALUE ").append(options.get("maxValue"));
        }
        if (options.containsKey("startValue")) {
            ddl.append("\n\tSTART ").append(options.get("startValue"));
        }
        if (options.get("lastValue") != null) {
            ddl.append("\n\tRESTART ").append(options.get("lastValue"));
        }
        if (options.containsKey("cacheValue")) {
            ddl.append("\n\tCACHE ").append(options.get("cacheValue"));
        }
        if (options.containsKey("cycled")) {
            ddl.append("\n\t");
            if (!CommonUtils.toBoolean(options.get("cycled"))) {
                ddl.append("NO ");
            }
            ddl.append("CYCLE");
        }
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        actions.add(
            new SQLDatabasePersistAction("Rename sequence",
                "ALTER SEQUENCE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) +
                    " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreTableBase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, options, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<PostgreTableBase, PropertyHandler> command, @NotNull Map<String, Object> options) {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment sequence",
                "COMMENT ON SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(command.getObject(), CommonUtils.notEmpty(command.getObject().getDescription()))));
        }
    }

}
