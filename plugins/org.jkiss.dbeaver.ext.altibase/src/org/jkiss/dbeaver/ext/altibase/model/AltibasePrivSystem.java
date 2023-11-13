package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class AltibasePrivSystem extends AltibasePriv {
    
    private String grantorName;
    private boolean isGranted;
    
    protected AltibasePrivSystem(AltibaseGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIV_NAME"));
        grantorName = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
        isGranted = (grantorName != null);
    }
    
    // For special account: SYSTEM_, SYS account
    protected AltibasePrivSystem(AltibaseGrantee user, String privName) {
        super(user, privName);
        isGranted = true;
    }
    
    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return super.getName();
    }
    
    @Property(viewable = true, order = 3)
    public boolean getGranted() {
        return isGranted;
    }
    
    @Property(viewable = true, order = 4)
    public String getGrantor() {
        return grantorName;
    }
}
