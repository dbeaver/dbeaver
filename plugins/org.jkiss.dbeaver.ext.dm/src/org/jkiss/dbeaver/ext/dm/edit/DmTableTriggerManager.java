package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.ext.dm.model.DmTableTrigger;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class DmTableTriggerManager extends SQLTriggerManager<DmTableTrigger, DmTableBase> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmTableTrigger> getObjectsCache(DmTableTrigger object) {
		return object.getTable().triggerCache;
	}

	@Override
	public boolean canCreateObject(Object container) {
		return container instanceof DmTableBase;
	}

	@Override
	protected DmTableTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		DmTableBase table = (DmTableBase) container;
		return new DmTableTrigger(table, "NEW_TRIGGER");
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop trigger",
				"DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)));
	}

	@Override
	protected void createOrReplaceTriggerQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DmTableTrigger trigger, boolean create) {
		String source = DmUtils.normalizeSourceName(trigger, false);
		if (source == null) {
			return;
		}
		String script = source;
		if(!script.toUpperCase(Locale.ENGLISH).trim().contains("CREATE ")) {
			script = "CREATE OR REPLACE " + script;
		}
		actions.add(new SQLDatabasePersistAction("Create trigger", script, true));
		DmUtils.addSchemaChangeActions(executionContext, actions, trigger);
	}

}
