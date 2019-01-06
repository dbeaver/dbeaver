package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;
import java.util.Map;

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
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

public class ExasolTablePartitionColumnManager extends SQLObjectEditor<ExasolTablePartitionColumn, ExasolTable> implements DBEObjectEditor<ExasolTablePartitionColumn>, DBEObjectMaker<ExasolTablePartitionColumn, ExasolTable>  {

    private static final Log LOG = Log.getLog(ExasolTablePartitionColumnManager.class);

	@Override
	public ExasolTablePartitionColumnCache getObjectsCache(
			ExasolTablePartitionColumn object) {
		return ((ExasolTable) object.getTable()).getPartitionCache();
	}

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}
	
	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList,
			SQLObjectEditor<ExasolTablePartitionColumn, ExasolTable>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		ExasolTable table = (ExasolTable) command.getObject().getTable();
		try {
			actionList.add(new SQLDatabasePersistAction(generateAction(table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}

	@Override
	protected ExasolTablePartitionColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			ExasolTable parent, Object copyFrom) throws DBException {
		return new ExasolTablePartitionColumn(parent);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolTablePartitionColumn, ExasolTable>.ObjectCreateCommand command,
			Map<String, Object> options) {
		ExasolTable table = (ExasolTable) command.getObject().getTable();
		try {
			actions.add(new SQLDatabasePersistAction(generateAction(table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}

	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolTablePartitionColumn, ExasolTable>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		ExasolTablePartitionColumn col = command.getObject();
		ExasolTablePartitionColumnCache cache = getObjectsCache(col);
		cache.removeObject(col, false);
		ExasolTable table = (ExasolTable) command.getObject().getTable();
		try {
			actions.add(new SQLDatabasePersistAction(generateAction(table)));
		} catch (DBException e) {
			LOG.error("Failed to create Partition Action", e);
		}
	}
	
	private String generateAction(ExasolTable table) throws DBException
	{
		if (table.getHasDistKey(new VoidProgressMonitor()) & table.getPartitions().size() == 0)
		{
			return "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " DROP PARTITION KEYS";
		} 
		if (table.getPartitions().size() > 0)
		{
			if (! table.getHasPartitionKey(new VoidProgressMonitor()))
				table.setHasPartitionKey(true, true);
			return ExasolUtils.getPartitionDdl(table, new VoidProgressMonitor());
		}
			
		return null;
	}



}
