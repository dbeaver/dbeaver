package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.util.List;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBProcedureManager extends SQLObjectEditor<YashanDBProcedureStandalone, YashanDBSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBProcedureStandalone> getObjectsCache(YashanDBProcedureStandalone object) {
        return object.getSchema().proceduresCache;
    }

    @Override
    protected YashanDBProcedureStandalone createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) {
        return new YashanDBProcedureStandalone(
                (YashanDBSchema) container,
                "NEW_PROCEDURE",
                DBSProcedureType.PROCEDURE);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options) {
        createOrReplaceProcedureQuery(executionContext, actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options) {
        final YashanDBProcedureStandalone object = objectDeleteCommand.getObject();
        actions.add(
                new SQLDatabasePersistAction("Drop procedure",
                        "DROP " + object.getProcedureType().name() + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-1$ //$NON-NLS-2$
        );
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand, Map<String, Object> options) {
        createOrReplaceProcedureQuery(executionContext, actionList, objectChangeCommand.getObject());
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList, YashanDBProcedureStandalone procedure) {
        String source = YashanDBUtils.normalizeSourceName(procedure, false);
        if (source == null) {
            return;
        }
        actionList.add(new YashanDBObjectValidateAction(procedure, YashanDBObjectType.PROCEDURE, "Create procedure", source)); //$NON-NLS-2$
        YashanDBUtils.addSchemaChangeActions(executionContext, actionList, procedure);
    }

}
