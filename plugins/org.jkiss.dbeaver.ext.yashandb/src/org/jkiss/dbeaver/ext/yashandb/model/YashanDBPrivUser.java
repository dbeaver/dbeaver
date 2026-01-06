package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

public class YashanDBPrivUser extends YashanDBPriv implements DBSObjectLazy<YashanDBDataSource> {

	public YashanDBPrivUser(YashanDBGrantee user, ResultSet resultSet) {
		super(user, JDBCUtils.safeGetString(resultSet, "GRANTEE"), resultSet);
		this.grantedRole = JDBCUtils.safeGetString(resultSet, "GRANTED_ROLE");
		this.user = this.name;
	}

	private Object user;
	private String grantedRole;

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
		return YashanDBUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().userCache, this, null);
	}

	@Property(viewable = true, order = 4)
	public String getGrantedRole() {
		return grantedRole;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return this.user;
	}
}
