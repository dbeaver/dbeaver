package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEvent;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MySQLEventManager extends SQLObjectEditor<MySQLEvent, MySQLCatalog> {

    public DBSObjectCache<MySQLCatalog, MySQLEvent> getObjectsCache(MySQLEvent object) {
        return object.getCatalog().getEventCache();
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    protected MySQLEvent createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) {
        return new MySQLEvent((MySQLCatalog) container, "NewEvent");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectCreateCommand command, Map<String, Object> options) {
        final MySQLEvent event = command.getObject();
        final StringBuilder script = new StringBuilder();
        try {
            script.append(event.getObjectDefinitionText(monitor, options));
        } catch (DBException e) {
            log.error(e);
        }

        makeEventActions(actions, event, false, script.toString());
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        final MySQLEvent event = command.getObject();
        final StringBuilder script = new StringBuilder();
        options = new LinkedHashMap<>(options);
        options.put(DBPScriptObject.OPTION_OBJECT_ALTER, true);
        try {
            script.append(event.getObjectDefinitionText(monitor, options));
        } catch (DBException e) {
            log.error(e);
        }
        String ddlText = script.toString();
        if (ddlText.startsWith("CREATE ") || ddlText.startsWith("create ")) {
            ddlText = "ALTER " + ddlText.substring(7);
        }

        makeEventActions(actionList, event, true, ddlText);
    }

    private void makeEventActions(List<DBEPersistAction> actionList, MySQLEvent event, boolean alter, String ddlText) {
        MySQLCatalog curCatalog = event.getCatalog().getDataSource().getDefaultDatabase();
        if (curCatalog != event.getCatalog()) {
            actionList.add(new SQLDatabasePersistAction("Set current schema ", "USE " + DBUtils.getQuotedIdentifier(event.getCatalog()), false)); //$NON-NLS-2$
        }
        actionList.add(new SQLDatabasePersistAction(alter ? "Alter event" : "Create event", ddlText)); // $NON-NLS-2$
        if (curCatalog != null && curCatalog != event.getCatalog()) {
            actionList.add(new SQLDatabasePersistAction("Set current schema ", "USE " + DBUtils.getQuotedIdentifier(curCatalog), false)); //$NON-NLS-2$
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Drop event", "DROP EVENT " + DBUtils.getQuotedIdentifier(command.getObject())));

    }

}
