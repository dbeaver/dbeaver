package org.jkiss.dbeaver.ext.altibase.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.access.DBAPrivilege;

public class AltibasePriv extends AltibaseObject<AltibaseUser> implements DBAPrivilege {

    protected AltibasePriv(AltibaseUser user, String name, ResultSet resultSet) {
        super(user, name, true);
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }
}
