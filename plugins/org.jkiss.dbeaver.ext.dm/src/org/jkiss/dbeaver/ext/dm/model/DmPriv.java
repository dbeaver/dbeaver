package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class DmPriv extends DmObject<DmGrantee> implements DBAPrivilege {

	private boolean adminOption;

	public DmPriv(DmGrantee user, String name, ResultSet resultSet) {
		super(user, name, true);
		this.adminOption = JDBCUtils.safeGetBoolean(resultSet, "ADMIN_OPTION", "Y");
	}

	@NotNull
	@Override
	public String getName() {
		return super.getName();
	}

	@Property(viewable = true, order = 3)
	public boolean isAdminOption() {
		return adminOption;
	}
}
