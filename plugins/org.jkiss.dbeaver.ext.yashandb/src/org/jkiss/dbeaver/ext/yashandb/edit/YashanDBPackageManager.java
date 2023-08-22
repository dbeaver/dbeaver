package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBPackageManager extends SQLObjectEditor<YashanDBPackage, YashanDBSchema> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBPackage> getObjectsCache(YashanDBPackage object) {
        return object.getSchema().packageCache;
    }

    @Override
    protected YashanDBPackage createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) {
        return new YashanDBPackage(
                (YashanDBSchema) container,
                "NEW_PACKAGE");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options) {
        createOrReplaceProcedureQuery(executionContext, actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options) {
        final YashanDBPackage object = objectDeleteCommand.getObject();
        actions.add(
                new SQLDatabasePersistAction("Drop package",
                        "DROP PACKAGE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL))
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

    private void createOrReplaceProcedureQuery(DBCExecutionContext executionContext, List<DBEPersistAction> actionList, YashanDBPackage pack) {
        try {
            String header = pack.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS).trim();
            if (!header.endsWith(";")) {
                header += ";";
            }
            if (!CommonUtils.isEmpty(header)) {
                actionList.add(
                        new YashanDBObjectValidateAction(
                                pack, YashanDBObjectType.PACKAGE,
                                "Create package header",
                                header));
            }
            String body = pack.getExtendedDefinitionText(new VoidProgressMonitor());
            if (!CommonUtils.isEmpty(body)) {
                body = body.trim();
                if (!body.endsWith(";")) {
                    body += ";";
                }
                actionList.add(
                        new YashanDBObjectValidateAction(
                                pack, YashanDBObjectType.PACKAGE_BODY,
                                "Create package body",
                                body));
            } else {
                actionList.add(
                        new SQLDatabasePersistAction(
                                "Drop package header",
                                "DROP PACKAGE BODY " + pack.getFullyQualifiedName(DBPEvaluationContext.DDL),
                                DBEPersistAction.ActionType.OPTIONAL)
                );
            }
        } catch (DBException e) {
            log.warn(e);
        }
        YashanDBUtils.addSchemaChangeActions(executionContext, actionList, pack);
    }
}
