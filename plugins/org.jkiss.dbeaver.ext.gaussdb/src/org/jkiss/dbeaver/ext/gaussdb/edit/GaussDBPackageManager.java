package org.jkiss.dbeaver.ext.gaussdb.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDatabase;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBPackage;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBSchema;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class GaussDBPackageManager extends SQLObjectEditor<GaussDBPackage, GaussDBDatabase> implements
                                   DBEObjectRenamer<GaussDBPackage> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return 1 << 2;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GaussDBPackage> getObjectsCache(GaussDBPackage object) {
        return object.getSchema().packageCache;
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
                             GaussDBPackage object,
                             Map<String, Object> options,
                             String newName) throws DBException {
        ObjectRenameCommand command = new ObjectRenameCommand(object, ModelMessages.model_jdbc_rename_object, options, newName);
        commandContext.addCommand(command, new RenameObjectReflector(), true);
    }

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public boolean canDeleteObject(GaussDBPackage object) {
        return true;
    }

    @Override
    protected GaussDBPackage createDatabaseObject(DBRProgressMonitor monitor,
                                                  DBECommandContext context,
                                                  Object container,
                                                  Object copyFrom,
                                                  Map<String, Object> options) throws DBException {
        GaussDBSchema schema = (GaussDBSchema) container;
        return new GaussDBPackage(schema, monitor, "NewPackage");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          SQLObjectEditor<GaussDBPackage, GaussDBDatabase>.ObjectCreateCommand command,
                                          Map<String, Object> options) throws DBException {
        GaussDBPackage pack = command.getObject();
        createOrReplaceProcedureQuery(actions, pack);

    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command,
                                          Map<String, Object> options) throws DBException {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceProcedureQuery(actionList, command.getObject());
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor,
                                          DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions,
                                          SQLObjectEditor<GaussDBPackage, GaussDBDatabase>.ObjectDeleteCommand command,
                                          Map<String, Object> options) throws DBException {

        GaussDBPackage pack = command.getObject();
        actions.add(new SQLDatabasePersistAction("Drop package", "DROP PACKAGE " + pack.getName()) //$NON-NLS-2$
        );
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, GaussDBPackage pack) throws DBException {
        String header = pack.getObjectDefinitionText().trim();
        if (!header.endsWith(";")) {
            header += ";";
        }
        if (!CommonUtils.isEmpty(header)) {
            actionList.add(new SQLDatabasePersistAction("Create package header", header)); // $NON-NLS-1$
        }
        String body = pack.getExtendedDefinitionText();
        if (!CommonUtils.isEmpty(body)) {
            body = body.trim();
            if (!body.endsWith(";")) {
                body += ";";
            }
            actionList.add(new SQLDatabasePersistAction("Create package body", body));
        } else {
            actionList.add(new SQLDatabasePersistAction("Drop package header",
                                                        "DROP PACKAGE BODY " + pack.getName(),
                                                        DBEPersistAction.ActionType.OPTIONAL) // $NON-NLS-1$
            );
        }
    }
}
