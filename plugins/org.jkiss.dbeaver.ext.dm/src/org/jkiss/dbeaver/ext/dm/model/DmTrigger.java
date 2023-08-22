package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

public abstract class DmTrigger<PARENT extends DBSObject> extends DmObject<PARENT>
		implements DBSTrigger, DBPQualifiedObject, DmSourceObject {

	private String triggerType;
	private String triggeringEvent;
	private String columnName;
	private DmObjectStatus status;
	private String sourceDeclaration;

	public DmTrigger(PARENT parent, String name) {
		super(parent, name, false);
	}

	public DmTrigger(PARENT parent, ResultSet dbResult) {
		super(parent, JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"), true);
		this.triggerType = JDBCUtils.safeGetString(dbResult, "TRIGGERING_TYPE");
		this.triggeringEvent = JDBCUtils.safeGetString(dbResult, "TRIGGERING_EVENT");
		this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
		this.status = CommonUtils.valueOf(DmObjectStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return super.getName();
	}

	@NotNull
	@Property(viewable = true, order = 5)
	public String getTriggerType() {
		return triggerType;
	}

	@Property(viewable = true, order = 6)
	public String getTriggeringEvent() {
		return triggeringEvent;
	}

	@Property(viewable = true, order = 7)
	public String getColumnName() {
		return columnName;
	}

	@Property(viewable = true, order = 8)
	public DmObjectStatus getStatus() {
		return status;
	}

	@Override
	public DmSourceType getSourceType() {
		return DmSourceType.TRIGGER;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = DmUtils.getSource(monitor, this, false, false);
		}
		return sourceDeclaration;
	}

	public void setObjectDefinitionText(String source) {
		this.sourceDeclaration = source;
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return status != DmObjectStatus.ERROR ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.status = (DmUtils.getObjectStatus(monitor, this, DmObjectType.TRIGGER) ? DmObjectStatus.ENABLED
				: DmObjectStatus.ERROR);
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] { new DmObjectPersistAction(DmObjectType.TRIGGER, "Compile trigger",
				"ALTER TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE") };
	}

	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.DDL);
	}
}
