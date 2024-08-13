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
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.ExasolUserType;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriority;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolUser;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class ExasolUserManager extends SQLObjectEditor<ExasolUser, ExasolDataSource> implements DBEObjectRenamer<ExasolUser> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<ExasolDataSource, ExasolUser> getObjectsCache(
        ExasolUser object) {
        ExasolDataSource ds = (ExasolDataSource) object.getDataSource();
        return ds.getUserCache();

    }

    @Override
    protected ExasolUser createDatabaseObject(@NotNull DBRProgressMonitor monitor,
                                              @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options)
        throws DBException {
    	return new ExasolUser((ExasolDataSource) container, "user", "", "", "password", "", ExasolUserType.LOCAL);
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) {
        ExasolUser obj = command.getObject();

        StringBuilder script = new StringBuilder("CREATE USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED ");

        switch (obj.getType()) {
            case LOCAL:
                script.append(" BY \"" + obj.getPassword() + "\"");
                break;
            case LDAP:
                script.append(" AT LDAP AS '" + obj.getDn() + "'");
                break;
            default:
                script.append(" BY KERBEROS PRINCIPAL '" + obj.getKerberosPrincipal() + "'");
                break;
        }
        actions.add(new SQLDatabasePersistAction("Create User", script.toString()));

        if (!CommonUtils.isEmpty(obj.getDescription())) {
            actions.add(Comment(obj));
        }

    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        ExasolUser obj = command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop User", "DROP USER " + DBUtils.getQuotedIdentifier(obj)));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext,
                             @NotNull ExasolUser object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void processObjectRename(DBECommandContext commandContext,
                                       ExasolUser object, Map<String, Object> options, String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, options, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions,
                                          @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        ExasolUser obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename User",
                "RENAME USER " + DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
    }

    private SQLDatabasePersistAction Comment(ExasolUser obj) {
        return new SQLDatabasePersistAction("Comment on User", "COMMENT ON USER " + DBUtils.getQuotedIdentifier(obj) + " IS '" + ExasolUtils.quoteString(obj.getDescription()) + "'");
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList,
                                          @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        ExasolUser obj = command.getObject();

        if (command.getProperties().containsKey("description")) {

            actionList.add(Comment(obj));
        }

        if (command.getProperties().containsKey("priority")) {
        	
        	ExasolPriority priority = obj.getPriority();
        	
        	if (ExasolConstants.PRIORITY_GROUP_CLASS.equals(priority.getClass().getName())) {
                String script = String.format("GRANT PRIORITY GROUP %s to %s", DBUtils.getQuotedIdentifier(priority), DBUtils.getQuotedIdentifier(obj));
                actionList.add(new SQLDatabasePersistAction(ExasolMessages.manager_assign_priority_group, script));
        	}
        	if (ExasolConstants.CONSUMER_GROUP_CLASS.equals(priority.getClass().getName())) {
                String script = String.format("ALTER USER  %s SET CONSUMER_GROUP = %s", DBUtils.getQuotedIdentifier(obj), DBUtils.getQuotedIdentifier(priority));
                actionList.add(new SQLDatabasePersistAction(ExasolMessages.manager_assign_priority_group, script));
        	}
        }

        if (command.getProperties().containsKey("dn")) {
            String script = String.format("ALTER USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED AT LDAP AS '%s'", obj.getDn());
            actionList.add(new SQLDatabasePersistAction("alter user", script));
            return;
        }

        if (command.getProperties().containsKey("kerberosPrincipal")) {
            String script = String.format("ALTER USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED BY KERBEROS PRINCIPAL '%s'", obj.getKerberosPrincipal());
            actionList.add(new SQLDatabasePersistAction("alter user", script));
            return;
        }

        if (command.getProperties().containsKey("password")) {

            StringBuilder script = new StringBuilder("ALTER USER " + DBUtils.getQuotedIdentifier(obj) + " IDENTIFIED ");
            script.append(" BY \"" + obj.getPassword() + "\" ");
            /*ExasolDataSource ds = (ExasolDataSource) obj.getDataSource();
            if (!ds.hasAlterUserPrivilege()) {
                String oldPassword = new UITask<String>() {
                    @Override
                    protected String runTask() {
                        ExasolUserQueryPassword dialog = new ExasolUserQueryPassword(UIUtils.getActiveWorkbenchShell());

                        if (dialog.open() != IDialogConstants.OK_ID) {
                            throw new IllegalStateException("Password has to be provided");
                        }

                        return dialog.getPassword();
                    }
                }.execute();

                if (CommonUtils.isEmpty(oldPassword)) {
                    throw new IllegalStateException("Old password can not be empty");
                }

                script.append(" REPLACE \"" + oldPassword + "\"");
            }*/

            actionList.add(new SQLDatabasePersistAction("Modify User", script.toString()));
        }
    }

}