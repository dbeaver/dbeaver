/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Role used by Users and Groups
 * 
 * @author Denis Forveille
 */
public class DB2RoleDep extends DB2Object<DB2Role> implements DBAPrivilege {

    DB2GrantorGranteeType granteeType;
    private DB2Grantee grantee;
    private Boolean admin;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2RoleDep(DB2Role role, ResultSet resultSet) throws DBException
    {
        super(role, JDBCUtils.safeGetString(resultSet, "GRANTEE"), true);

        this.granteeType = CommonUtils.valueOf(DB2GrantorGranteeType.class,
            JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTEETYPE"));
        String granteeName = JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTEE");
        switch (granteeType) {
        case U:
            this.grantee = getDataSource().getUser(new VoidProgressMonitor(), granteeName);
            break;
        case G:
            this.grantee = getDataSource().getGroup(new VoidProgressMonitor(), granteeName);
            break;
        case R:
            this.grantee = getDataSource().getRole(new VoidProgressMonitor(), granteeName);
            break;

        default:
            break;
        }
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
    public DB2Grantee getGrantee()
    {
        return grantee;
    }

    @Property(viewable = true, order = 2)
    public DB2GrantorGranteeType getGranteeType()
    {
        return granteeType;
    }

    @Property(viewable = true, order = 3)
    public Boolean getAdmin()
    {
        return admin;
    }

}
