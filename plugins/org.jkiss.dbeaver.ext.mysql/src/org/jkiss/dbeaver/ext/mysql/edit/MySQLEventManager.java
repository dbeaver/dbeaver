package org.jkiss.dbeaver.ext.mysql.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEvent;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class MySQLEventManager extends SQLObjectEditor<MySQLEvent, MySQLCatalog> {

    @Nullable
    @Override
	protected MySQLEvent createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			MySQLCatalog parent, Object copyFrom) throws DBException {
		
		return null;
	}
    
    public DBSObjectCache<MySQLCatalog, MySQLEvent> getObjectsCache(MySQLEvent object)
    {
        return object.getCatalog().getEventCache(); 
    }

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectCreateCommand command, Map<String, Object> options) 
	{
        final MySQLEvent event = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE EVENT " + DBUtils.getQuotedIdentifier(event) + "\n" +
        "ON SCHEDULE AT" + event.getTimeZone() + "INTERVAL" + event.getIntervalValue());

        actions.add(
            new SQLDatabasePersistAction("Create event", event.getEventBody()) //$NON-NLS-2$
        );
	}
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        final MySQLEvent event = command.getObject();
        final StringBuilder script = new StringBuilder("ALTER EVENT " + DBUtils.getQuotedIdentifier(event));

        actionList.add(
            new SQLDatabasePersistAction("Alter event", script.toString()) //$NON-NLS-2$
        );
    }

    
	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(
                new SQLDatabasePersistAction("Drop event", "DROP EVENT " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-2$
            );
		
	}
    private void appendDatabaseModifiers(MySQLEvent event, StringBuilder script) {
        if (event.getTimeZone() != null) {
            script.append("\nON SCHEDULE AT ").append(event.getTimeZone());
        }
        if (event.getIntervalValue() != null) {
            script.append("\nINTERVAL ").append(event.getIntervalValue());
        }
        if (event.getIntervalValue() != null) {
            script.append("\nINTERVAL ").append(event.getIntervalValue());
        }
    }
}
