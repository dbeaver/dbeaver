package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmObjectType;
import org.jkiss.dbeaver.ext.dm.model.DmObjectValidateAction;
import org.jkiss.dbeaver.ext.dm.model.DmPackage;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * DM Package Manager
 * @author caosw
 *
 */
public class DmPackageManager extends SQLObjectEditor<DmPackage, DmSchema> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmPackage> getObjectsCache(DmPackage object) {
		return object.getSchema().packageCache;
	}

	@Override
	protected DmPackage createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		return new DmPackage((DmSchema) container, "NEW_PACKAGE");
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
		final DmPackage object = command.getObject();
		actions.add(new SQLDatabasePersistAction("Drop package",
				"DROP PACKAGE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)));
	}
	

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		createOrReplaceProcedureQuery(executionContext, actionList, command.getObject());
	}

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
			DmPackage pack) {
		try {
			String header = pack.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS)
					.trim();
			if (!header.endsWith(";")) {
				header += ";";
			}
			if (!CommonUtils.isEmpty(header)) {
				actionList.add(new DmObjectValidateAction(pack, DmObjectType.PACKAGE, "Create package header", header));
			}
			String body = pack.getExtendedDefinitionText(new VoidProgressMonitor());
			if (!CommonUtils.isEmpty(body)) {
				body = body.trim();
				if (!body.endsWith(";")) {
					body += ";";
				}
				actionList
						.add(new DmObjectValidateAction(pack, DmObjectType.PACKAGE_BODY, "Create package body", body));
			} else {
				actionList.add(new SQLDatabasePersistAction("Drop package header",
						"DROP PACKAGE BODY " + pack.getFullyQualifiedName(DBPEvaluationContext.DDL),
						DBEPersistAction.ActionType.OPTIONAL));
			}
		} catch (Exception e) {
			log.warn(e);
		}
		DmUtils.addSchemaChangeActions(executionContext, actionList, pack);
	}
}
