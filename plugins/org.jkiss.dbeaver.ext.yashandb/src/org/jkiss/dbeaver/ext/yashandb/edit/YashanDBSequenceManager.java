package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSequence;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
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
public class YashanDBSequenceManager extends SQLObjectEditor<YashanDBSequence, YashanDBSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Sequence name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBSequence> getObjectsCache(YashanDBSequence object) {
        return object.getSchema().sequenceCache;
    }

    @Override
    protected YashanDBSequence createDatabaseObject(
            DBRProgressMonitor monitor, DBECommandContext context,
            final Object container,
            Object copyFrom, Map<String, Object> options) {
        YashanDBSchema schema = (YashanDBSchema) container;
        return new YashanDBSequence(schema, "NEW_SEQUENCE");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        String sql = command.getObject().buildStatement(false);
        actions.add(new SQLDatabasePersistAction("Create Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            actions.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        String sql = command.getObject().buildStatement(true);
        actionList.add(new SQLDatabasePersistAction("Alter Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            actionList.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        String sql = "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
        DBEPersistAction action = new SQLDatabasePersistAction("Drop Sequence", sql);
        actions.add(action);
    }

    private String buildComment(YashanDBSequence sequence) {
        if (!CommonUtils.isEmpty(sequence.getDescription())) {
            return "COMMENT ON SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS " + SQLUtils.quoteString(sequence, sequence.getDescription());
        }
        return null;
    }
}
