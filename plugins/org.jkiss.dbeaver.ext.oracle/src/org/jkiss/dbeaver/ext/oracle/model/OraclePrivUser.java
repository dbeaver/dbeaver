/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;

/**
 * OraclePrivUser
 */
public class OraclePrivUser extends OraclePriv implements DBSObjectLazy<OracleDataSource> {
    private Object user;
    private boolean defaultRole;

    public OraclePrivUser(OracleGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTEE"), resultSet);
        this.defaultRole = JDBCUtils.safeGetBoolean(resultSet, "DEFAULT_ROLE", "Y");
        this.user = this.name;
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }

    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 2, supportsPreview = true)
    public Object getUser(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null) {
            return user;
        }
        return OracleUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().userCache, this, null);
    }

    @Property(viewable = true, order = 4)
    public boolean isDefaultRole()
    {
        return defaultRole;
    }

    @Override
    public Object getLazyReference(Object propertyId)
    {
        return this.user;
    }

}
