package org.jkiss.dbeaver.ext.cubrid.model.meta;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cubrid.model.CubridStructContainer;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUser;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;

public class CubridMetaModel extends GenericMetaModel {
	
	CubridMetaModelDescriptor descriptor;

	public CubridMetaModel()
	{
	}

	public GenericMetaObject getMetaObject(String id) {
		return descriptor == null ? null : descriptor.getObject(id);
	}
    
	// User
	public CubridUser createCubridUserImpl(CubridStructContainer container, String name, String comment) {
		return new CubridUser(container, name, comment);
	}

	public JDBCStatement prepareCubridUserLoadStatement(@NotNull JDBCSession session, @NotNull CubridStructContainer container) throws SQLException {
		String sql= "select * from db_user";
		final JDBCPreparedStatement dbStat = session.prepareStatement(sql);

		return dbStat;
	}

}
