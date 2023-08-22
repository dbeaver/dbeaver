package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

public enum DmParameterMode {
	IN(DBSProcedureParameterKind.IN), OUT(DBSProcedureParameterKind.OUT), INOUT(DBSProcedureParameterKind.INOUT),
	RETURN(DBSProcedureParameterKind.RETURN);
	private final DBSProcedureParameterKind parameterKind;

	DmParameterMode(DBSProcedureParameterKind parameterKind) {
		this.parameterKind = parameterKind;
	}

	public static DmParameterMode getMode(String modeName) {
		if (CommonUtils.isEmpty(modeName)) {
			return null;
		} else if ("IN".equals(modeName)) {
			return IN;
		} else if ("OUT".equals(modeName)) {
			return DmParameterMode.OUT;
		} else {
			return DmParameterMode.INOUT;
		}
	}

	public DBSProcedureParameterKind getParameterKind() {
		return parameterKind;
	}
}
