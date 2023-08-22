package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * DM Priv System
 * @author caosw
 *
 */
public class DmPrivSystem extends DmPriv {
	private boolean defaultRole;

	public DmPrivSystem(DmGrantee user, ResultSet resultSet) {
		super(user, JDBCUtils.safeGetString(resultSet, "PRIVILEGE"), resultSet);
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 2)
	public String getName() {
		return super.getName();
	}
}
