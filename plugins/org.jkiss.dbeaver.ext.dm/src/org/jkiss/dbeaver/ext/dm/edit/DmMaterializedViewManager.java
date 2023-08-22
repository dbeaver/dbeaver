package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmMaterializedView;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DM MaterializedView Manager
 * 
 * @author caosw
 *
 */
public class DmMaterializedViewManager extends SQLObjectEditor<DmMaterializedView, DmSchema> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	protected void validateObjectProperties(DBRProgressMonitor monitor,ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("View name cannot be empty");
		}
		if (CommonUtils.isEmpty(command.getObject().getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS))) {
			throw new DBException("View definition cannot be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmMaterializedView> getObjectsCache(DmMaterializedView object) {
		return object.getSchema().mviewCache;
	}

	@Override
	protected DmMaterializedView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		DmMaterializedView newView = new DmMaterializedView((DmSchema) container, "NewView");
		newView.setObjectDefinitionText("SELECT 1 FROM DUAL");
		return newView;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmMaterializedView, DmSchema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		createOrReplaceViewQuery(actions, command);
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList,
			SQLObjectEditor<DmMaterializedView, DmSchema>.ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		createOrReplaceViewQuery(actionList, command);
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmMaterializedView, DmSchema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop view",
				"DROP MATERIALIZED VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL))
		);
	}

	private void createOrReplaceViewQuery(List<DBEPersistAction> actions,
			DBECommandComposite<DmMaterializedView, PropertyHandler> command) {
		DmMaterializedView view = command.getObject();
		StringBuilder decl = new StringBuilder(200);
		final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
		boolean hasComment = command.getProperty("comment") != null;
		if (!hasComment || command.getProperties().size() > 1) {
			decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL))
					.append(lineSeparator).append("AS ")
					.append(view.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS));
			if (view.isPersisted()) {
				actions.add(new SQLDatabasePersistAction("Drop view",
						"DROP MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL)));
			}
			actions.add(new SQLDatabasePersistAction("Create view", decl.toString()));
		}
		if (hasComment) {
			actions.add(new SQLDatabasePersistAction("Comment table", "COMMENT ON MATERIALIZED VIEW "
					+ view.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS '" + view.getComment() + "'"));
		}
	}
}
