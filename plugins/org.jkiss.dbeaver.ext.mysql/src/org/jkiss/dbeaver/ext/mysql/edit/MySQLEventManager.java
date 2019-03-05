package org.jkiss.dbeaver.ext.mysql.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEvent;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

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
    protected MySQLEvent createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, MySQLCatalog parent, Object copyFrom) {
        return new UITask<MySQLEvent>() {
            @Override
            protected MySQLEvent runTask() {
                EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.EVENT);
                if (!editPage.edit()) {
                    return null;
                    }
                MySQLEvent newEvent = new MySQLEvent(parent, editPage.getEntityName());
                newEvent.setObjectDefinitionText("SELECT 1");
                return newEvent;
                }
            }.execute();
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

        actions.add(new SQLDatabasePersistAction("Create event", script.toString())); // $NON-NLS-2$

    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction("Drop event", "DROP EVENT " + DBUtils.getQuotedIdentifier(command.getObject())));

    }

}
