package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/09
 * @Description:
 */
public class YashanDBPrivRole extends YashanDBPriv implements DBSObjectLazy<YashanDBDataSource> {
    private Object role;
    private boolean defaultRole;

    public YashanDBPrivRole(YashanDBGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTED_ROLE"), resultSet);
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.role = this.name;
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * DBA_ROLES not exists.
     */
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 2, supportsPreview = true)
    public Object getRole(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null) {
            return role;
        }
//        return role;
      return YashanDBUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().roleCache, this, null);
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
