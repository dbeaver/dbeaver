package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmSequence;
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

public class DmSequenceManager extends SQLObjectEditor<DmSequence, DmSchema> {

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	protected void validateObjectProperties(DBRProgressMonitor monitor,ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Sequence name cannot be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmSequence> getObjectsCache(DmSequence object) {
		return object.getSchema().sequenceCache;
	}

	@Override
	protected DmSequence createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DmSchema schema = (DmSchema) container;
		return new DmSequence(schema, "NEW_SEQUENCE");
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmSequence, DmSchema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		String sql = buildStatement(command.getObject(), false);
		actions.add(new SQLDatabasePersistAction("Create Sequence", sql));
		String comment = buildComment(command.getObject());
		if (comment != null) {
			actions.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<DmSequence, DmSchema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		String sql = buildStatement(command.getObject(), true);
		actionList.add(new SQLDatabasePersistAction("Alter Sequence", sql));
		String comment = buildComment(command.getObject());
		if (comment != null) {
			actionList.add(new SQLDatabasePersistAction("Comment on Sequence", comment));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmSequence, DmSchema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
		DBEPersistAction action = new SQLDatabasePersistAction("Drop Sequence", sql);
		actions.add(action);
	}

	private String buildComment(DmSequence sequence) {
		if (!CommonUtils.isEmpty(sequence.getDescription())) {
			return "COMMENT ON SEQUENCE " + sequence.getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS "
					+ SQLUtils.quoteString(sequence, sequence.getDescription());
		}
		return null;
	}

	private String buildStatement(DmSequence sequence, Boolean forUpdate) {
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
}
