/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Authorisations assigned to Roles
 * 
 * @author Denis Forveille
 */
public class DB2RoleAuth extends DB2Object<DB2Role> implements DBAPrivilege {

    private String grantor;
    private DB2GrantorGranteeType grantorType;
    private Boolean admin;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2RoleAuth(DB2Role role, ResultSet resultSet)
    {
        super(role, JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTEE"), true);

        this.grantor = JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class,
            JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTORTYPE"));
        this.admin = JDBCUtils.safeGetBoolean(resultSet, "ADMIN", DB2YesNo.Y.name());
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(hidden = true)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 1)
    public DB2Role getRole()
    {
        return parent;
    }

    @Property(viewable = true, order = 2)
    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

    @Property(viewable = true, order = 3)
    public String getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 4)
    public Boolean getAdmin()
    {
        return admin;
    }

}
