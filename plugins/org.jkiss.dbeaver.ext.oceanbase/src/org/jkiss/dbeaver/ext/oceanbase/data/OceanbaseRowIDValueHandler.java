package org.jkiss.dbeaver.ext.oceanbase.data;

import java.sql.SQLException;

import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class OceanbaseRowIDValueHandler extends JDBCStringValueHandler {

	public static final OceanbaseRowIDValueHandler INSTANCE = new OceanbaseRowIDValueHandler();

	@Override
	protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
			throws SQLException {
		return resultSet.getString(index);
	}

}
