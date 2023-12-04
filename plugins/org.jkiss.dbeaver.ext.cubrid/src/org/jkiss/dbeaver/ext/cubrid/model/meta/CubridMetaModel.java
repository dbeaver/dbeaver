package org.jkiss.dbeaver.ext.cubrid.model.meta;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.CubridObjectContainer;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableBase;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableIndex;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUser;
import org.jkiss.dbeaver.ext.cubrid.model.CubridView;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class CubridMetaModel extends GenericMetaModel {
	
	CubridMetaModelDescriptor descriptor;

	public CubridMetaModel()
	{
	}

	public GenericMetaObject getMetaObject(String id) {
		return descriptor == null ? null : descriptor.getObject(id);
	}

	public CubridUser createCubridUserImpl(CubridObjectContainer container, String name, String comment) {
		return new CubridUser(container, name, comment);
	}

	public JDBCStatement prepareCubridUserLoadStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer container) throws SQLException {
		String sql= "select * from db_user";
		final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
		return dbStat;
	}

	public JDBCStatement prepareTableLoadStatement(@NotNull JDBCSession session)
		throws SQLException {
		String sql= "select a.*, case when class_type = 'CLASS' then 'TABLE' \r\n"
				+ "when class_type = 'VCLASS' then 'VIEW' end as TABLE_TYPE, \r\n"
				+ "b.current_val from db_class a LEFT JOIN db_serial b on \r\n"
				+ "a.class_name = b.class_name where a.is_system_class='NO'";
		final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
		return dbStat;
	}

	public JDBCStatement prepareSystemTableLoadStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner, @Nullable CubridTableBase object, @Nullable String objectName)
		throws SQLException {
		String sql= "select *, class_name as TABLE_NAME, case when class_type = 'CLASS' \r\n"
	    		+ "then 'TABLE' end as TABLE_TYPE from db_class\r\n"
	       		+ "where class_type = 'CLASS' \r\n"
	       		+ "and is_system_class = 'YES'";
		final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
		return dbStat;
	}

	public JDBCStatement prepareSystemViewLoadStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner, @Nullable CubridTableBase object, @Nullable String objectName)
		throws SQLException {
		String sql= "select *, case when class_type = 'VCLASS' \r\n"
				+ "then 'VIEW' end as TABLE_TYPE,\r\n"
				+ "class_name as TABLE_NAME from db_class\r\n"
				+ "where class_type='VCLASS'\r\n"
				+ "and is_system_class='YES'";
		final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
		return dbStat;
	}


	@Override
	public JDBCStatement prepareTableColumnLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException {
	    if(forTable instanceof CubridTable) {
			CubridTableBase tablebase = (CubridTableBase) forTable;
		    return session.getMetaData().getColumns(null, null, tablebase!=null?tablebase.getUniqueName():null, null).getSourceStatement();
	    }
	   return super.prepareTableColumnLoadStatement(session, owner, forTable);
	   }

	public CubridTableBase createTableImpl(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner, @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {

		String tableName = JDBCUtils.safeGetString( dbResult, CubridConstants.CLASS_NAME);
		String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);

		CubridTableBase table = this.createTableImpl(owner, tableName, tableType, dbResult);
		if (table == null) {
			return null;
		}
		return table;
	}

	public CubridTableBase createTableImpl(CubridObjectContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
		if (tableType != null && isView(tableType)) {
			return new CubridView(container, tableName, tableType, dbResult);
		}
		return new CubridTable(container, tableName, tableType, dbResult);
	}

	public CubridTableIndex createIndexImpl(
		CubridTable table,
		boolean nonUnique,
		String qualifier,
		long cardinality,
		String indexName,
		DBSIndexType indexType,
		boolean persisted) {
		return new CubridTableIndex(
			table,
			nonUnique,
			qualifier,
			cardinality,
			indexName,
			indexType,
			persisted);
	}

}
