/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class AltibasePrivObject extends AltibasePriv {
    
    private String grantorName;
    private String objType;
    private String objSchema;
    private String objName;
    private boolean grantable;
    
    protected AltibasePrivObject(AltibaseGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIV_NAME"));
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
