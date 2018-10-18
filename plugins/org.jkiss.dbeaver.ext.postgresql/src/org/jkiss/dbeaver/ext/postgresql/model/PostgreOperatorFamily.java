/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreOperatorFamily
 */
public class PostgreOperatorFamily extends PostgreInformation {

    private long oid;
    private PostgreAccessMethod accessMethod;
    private String name;
    private long namespaceId;
    private long ownerId;

    public PostgreOperatorFamily(PostgreAccessMethod accessMethod, ResultSet dbResult)
        throws SQLException
    {
        super(accessMethod.getDatabase());
        this.accessMethod = accessMethod;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "opfname");
        this.namespaceId = JDBCUtils.safeGetLong(dbResult, "opfnamespace");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "opfowner");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 3)
    public PostgreSchema getNamespace(DBRProgressMonitor monitor) throws DBException {
        return accessMethod.getDatabase().getSchema(monitor, namespaceId);
    }

    @Property(viewable = true, order = 4)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return accessMethod.getDatabase().getRoleById(monitor, ownerId);
    }

}

