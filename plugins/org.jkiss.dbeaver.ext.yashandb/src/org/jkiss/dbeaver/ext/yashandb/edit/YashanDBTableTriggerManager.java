package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableTrigger;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBTableTriggerManager extends SQLTriggerManager<YashanDBTableTrigger, YashanDBTableBase> {
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBTableTrigger> getObjectsCache(YashanDBTableTrigger object) {
        return object.getTable().getSchema().tableTriggerCache;
    }

    @Override
    public boolean canCreateObject(Object container) {
        return container instanceof YashanDBTableBase;
    }

    @Override
    protected YashanDBTableTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) {
        YashanDBTableBase table = (YashanDBTableBase) container;
        return new YashanDBTableTrigger(table, "NEW_TRIGGER");
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(
                new SQLDatabasePersistAction("Drop trigger", "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected void createOrReplaceTriggerQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, YashanDBTableTrigger trigger, boolean create) {
        String source = YashanDBUtils.normalizeSourceName(trigger, false);
        if (source == null) {
            return;
        }
        String script = source;
        if (!script.toUpperCase(Locale.ENGLISH).trim().contains("CREATE ")) {
            script = "CREATE OR REPLACE " + script;
        }
        actions.add(new SQLDatabasePersistAction("Create trigger", script, true)); //$NON-NLS-2$
        YashanDBUtils.addSchemaChangeActions(executionContext, actions, trigger);
    }
}
