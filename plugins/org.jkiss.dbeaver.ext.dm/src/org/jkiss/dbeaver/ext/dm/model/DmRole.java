package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Dm Role
 * 
 * @author caosw
 *
 */
public class DmRole extends DmGrantee implements DBARole {

	private static final Log log = Log.getLog(DmRole.class);

	private String name;
	private String authentication;
	private final UserCache userCache = new UserCache();

	public DmRole(DmDataSource dataSource, ResultSet resultSet) {
        super(dataSource);
        this.name = JDBCUtils.safeGetString(resultSet, "ROLE");
        this.authentication = JDBCUtils.safeGetStringTrimmed(resultSet, "PASSWORD_REQUIRED");
    }

	@NotNull
	@Override
	@Property(viewable = true, order = 2)
	public String getName() {
		return name;
	}

	@Property(viewable = true, order = 3)
	public String getAuthentication() {
		return authentication;
	}

	@Association
	public Collection<DmPrivUser> getUserPrivs(DBRProgressMonitor monitor) throws DBException {
		return userCache.getAllObjects(monitor, this);
	}

	@Nullable
	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		userCache.clearCache();
		return super.refreshObject(monitor);
	}

	static class UserCache extends JDBCObjectCache<DmRole, DmPrivUser> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmRole owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM DBA_ROLE_PRIVS WHERE GRANTED_ROLE=? ORDER BY GRANTEE");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected DmPrivUser fetchObject(@NotNull JDBCSession session, @NotNull DmRole owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DmPrivUser(owner, resultSet);
		}
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
}
