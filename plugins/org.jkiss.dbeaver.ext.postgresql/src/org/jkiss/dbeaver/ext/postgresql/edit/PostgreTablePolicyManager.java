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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablePolicy;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PostgreTablePolicyManager
    extends SQLObjectEditor<PostgreTablePolicy, PostgreTable>
    implements DBEObjectRenamer<PostgreTablePolicy> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTablePolicy> getObjectsCache(PostgreTablePolicy object) {
        return object.getParentObject().getPolicyCache();
    }

    @Override
    protected PostgreTablePolicy createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        Object container,
        Object copyFrom,
        @NotNull Map<String, Object> options
    ) throws DBException {
        final PostgreTable table = (PostgreTable) container;
        return new PostgreTablePolicy(
            table,
            DBUtils.makeNewObjectName(monitor, "Policy{0}", table, PostgreTablePolicy.class, PostgreTable::getPolicy, context)
        );
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        final PostgreTablePolicy policy = command.getObject();
        final StringJoiner sql = new StringJoiner("\n\t");

        sql.add("CREATE POLICY " + getNameClause(policy));
        sql.add("AS " + policy.getType());
        sql.add("FOR " + policy.getEvent());

        if (policy.getRole() != null) {
            sql.add("TO " + DBUtils.getQuotedIdentifier(policy.getRole()));
        }

        if (CommonUtils.isNotEmpty(policy.getUsing())) {
            sql.add("USING (" + policy.getUsing() + ")");
        }

        if (CommonUtils.isNotEmpty(policy.getCheck())) {
            sql.add("WITH CHECK (" + policy.getCheck() + ")");
        }

        actions.add(new SQLDatabasePersistAction(
            "Create policy",
            sql.toString()
        ));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        final PostgreTablePolicy policy = command.getObject();
        final StringJoiner sql = new StringJoiner("\n\t");

        sql.add("ALTER POLICY " + getNameClause(policy));

        if (command.hasProperty("role")) {
            sql.add("TO " + (policy.getRole() != null ? DBUtils.getQuotedIdentifier(policy.getRole()) : "PUBLIC"));
        }

        if (command.hasProperty("using")) {
            sql.add("USING (" + policy.getUsing() + ")");
        }

        if (command.hasProperty("check")) {
            sql.add("WITH CHECK (" + policy.getCheck() + ")");
        }

        actions.add(new SQLDatabasePersistAction(
            "Alter policy",
            sql.toString()
        ));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        actions.add(new SQLDatabasePersistAction(
            "Drop policy",
            "DROP POLICY " + getNameClause(command.getObject())
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
        final DBPDataSource dataSource = command.getObject().getDataSource();
        actions.add(new SQLDatabasePersistAction(
            "Rename policy",
            "ALTER POLICY "
                + DBUtils.getQuotedIdentifier(dataSource, command.getOldName())
                + " ON "
                + DBUtils.getQuotedIdentifier(dataSource, command.getObject().getParentObject().getName())
                + " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())
        ));
    }

    @Override
    public void renameObject(
        @NotNull DBECommandContext commandContext,
        @NotNull PostgreTablePolicy object,
        @NotNull Map<String, Object> options,
        @NotNull String newName
    ) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @NotNull
    private static String getNameClause(@NotNull PostgreTablePolicy policy) {
        final DBPDataSource dataSource = policy.getDataSource();
        return DBUtils.getQuotedIdentifier(dataSource, policy.getName())
            + " ON "
            + DBUtils.getObjectFullName(policy.getParentObject(), DBPEvaluationContext.DDL);
    }
}
