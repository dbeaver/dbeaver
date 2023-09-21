package org.jkiss.dbeaver.ext.yashandb.edit;


import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSequence;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSynonym;
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
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class YashanDBSynonymManager extends SQLObjectEditor<YashanDBSynonym, YashanDBSchema> {
    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Synonym name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBSynonym> getObjectsCache(YashanDBSynonym object) {
        return object.getSchema().synonymCache;
    }

    @Override
    protected YashanDBSynonym createDatabaseObject(
            DBRProgressMonitor monitor, DBECommandContext context,
            final Object container,
            Object copyFrom, Map<String, Object> options) {
        YashanDBSchema schema = (YashanDBSchema) container;
        return new YashanDBSynonym(schema, "NEW_SYNONYM");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        String sql = command.getObject().buildStatement();
        actions.add(new SQLDatabasePersistAction("Create Synonym", sql));
    }


    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        String sql = "DROP " + (command.getObject().getIsPublic() ? "PUBLIC" : "") + " SYNONYM " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
        DBEPersistAction action = new SQLDatabasePersistAction("Drop Synonym", sql);
        actions.add(action);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBSynonym, YashanDBSchema>.ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        String sql = command.getObject().buildStatement();
        actions.add(new SQLDatabasePersistAction("Modify Synonym", sql));
    }
}
