package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;

public class CubridView extends CubridTableBase implements DBSObjectWithScript {

	private String ddl;

	public CubridView(CubridObjectContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
		super(container, tableName, tableType, dbResult);
	}

	@Override
	public boolean isView() {
		return true;
	}

	@Override
	public String getDDL() {
		return ddl;
	}

	@Override
	public void setObjectDefinitionText(String source) {
		this.ddl = source;
	}

}
