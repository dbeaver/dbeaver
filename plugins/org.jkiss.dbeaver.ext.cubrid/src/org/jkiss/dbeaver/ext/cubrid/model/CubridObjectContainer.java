package org.jkiss.dbeaver.ext.cubrid.model;

import java.sql.SQLException;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public abstract class CubridObjectContainer extends GenericObjectContainer implements CubridStructContainer{

	private final CubridDataSource dataSource;
	private final CubridUserCache cubridUserCache;

	protected CubridObjectContainer(CubridDataSource dataSource) {
		super(dataSource);
		this.dataSource = dataSource;
		this.cubridUserCache = new CubridUserCache();
	}
	
	@NotNull
	@Override
	public CubridDataSource getDataSource() {
		return dataSource;
	}
	
	@Override
	public Collection<? extends CubridUser> getCubridUsers(DBRProgressMonitor monitor) throws DBException {
		return cubridUserCache.getAllObjects(monitor, this);
	}
	
	public class CubridUserCache extends JDBCObjectCache<CubridObjectContainer, CubridUser> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull CubridObjectContainer container) throws SQLException {
			return container.getDataSource().getMetaModel().prepareCubridUserLoadStatement(session, container);
		}

		@Override
		protected CubridUser fetchObject(JDBCSession session, CubridObjectContainer container, JDBCResultSet resultSet) throws SQLException, DBException {
			String name = resultSet.getString("name");
			String comment = resultSet.getString("comment");
			return container.getDataSource().getMetaModel().createCubridUserImpl(container, name, comment);
		}

	}

}
