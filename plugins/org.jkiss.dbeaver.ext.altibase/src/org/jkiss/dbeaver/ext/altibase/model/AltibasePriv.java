package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.access.DBAPrivilege;

public class AltibasePriv extends AltibaseObject<AltibaseGrantee> implements DBAPrivilege {

    protected AltibasePriv(AltibaseGrantee user, String name) {
        super(user, name, true);
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }
}
