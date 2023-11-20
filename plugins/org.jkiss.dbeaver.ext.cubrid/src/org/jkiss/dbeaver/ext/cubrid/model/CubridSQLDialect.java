package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;

public class CubridSQLDialect extends GenericSQLDialect {
	public static final String CUBRID_DIALECT_ID = "cubrid";

	public CubridSQLDialect() {
		super("Cubrid", "cubrid");
	}
}
