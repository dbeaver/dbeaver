package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmObjectType;
import org.jkiss.dbeaver.ext.dm.model.DmObjectValidateAction;
import org.jkiss.dbeaver.ext.dm.model.DmProcedureStandalone;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * DM Procedure Manager
 * 
 * @author caosw
 *
 */
public class DmProcedureManager extends SQLObjectEditor<DmProcedureStandalone, DmSchema> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmProcedureStandalone> getObjectsCache(DmProcedureStandalone object) {
		return object.getSchema().proceduresCache;
	}

	@Override
	protected DmProcedureStandalone createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		return new DmProcedureStandalone((DmSchema) container, "NEW_PROCEDURE", DBSProcedureType.PROCEDURE);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
			throws DBException {
		createOrReplaceProcedureQuery(executionContext, actions, command.getObject());
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		    DmProcedureStandalone object = command.getObject();
		actions.add(new SQLDatabasePersistAction("Drop procedure", "DROP " + object.getProcedureType().name() + " "
				+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)));
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		createOrReplaceProcedureQuery(executionContext, actionList, command.getObject());
	}
	
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
			DmProcedureStandalone procedure) {
		String source = DmUtils.normalizeSourceName(procedure, false);
		if (source == null) {
			return;
		}
		actionList.add(new DmObjectValidateAction(procedure, DmObjectType.PROCEDURE, "Create procedure", source));
		DmUtils.addSchemaChangeActions(executionContext, actionList, procedure);
	}
}
