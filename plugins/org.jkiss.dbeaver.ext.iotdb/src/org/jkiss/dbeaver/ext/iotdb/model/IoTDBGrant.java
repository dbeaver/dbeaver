package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.access.DBAPrivilegeGrant;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

public class IoTDBGrant implements DBSObject, DBAPrivilegeGrant{

    private final Log log = Log.getLog(IoTDBGrant.class);
    private final IoTDBUser user;
    private final List<IoTDBPrivilege> privileges;
    private String role;
    private String scope;
    private boolean grantOption;

    public IoTDBGrant(IoTDBUser user, List<IoTDBPrivilege> privileges, String role, String scope, boolean grantOption) {
        this.user = user;
        this.privileges = privileges;
        this.role = role;
        this.scope = scope;
        this.grantOption = grantOption;
    }

    @Override
    public Object getSubject(@NotNull DBRProgressMonitor dbrProgressMonitor) throws DBException {
        return user;
    }

    @Override
    public Object getObject(@NotNull DBRProgressMonitor dbrProgressMonitor) throws DBException {
        return "testObject";
    }

    @Override
    public DBAPrivilege[] getPrivileges() {
        return privileges.toArray(new DBAPrivilege[0]);
    }

    @Override
    public boolean isGranted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return this.user;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this.user.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return privileges.get(0).name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Property(viewable = true, editable = true, order = 4, id = "grantOption")
    public boolean getGrantOption() {
        return grantOption;
    }

    public void setGrantOption(boolean grantOption) {
        this.grantOption = grantOption;
    }
}
