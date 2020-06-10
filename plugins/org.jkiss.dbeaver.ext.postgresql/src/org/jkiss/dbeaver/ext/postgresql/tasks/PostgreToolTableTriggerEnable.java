package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class PostgreToolTableTriggerEnable extends PostgreToolWithStatus<PostgreTrigger, PostgreToolTableTriggerSettings> {
    @Override
    public PostgreToolTableTriggerSettings createToolSettings() {
        return new PostgreToolTableTriggerSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, PostgreToolTableTriggerSettings settings, List<DBEPersistAction> queries, PostgreTrigger object) throws DBCException {
        String sql = "ALTER TABLE " + object.getTable() + " ENABLE TRIGGER " + DBUtils.getQuotedIdentifier(object);
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
