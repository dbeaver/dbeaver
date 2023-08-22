package org.jkiss.dbeaver.ext.dm.edit;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataType;
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
import org.jkiss.utils.CommonUtils;

/**
 * DM DataTypeManager
 * 
 * @author caosw
 *
 */
public class DmDataTypeManager extends SQLObjectEditor<DmDataType, DmSchema> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmDataType> getObjectsCache(DmDataType object) {
		return object.getSchema().dataTypeCache;
	}

	@Override
	protected DmDataType createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DmSchema schema = (DmSchema) container;
		DmDataType dataType = new DmDataType(schema, "DataType", false);
		dataType.setObjectDefinitionText("TYPE" + dataType.getName() + " AS OBJECT\n" + "(\n" + ")");
		return dataType;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmDataType, DmSchema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		createOrReplaceProcedureQuery(executionContext, actions, command.getObject());
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmDataType, DmSchema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		final DmDataType object = command.getObject();
		actions.add(new SQLDatabasePersistAction("Drop type",
				"DROP TYPE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$
		);
	}

	private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
			DmDataType dataType) {
		String header = DmUtils.normalizeSourceName(dataType, false);
		if (!CommonUtils.isEmpty(header)) {
			actionList.add(new SQLDatabasePersistAction("Create type header", "CREATE OR REPLACE " + header)); //$NON-NLS-2$
		}
		String body = DmUtils.normalizeSourceName(dataType, true);
		if (!CommonUtils.isEmpty(body)) {
			actionList.add(new SQLDatabasePersistAction("Create type body", "CREATE OR REPLACE " + body)); //$NON-NLS-2$
		}
		DmUtils.addSchemaChangeActions(executionContext, actionList, dataType);
	}

	@Override
	public boolean canCreateObject(Object container) {
		 return container instanceof DmSchema;
	}
	
}
