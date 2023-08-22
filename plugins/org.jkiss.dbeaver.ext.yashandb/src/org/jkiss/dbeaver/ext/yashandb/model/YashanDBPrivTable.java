package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;


public class YashanDBPrivTable extends YashanDBObject<YashanDBTableBase> implements DBAPrivilege {
    private String grantee;
    private String grantor;
    private boolean grantable;
    private boolean hierarchy;

    public YashanDBPrivTable(YashanDBTableBase table, ResultSet resultSet) {
        super(table, JDBCUtils.safeGetString(resultSet, "PRIVILEGE"), true);
        this.grantee = JDBCUtils.safeGetString(resultSet, "GRANTEE");
        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantable = JDBCUtils.safeGetBoolean(resultSet, "GRANTABLE", "Y");
        this.hierarchy = JDBCUtils.safeGetBoolean(resultSet, "HIERARCHY", "Y");
    }

    @Property(viewable = true, order = 1)
    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    @Property(viewable = true, order = 5, supportsPreview = true)
    public Object getGrantee(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null) {
            return grantee;
        }
        return getDataSource().getGrantee(monitor, grantee);
    }

    @Property(viewable = true, order = 6, supportsPreview = true)
    public Object getGrantor(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null) {
            return grantor;
        }
        return getDataSource().getGrantee(monitor, grantor);
    }

    @Property(viewable = true, order = 10)
    public boolean isGrantable() {
        return grantable;
    }

    @Property(viewable = true, order = 11)
    public boolean isHierarchy() {
        return hierarchy;
    }
}
