package org.jkiss.dbeaver.ext.mysql.edit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLEvent;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

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
	protected MySQLEvent createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			MySQLCatalog parent, Object copyFrom) {
		return new UITask<MySQLEvent>() {
			@Override
			protected MySQLEvent runTask() {
				EntityEditPage editPage = new EntityEditPage(parent.getDataSource(), DBSEntityType.EVENT);
				if (!editPage.edit()) {
					return null;
				}
				return null;
			}
		}.execute();
	}

	@Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions, SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectCreateCommand command, Map<String, Object> options) {
    	final MySQLEvent event = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE EVENT " + DBUtils.getQuotedIdentifier(event));
        addEventOptions(script, event);

        actions.add(
            new SQLDatabasePersistAction("Create event", script.toString()) //$NON-NLS-2$
        );
    }

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList,
			ObjectChangeCommand command, Map<String, Object> options) {
		final MySQLEvent event = command.getObject();
		final StringBuilder script = new StringBuilder("ALTER EVENT " + DBUtils.getQuotedIdentifier(event));
		addEventOptions(script, event);
		
		actionList.add(new SQLDatabasePersistAction("Alter event", script.toString()) // $NON-NLS-2$
		);
	}

	@Override
	protected void addObjectDeleteActions(List<DBEPersistAction> actions,
			SQLObjectEditor<MySQLEvent, MySQLCatalog>.ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction("Drop event",
				"DROP EVENT " + DBUtils.getQuotedIdentifier(command.getObject())) //$NON-NLS-1$
		);

	}

	private void addEventOptions(StringBuilder script, MySQLEvent event) {
		DateFormat dateFormat = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);
		script.append("ON SCHEDULE EVERY '").append(event.getIntervalValue() + " " + event.getIntervalField());
		if (event.getStatus() != null) {
			script.append("STARTS '").append(dateFormat.format(event.getStatus())).append("'\n");
		}
		if (event.getEnds() != null) {
			script.append("ENDS '").append(dateFormat.format(event.getEnds())).append("'\n");
		}
		if (!CommonUtils.isEmpty(event.getOnCompletion())) {
			script.append("ON COMPLETION ").append(event.getOnCompletion()).append("\n");
		}

		script.append("ENABLED".equals(event.getStatus()) ? "ENABLE"
				: "DISABLED".equals(event.getStatus()) ? "DISABLE" : "DISABLE ON SLAVE").append("\n");

		if (!CommonUtils.isEmpty(event.getDescription())) {
			script.append("COMMENT '").append(SQLUtils.escapeString(event.getDataSource(), event.getDescription()))
					.append("'\n");
		}
		script.append("DO ").append(eventDefinition);
	}

}
