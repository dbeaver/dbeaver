package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * DM View Manager
 * 
 * @author caosw
 *
 */
public class DmViewManager extends SQLObjectEditor<DmView, DmSchema> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	protected void validateObjectProperties(DBRProgressMonitor monitor,SQLObjectEditor<DmView, DmSchema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("View name cannot be empty");
		}
		if (CommonUtils.isEmpty(command.getObject().getViewText())) {
			throw new DBException("View definition cannot be empty");
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, DmView> getObjectsCache(DmView object) {
		return (DBSObjectCache) object.getSchema().tableCache;
	}

	@Override
	protected String getBaseObjectName() {
		return SQLTableManager.BASE_VIEW_NAME;
	}

	@Override
	protected DmView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DmSchema schema = (DmSchema) container;
		DmView newView = new DmView(schema, "NEW_VIEW");
		setNewObjectName(monitor, schema, newView);
		newView.setViewText(
				"CREATE OR REPLACE VIEW " + newView.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\nSELECT");
		return newView;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmView, DmSchema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		createOrReplaceViewQuery(actions, command);
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<DmView, DmSchema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		createOrReplaceViewQuery(actionList, command);
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmView, DmSchema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop view",
				"DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) 
		);
	}

	private void createOrReplaceViewQuery(List<DBEPersistAction> actions,
			DBECommandComposite<DmView, PropertyHandler> command) {
		final DmView view = command.getObject();
		boolean hasComment = command.getProperty("comment") != null;
		if (!hasComment || command.getProperties().size() > 1) {
			String viewText = view.getViewText().trim();
			while (viewText.endsWith(";")) {
				viewText = viewText.substring(0, viewText.length() - 1);
			}
			actions.add(new SQLDatabasePersistAction("Create view", viewText));
		}
		if (hasComment) {
			actions.add(new SQLDatabasePersistAction("Comment table", "COMMENT ON TABLE "
					+ view.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS '" + view.getComment() + "'"));
		}
	}
}
