package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class CubridUniqueKey extends GenericUniqueKey{

	public CubridUniqueKey(CubridTable table, String name, String remarks, DBSEntityConstraintType constraintType,
		boolean persisted) {
		super(table, name, remarks, constraintType, persisted);
	}

}
