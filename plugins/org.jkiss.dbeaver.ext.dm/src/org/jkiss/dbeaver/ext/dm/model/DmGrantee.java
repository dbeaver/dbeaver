package org.jkiss.dbeaver.ext.dm.model;

import java.sql.SQLException;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Dm Grantee
 * @author caosw
 *
 */
public abstract class DmGrantee extends DmGlobalObject implements DBAUser, DBPScriptObject, DBPRefreshableObject {

	private static final Log log = Log.getLog(DmGrantee.class);

	final RolePrivCache rolePrivCache = new RolePrivCache();
	private final SystemPrivCache systemPrivCache = new SystemPrivCache();
	private final ObjectPrivCache objectPrivCache = new ObjectPrivCache();

	public DmGrantee(DmDataSource dataSource) {
        super(dataSource, true);
    }
	
	
	@Association
	public Collection<DmPrivRole> getRolePrivs(DBRProgressMonitor monitor) throws DBException {
		return rolePrivCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmPrivSystem> getSystemPrivs(DBRProgressMonitor monitor) throws DBException {
		return systemPrivCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<DmPrivObject> getObjectPrivs(DBRProgressMonitor monitor) throws DBException {
		return objectPrivCache.getAllObjects(monitor, this);
	}

	@Nullable
	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		rolePrivCache.clearCache();
		systemPrivCache.clearCache();
		objectPrivCache.clearCache();
		return this;
	}

	static class RolePrivCache extends JDBCObjectCache<DmGrantee, DmPrivRole> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmGrantee owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM DBA_ROLE_PRIVS WHERE GRANTEE=? ORDER BY GRANTED_ROLE");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmPrivRole fetchObject(@NotNull JDBCSession session, @NotNull DmGrantee owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmPrivRole(owner, resultSet);
		}
	}

	static class SystemPrivCache extends JDBCObjectCache<DmGrantee, DmPrivSystem> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmGrantee owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM DBA_SYS_PRIVS WHERE GRANTEE=? ORDER BY PRIVILEGE");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmPrivSystem fetchObject(@NotNull JDBCSession session, @NotNull DmGrantee owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmPrivSystem(owner, resultSet);
		}
	}

	static class ObjectPrivCache extends JDBCObjectCache<DmGrantee, DmPrivObject> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmGrantee owner)
				throws SQLException {
			boolean hasDBA = owner.getDataSource().isViewAvailable(session.getProgressMonitor(),
					DmConstants.SCHEMA_SYS, DmConstants.VIEW_DBA_TAB_PRIVS);
			String sql = "SELECT p.*,o.OBJECT_TYPE\n" + "FROM "
					+ (hasDBA ? "DBA_TAB_PRIVS p, DBA_OBJECTS o" : "ALL_TAB_PRIVS p, ALL_OBJECTS o") + "\n"
					+ "WHERE p.GRANTEE=? " + "AND o.OWNER=p." + (hasDBA ? "OWNER" : "TABLE_SCHEMA")
					+ " AND o.OBJECT_NAME=p.TABLE_NAME AND o.OBJECT_TYPE<>'PACKAGE BODY'";
//			final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT p.*,o.OBJECT_TYPE\n" + "FROM "
//					+ (hasDBA ? "DBA_TAB_PRIVS p, DBA_OBJECTS o" : "ALL_TAB_PRIVS p, ALL_OBJECTS o") + "\n"
//					+ "WHERE p.GRANTEE=? " + "AND o.OWNER=p." + (hasDBA ? "OWNER" : "TABLE_SCHEMA")
//					+ " AND o.OBJECT_NAME=p.TABLE_NAME AND o.OBJECT_TYPE<>'PACKAGE BODY'");
			final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmPrivObject fetchObject(@NotNull JDBCSession session, @NotNull DmGrantee owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmPrivObject(owner, resultSet);
		}
	}
}
