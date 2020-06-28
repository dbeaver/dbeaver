package org.jkiss.dbeaver.ext.exasol.manager;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTablePartitionColumn;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolTablePartitionColumnCache;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.Map;

public class ExasolTablePartitionColumnManager extends SQLObjectEditor<ExasolTablePartitionColumn, ExasolTable> implements DBEObjectEditor<ExasolTablePartitionColumn>, DBEObjectMaker<ExasolTablePartitionColumn, ExasolTable>  {

    private static final Log LOG = Log.getLog(ExasolTablePartitionColumnManager.class);

	@Override
	public ExasolTablePartitionColumnCache getObjectsCache(
			ExasolTablePartitionColumn object) {
		return object.getTable().getPartitionCache();
	}

	@Override
	public boolean canCreateObject(Object container) {
		return false;
	}

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}
	
	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command,
                                          Map<String, Object> options) throws DBException {
		ExasolTable table = command.getObject().getTable();
		try {
			actionList.add(new SQLDatabasePersistAction(generateAction(monitor, table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}

	@Override
	protected ExasolTablePartitionColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                                              Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		return new ExasolTablePartitionColumn((ExasolTable) container);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command,
                                          Map<String, Object> options) {
		ExasolTable table = command.getObject().getTable();
		try {
			actions.add(new SQLDatabasePersistAction(generateAction(monitor, table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
										  ObjectDeleteCommand command,
										  Map<String, Object> options) {
		ExasolTablePartitionColumn col = command.getObject();
		ExasolTablePartitionColumnCache cache = getObjectsCache(col);
		cache.removeObject(col, false);
		ExasolTable table = command.getObject().getTable();
		try {
			actions.add(new SQLDatabasePersistAction(generateAction(monitor, table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}
	
	private String generateAction(DBRProgressMonitor monitor, ExasolTable table) throws DBException
	{
		if (table.getAdditionalInfo(monitor).getHasPartitionKey(monitor) & table.getPartitions().size() == 0)
		{
			return "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " DROP PARTITION KEYS";
		} 
		if (table.getPartitions().size() > 0)
		{
			if (! table.getAdditionalInfo(monitor).getHasPartitionKey(monitor))
					table.setHasPartitionKey(true, true);
			return ExasolUtils.getPartitionDdl(table, monitor);
		}
			
		return null;
	}



}
