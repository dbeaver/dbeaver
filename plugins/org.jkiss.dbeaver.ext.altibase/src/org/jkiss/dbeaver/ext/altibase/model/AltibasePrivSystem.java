package org.jkiss.dbeaver.ext.altibase.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class AltibasePrivSystem extends AltibasePriv {
    
    private String grantorName;
    private boolean isGranted;
    
    protected AltibasePrivSystem(AltibaseUser user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIV_NAME"), resultSet);
        grantorName = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
        isGranted = (grantorName != null);
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
