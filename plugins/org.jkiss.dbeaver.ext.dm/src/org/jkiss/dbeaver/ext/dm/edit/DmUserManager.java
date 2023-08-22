package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class DmUserManager extends SQLObjectEditor<DmUser, DmDataSource> implements DBEObjectRenamer<DmUser> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return 0;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, DmUser> getObjectsCache(DmUser object) {
		return null;
	}


	@Override
	protected DmUser createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		return null;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmUser, DmDataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {		
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmUser, DmDataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
	}

	@Override
	public void renameObject(DBECommandContext commandContext, DmUser object, Map<String, Object> options,
			String newName) throws DBException {
	}

}
