package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/09
 * @Description:
 */
public class YashanDBPrivSystem extends YashanDBPriv {

    private boolean defaultRole;

    public YashanDBPrivSystem(YashanDBGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIVILEGE"), resultSet);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return super.getName();
    }
}
