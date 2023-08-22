package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

/**
 * Dm Priv User
 * 
 * @author caosw
 *
 */
public class DmPrivUser extends DmPriv implements DBSObjectLazy<DmDataSource> {

	private Object user;
	private boolean defaultRole;

	public DmPrivUser(DmGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTEE"), resultSet);
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.user = this.name;
    }

	@NotNull
	@Override
	public String getName() {
		return super.getName();
	}

	@Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 2, supportsPreview = true)
	public Object getUser(DBRProgressMonitor monitor) throws DBException {
		if (monitor == null) {
			return user;
		}
		return DmUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().userCache, this, null);
	}

	@Property(viewable = true, order = 4)
	public boolean isDefaultRole() {
		return defaultRole;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return this.user;
	}

}
