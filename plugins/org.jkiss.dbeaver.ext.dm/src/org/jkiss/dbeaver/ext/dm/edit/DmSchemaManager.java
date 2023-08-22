package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * DM Schema Manager
 * 
 * @author saorionesan
 *
 */
public class DmSchemaManager extends SQLObjectEditor<DmSchema, DmDataSource> implements DBEObjectRenamer<DmSchema> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmSchema> getObjectsCache(DmSchema object) {
		return object.getDataSource().schemaCache;
	}

	@Override
	protected DmSchema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		return new DmSchema((DmDataSource) container, -1, "NEW_SCHEMA");
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmSchema, DmDataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		DmUser user = command.getObject().getUser();
		String sql = "CREATE USER " + DBUtils.getQuotedIdentifier(user);
		if (!CommonUtils.isEmpty(user.getPassword())) {
			sql += " IDENTIFIED BY \"" + user.getPassword() + "\"";
		}
		actions.add(new SQLDatabasePersistAction("Create schema", sql));
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmSchema, DmDataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop schema",
				"DROP USER " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE"));

	}

	@Override
	public void renameObject(DBECommandContext commandContext, DmSchema object, Map<String, Object> options,
			String newName) throws DBException {
	throw new DBException(
					"Direct database rename is not yet implemented in Dm. You should use export/import functions for that.");
	}

}
