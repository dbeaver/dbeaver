package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/24 16:50
 */
public class YashanDBSchedulerJobManager extends SQLObjectEditor<YashanDBSchedulerJob, YashanDBSchema> {
    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("SchedulerJob name cannot be empty");
        }
    }

    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBSchedulerJob> getObjectsCache(YashanDBSchedulerJob object) {
        return object.getSchema().schedulerJobCache;
    }

    @Override
    protected YashanDBSchedulerJob createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        YashanDBSchema schema = (YashanDBSchema) container;
        return new YashanDBSchedulerJob(schema, "NEW_SCHEDULER_JOB");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBSchedulerJob, YashanDBSchema>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        YashanDBSchema schema=command.getObject().getSchema();
        if(schema!=null) {
            actions.add(0, new SQLDatabasePersistAction(
                    "Set target schema",
                    "ALTER SESSION SET CURRENT_SCHEMA=" + schema.getName(),
                    DBEPersistAction.ActionType.INITIALIZER));
        }
        actions.add(new SQLDatabasePersistAction("CREATE Scheduler Job",
                command.getObject().getObjectDefinitionText(monitor,options)));
        if(schema!=null) {
            YashanDBSchema defaultSchema = ((YashanDBExecutionContext) executionContext).getDefaultSchema();
            if (schema != defaultSchema && defaultSchema != null) {
                actions.add(new SQLDatabasePersistAction(
                        "Set current schema",
                        "ALTER SESSION SET CURRENT_SCHEMA=" + defaultSchema.getName(),
                        DBEPersistAction.ActionType.FINALIZER));
            }
        }

    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBSchedulerJob, YashanDBSchema>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        YashanDBSchema schema=command.getObject().getSchema();
        if(schema!=null) {
            actions.add(0, new SQLDatabasePersistAction(
                    "Set target schema",
                    "ALTER SESSION SET CURRENT_SCHEMA=" + schema.getName(),
                    DBEPersistAction.ActionType.INITIALIZER));
        }
        actions.add(new SQLDatabasePersistAction("Drop Scheduler Job",
                "BEGIN\n" +
                        "\tdbms_scheduler.drop_job(\n" +
                        "\t '"+command.getObject().getName()+"'\n" +
                        "\t);\n" +
                        "END;"));
        if(schema!=null) {
            YashanDBSchema defaultSchema = ((YashanDBExecutionContext) executionContext).getDefaultSchema();
            if (schema != defaultSchema && defaultSchema != null) {
                actions.add(new SQLDatabasePersistAction(
                        "Set current schema",
                        "ALTER SESSION SET CURRENT_SCHEMA=" + defaultSchema.getName(),
                        DBEPersistAction.ActionType.FINALIZER));
            }
        }
    }


}
