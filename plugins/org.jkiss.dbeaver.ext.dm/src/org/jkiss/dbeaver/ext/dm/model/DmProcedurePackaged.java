package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public class DmProcedurePackaged extends DmProcedureBase<DmPackage> implements DBPUniqueObject {

	private Integer overload;

	public DmProcedurePackaged(DmPackage ownerPackage, ResultSet dbResult) {
		super(ownerPackage, JDBCUtils.safeGetString(dbResult, "PROCEDURE_NAME"), 0l,
				DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "PROCEDURE_TYPE")));
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), getParentObject(), this);
	}

	@Override
	public DmSchema getSchema() {
		return getParentObject().getSchema();
	}

	@Override
	public Integer getOverloadNumber() {
		return overload;
	}

	public void setOverload(int overload) {
		this.overload = overload;
	}

	@NotNull
	@Override
	public String getUniqueName() {
		return overload == null || overload <= 1 ? getName() : getName() + "#" + overload;
	}

	@Override
	public Collection<? extends DBSProcedureParameter> getParameters(DBRProgressMonitor monitor) throws DBException {
		return null;
	}
}
