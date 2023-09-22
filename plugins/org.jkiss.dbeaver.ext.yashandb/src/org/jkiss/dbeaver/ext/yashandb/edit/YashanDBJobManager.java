package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBJob;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchedulerJob;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class YashanDBJobManager extends SQLObjectEditor<YashanDBJob, YashanDBSchema> {
    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return 0;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBJob> getObjectsCache(YashanDBJob object) {
        return object.getSchema().jobCache;
    }

    @Override
    protected YashanDBJob createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBJob, YashanDBSchema>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {

    }


    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBJob, YashanDBSchema>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction("Drop Job","EXEC DBMS_SCHEDULER.DROP_JOB('"+ command.getObject().getSchemaUser()+"."+command.getObject().getName() +"')"));
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        actionList.add(new SQLDatabasePersistAction("Modify Job","EXEC DBMS_SCHEDULER.SET_ATTRIBUTE('"+ command.getObject().getSchemaUser()+"."+command.getObject().getName() +"','job_action','"+ command.getProperties().get("objectDefinitionText") + "')"));
    }

}
