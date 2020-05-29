package org.jkiss.dbeaver.ext.exasol.manager;

//import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.ExasolUserType;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriority;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolUser;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
//import org.jkiss.dbeaver.ext.exasol.ui.ExasolUserQueryPassword;
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
//import org.jkiss.dbeaver.ui.UITask;
//import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class ExasolUserManager extends SQLObjectEditor<ExasolUser, ExasolDataSource> implements DBEObjectRenamer<ExasolUser> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<ExasolDataSource, ExasolUser> getObjectsCache(
        ExasolUser object) {
        ExasolDataSource ds = (ExasolDataSource) object.getDataSource();
        return ds.getUserCache();

    }

    @Override
    protected ExasolUser createDatabaseObject(DBRProgressMonitor monitor,
                                              DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
        throws DBException {
    	return new ExasolUser((ExasolDataSource) container, "user", "", "", "password", "", ExasolUserType.LOCAL);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options) {
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
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command, Map<String, Object> options) {
        ExasolUser obj = command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop User", "DROP USER " + DBUtils.getQuotedIdentifier(obj)));
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
                             ExasolUser object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void processObjectRename(DBECommandContext commandContext,
                                       ExasolUser object, String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options) {
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
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command, Map<String, Object> options) {
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