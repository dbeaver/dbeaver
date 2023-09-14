package org.jkiss.dbeaver.ext.altibase.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class AltibasePrivObject extends AltibasePriv {
    
    private String grantorName;
    private String objType;
    private String objSchema;
    private String objName;
    private boolean grantable;
    
    protected AltibasePrivObject(AltibaseUser user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIV_NAME"), resultSet);
        grantorName = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
        objType = JDBCUtils.safeGetString(resultSet, "OBJ_TYPE");
        objSchema = JDBCUtils.safeGetString(resultSet, "SCHEMA_NAME");
        objName = JDBCUtils.safeGetString(resultSet, "OBJ_NAME");
        grantable = JDBCUtils.safeGetBoolean(resultSet, "WITH_GRANT_OPTION", "1");
    }
    
    @Property(viewable = true, order = 1)
    public String getGrantor() {
        return grantorName;
    }
    
    @Property(viewable = true, order = 2)
    public String getObjType() {
        return objType;
    }
    
    @Property(viewable = true, order = 3)
    public String getObjSchema() {
        return objSchema;
    }
    
    @Property(viewable = true, order = 4)
    public String getObjName() {
        return objName;
    }
    
    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName() {
        return super.getName();
    }
    
    @Property(viewable = true, order = 20)
    public boolean getGrantable() {
        return grantable;
    }
    


}
