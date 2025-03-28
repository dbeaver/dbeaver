package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.iotdb.IoTDBPrivilegeInfo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class IoTDBPrivilege implements DBAPrivilege {

    private final IoTDBDataSource dataSource;
    public final String name;
    public IoTDBPrivilegeInfo.Kind kind;

    public IoTDBPrivilege(IoTDBDataSource dataSource,
                          String name,
                          IoTDBPrivilegeInfo.Kind kind) {
        this.dataSource = dataSource;
        this.name = name;
        this.kind = kind;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public IoTDBPrivilegeInfo.Kind getKind() {
        return kind;
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
}
