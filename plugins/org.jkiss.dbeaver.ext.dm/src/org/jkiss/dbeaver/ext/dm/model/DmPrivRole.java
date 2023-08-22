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

public class DmPrivRole extends DmPriv implements DBSObjectLazy<DmDataSource> {

	private Object role;
	private boolean defaultRole;

	public DmPrivRole(DmGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTED_ROLE"), resultSet);
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.role = this.name;
    }

	@NotNull
	@Override
	public String getName() {
		return super.getName();
	}

	@Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 2, supportsPreview = true)
	public Object getRole(DBRProgressMonitor monitor) throws DBException {
		if (monitor == null) {
			return role;
		}
		return DmUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().roleCache, this, null);
	}

	@Property(viewable = true, order = 4)
	public boolean isDefaultRole() {
		return defaultRole;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return this.role;
	}
}
