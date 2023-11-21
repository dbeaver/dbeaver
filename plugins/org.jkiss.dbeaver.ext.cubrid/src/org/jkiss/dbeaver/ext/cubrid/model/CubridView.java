package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;

public class CubridView extends CubridTable {

	public CubridView(CubridObjectContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
		super(container, tableName, tableType, dbResult);
	}

	@Override
	public boolean isView() {
		return true;
	}

	@Override
	public String getDDL() {
		return null;
	}

}
