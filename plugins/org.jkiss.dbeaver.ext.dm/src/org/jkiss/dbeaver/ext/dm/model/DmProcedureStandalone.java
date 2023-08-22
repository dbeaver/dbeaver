package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DmProcedureStandalone extends DmProcedureBase<DmSchema> implements DmSourceObject, DBPRefreshableObject {

	private boolean valid;
	private String sourceDeclaration;

	public DmProcedureStandalone(DmSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), JDBCUtils.safeGetLong(dbResult, "OBJECT_ID"),
				DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE")));
		this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
	}

	public DmProcedureStandalone(DmSchema schema, String name, DBSProcedureType procedureType) {
		super(schema, name, 01, procedureType);
		sourceDeclaration = procedureType.name() + " " + name + GeneralUtils.getDefaultLineSeparator() + "IS"
				+ GeneralUtils.getDefaultLineSeparator() + "BEGIN" + GeneralUtils.getDefaultLineSeparator() + "END "
				+ name + ";" + GeneralUtils.getDefaultLineSeparator();
	}

	@Property(viewable = true, order = 3)
	public boolean isValid() {
		return valid;
	}

	@NotNull
	@Override
	public DmSchema getSchema() {
		return getParentObject();
	}

	@Override
	public DmSourceType getSourceType() {
		return getProcedureType() == DBSProcedureType.PROCEDURE ? DmSourceType.PROCEDURE : DmSourceType.FUNCTION;
	}

	@Override
	public Integer getOverloadNumber() {
		return null;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = DmUtils.getSource(monitor, this, false, true);
		}
		return sourceDeclaration;
	}

	public void setObjectDefinitionText(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] { new DmObjectPersistAction(
				getProcedureType() == DBSProcedureType.PROCEDURE ? DmObjectType.PROCEDURE : DmObjectType.FUNCTION,
				"Compile procedure", "ALTER " + getSourceType().name() + " "
						+ getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE") };
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = DmUtils.getObjectStatus(monitor, this,
				getProcedureType() == DBSProcedureType.PROCEDURE ? DmObjectType.PROCEDURE
						: DmObjectType.FUNCTION);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getSchema().proceduresCache.refreshObject(monitor, getSchema(), this);
	}

	@Override
	public Collection<? extends DBSProcedureParameter> getParameters(DBRProgressMonitor monitor) throws DBException {
		return null;
	}
}
