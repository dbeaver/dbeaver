package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/09
 * @Description:
 */
public abstract class YashanDBPriv extends YashanDBObject<YashanDBGrantee> implements DBAPrivilege {
    private boolean adminOption;

    public YashanDBPriv(YashanDBGrantee user, String name, ResultSet resultSet) {
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
