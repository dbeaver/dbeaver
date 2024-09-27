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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;

/**
 * AltibasePrivUser
 */
public class AltibasePrivUser extends AltibasePriv implements DBSObjectLazy<AltibaseDataSource> {
    private Object user;
    private String grantor;

    public AltibasePrivUser(AltibaseGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "GRANTEE_NAME"));
        this.user = this.name;
        grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
    }
    
    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }
    
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 2, supportsPreview = true)
    public Object getUser(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null) {
            return user;
        }
        return AltibaseUtils.resolveLazyReference(monitor, getDataSource(), getDataSource().userCache, this, null);
    }
    
    @Property(viewable = true, order = 3)
    public String getGrantor() {
        return grantor;
    }
    
    @Nullable
    @Override
    public Object getLazyReference(Object propertyId) {
        return this.user;
    }
}