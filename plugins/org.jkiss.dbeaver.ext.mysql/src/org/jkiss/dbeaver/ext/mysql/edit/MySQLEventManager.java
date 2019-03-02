package org.jkiss.dbeaver.ext.mysql.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEvent;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
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
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
			SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectCreateCommand command, Map<String, Object> options) {
		
	}
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        createOrReplaceEventQuery(actionList, command.getObject());
    }

    
	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(
	            new SQLDatabasePersistAction("Drop event", "DROP " + command.getObject().getEventType() + " " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
	        );
		
	}
    private void createOrReplaceEventQuery(List<DBEPersistAction> actions, MySQLEvent event)
    {
        actions.add(
            new SQLDatabasePersistAction("Drop event", "DROP " + event.getEventType() + " IF EXISTS " + event.getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$ //$NON-NLS-3$
        actions.add(
            new SQLDatabasePersistAction("Create event", event.getDeclaration(), true));
    }
}
