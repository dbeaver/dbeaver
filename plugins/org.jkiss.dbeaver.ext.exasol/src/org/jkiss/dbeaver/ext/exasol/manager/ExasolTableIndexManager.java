package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndex;
import org.jkiss.dbeaver.ext.exasol.ui.ExasolIndexConfigurator;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class ExasolTableIndexManager extends SQLIndexManager<ExasolTableIndex, ExasolTable>  {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}
	
	@Nullable
	@Override
	public DBSObjectCache<ExasolSchema, ExasolTableIndex> getObjectsCache(ExasolTableIndex object) {
		return object.getTable().getContainer().getIndexCache();
	}
	
	@Override
	public boolean canEditObject(ExasolTableIndex object) {
		return false;
	}
	
	@Override
	protected ExasolTableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object from, Map<String, Object> options) throws DBException {
		ExasolTableIndex index = new ExasolTableIndex((ExasolTable) container, null,  DBSIndexType.OTHER, false );
		ExasolIndexConfigurator editDialog = new ExasolIndexConfigurator();
		return editDialog.configureObject(monitor, container, index);
	}
	
	
	
	@Override
	protected String getDropIndexPattern(ExasolTableIndex index) {
		return "DROP " + index.getType().getName() + " INDEX ON " + index.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " " + index.getColumnString();
	}
	
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<ExasolTableIndex, ExasolTable>.ObjectCreateCommand command,
			Map<String, Object> options) {
		ExasolTableIndex index = command.getObject();
		String SQL = String.format(
				"ENFORCE %s INDEX ON %s %s",
				index.getType().getName(),
				index.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL),
				index.getColumnString()
				);
		
		actions.add(
				new SQLDatabasePersistAction(
						"Create Index",
						SQL
						)
				); 
		
		super.addObjectCreateActions(monitor, executionContext, actions, command, options);
	}
	
	

}
