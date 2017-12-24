package org.jkiss.dbeaver.ext.oracle.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleSequence;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OracleSequenceManager extends SQLObjectEditor<OracleSequence, OracleSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command) throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Sequence name cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, OracleSequence> getObjectsCache(OracleSequence object)
    {
        return object.getSchema().sequenceCache;
    }

    @Override
    protected OracleSequence createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                               final OracleSchema schema,
                                               Object copyFrom)
    {
        return new UITask<OracleSequence>() {
            @Override
            protected OracleSequence runTask() {
                EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.SEQUENCE);
                if (!page.edit()) {
                    return null;
                }

                final OracleSequence sequence = new OracleSequence(schema, page.getEntityName());
                sequence.setIncrementBy(1L);
                sequence.setMinValue(new BigDecimal(0));
                sequence.setCycle(false);
                return sequence;
            }
        }.execute();
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options)
    {
        String sql = buildStatement(command.getObject(), false);
        actions.add(new SQLDatabasePersistAction("Create Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            actions.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
    {
        String sql = buildStatement(command.getObject(), true);
        actionList.add(new SQLDatabasePersistAction("Alter Sequence", sql));

        String comment = buildComment(command.getObject());
        if (comment != null) {
            actionList.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
        }
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        String sql = "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
        DBEPersistAction action = new SQLDatabasePersistAction("Drop Sequence", sql);
        actions.add(action);
    }

    private String buildStatement(OracleSequence sequence, Boolean forUpdate)
    {
        StringBuilder sb = new StringBuilder();
        if (forUpdate) {
            sb.append("ALTER SEQUENCE ");
        } else {
            sb.append("CREATE SEQUENCE ");
        }
        sb.append(sequence.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");

        if (sequence.getIncrementBy() != null) {
            sb.append("INCREMENT BY ").append(sequence.getIncrementBy()).append(" ");
        }
        if (sequence.getMinValue() != null) {
            sb.append("MINVALUE ").append(sequence.getMinValue()).append(" ");
        }
        if (sequence.getMaxValue() != null) {
            sb.append("MAXVALUE ").append(sequence.getMaxValue()).append(" ");
        }

        if (sequence.isCycle()) {
            sb.append("CYCLE ");
        } else {
            sb.append("NOCYCLE ");
        }
        if (sequence.getCacheSize() > 0) {
            sb.append("CACHE ").append(sequence.getCacheSize()).append(" ");
        } else {
            sb.append("NOCACHE ");
        }
        if (sequence.isOrder()) {
            sb.append("ORDER ");
        } else {
            sb.append("NOORDER ");
        }

        return sb.toString();
    }

    private String buildComment(OracleSequence sequence)
    {
        if (!CommonUtils.isEmpty(sequence.getDescription())) {
            return "COMMENT ON SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS " + SQLUtils.quoteString(sequence, sequence.getDescription());
        }
        return null;
    }

}
