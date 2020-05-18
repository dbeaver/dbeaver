package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriority;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolRole;
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

public class ExasolRoleManager extends SQLObjectEditor<ExasolRole, ExasolDataSource> implements DBEObjectRenamer<ExasolRole> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<ExasolDataSource, ExasolRole> getObjectsCache(
        ExasolRole object) {
        ExasolDataSource ds = (ExasolDataSource) object.getDataSource();
        return ds.getRoleCache();

    }

    @Override
    protected ExasolRole createDatabaseObject(DBRProgressMonitor monitor,
          DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        return new ExasolRole((ExasolDataSource) container, "ROLE", "");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options) {
        ExasolRole obj = command.getObject();

        String script = "CREATE ROLE " + DBUtils.getQuotedIdentifier(obj);

        actions.add(new SQLDatabasePersistAction("Create Role", script));

        if (!CommonUtils.isEmpty(obj.getDescription())) {
            actions.add(Comment(obj));
        }

    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command, Map<String, Object> options) {
        ExasolRole obj = command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop Role", "DROP ROLE " + DBUtils.getQuotedIdentifier(obj)));
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
                             ExasolRole object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void processObjectRename(DBECommandContext commandContext,
                                       ExasolRole object, String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options) {
        ExasolRole obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename ROLE",
                "RENAME ROLE " + DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
    }

    private SQLDatabasePersistAction Comment(ExasolRole obj) {
        return new SQLDatabasePersistAction("Comment on Role", "COMMENT ON ROLE " + DBUtils.getQuotedIdentifier(obj) + " IS '" + ExasolUtils.quoteString(obj.getDescription()) + "'");
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command, Map<String, Object> options) {
        ExasolRole obj = command.getObject();
        ExasolPriority priority = obj.getPriority();

        if (command.getProperties().containsKey("description")) {

            actionList.add(Comment(obj));
        }

        if (command.getProperties().containsKey("priority")) {
        	String script = "";
        	if (ExasolConstants.CONSUMER_GROUP_CLASS.equals(priority.getClass().getName())) { 
        		script = String.format("ALTER ROLE %s SET CONSUMER_GROUP = %s", DBUtils.getQuotedIdentifier(obj), DBUtils.getQuotedIdentifier(priority));
                actionList.add(new SQLDatabasePersistAction(ExasolMessages.manager_assign_priority_group, script));
        	}
        	else if (ExasolConstants.PRIORITY_GROUP_CLASS.equals(priority.getClass().getName())) {
        		script = String.format("GRANT PRIORITY GROUP %s to %s", DBUtils.getQuotedIdentifier(priority), DBUtils.getQuotedIdentifier(obj));
                actionList.add(new SQLDatabasePersistAction(ExasolMessages.manager_assign_priority_group, script));
			} 
        }


    }

}