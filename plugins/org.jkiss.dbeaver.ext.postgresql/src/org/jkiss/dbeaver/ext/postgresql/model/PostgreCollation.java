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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreCollation
 */
public class PostgreCollation implements PostgreObject {

    private PostgreDatabase database;
    private PostgreSchema schema;
    private long oid;
    private String name;
    private long ownerId;
    private String provider;
    private long encodingId;
    private String collate;
    private String ctype;

    public PostgreCollation(DBRProgressMonitor monitor, PostgreDatabase database, ResultSet dbResult)
        throws SQLException, DBException {
        this.database = database;
        this.loadInfo(monitor, dbResult);
    }

    private void loadInfo(DBRProgressMonitor monitor, ResultSet dbResult)
        throws SQLException, DBException {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "collname");
        this.schema = database.getSchema(monitor, JDBCUtils.safeGetLong(dbResult, "collnamespace"));
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "collowner");
        if (getDataSource().isServerVersionAtLeast(10, 0)) {
            this.provider = JDBCUtils.safeGetString(dbResult, "collprovider");
        }
        this.encodingId = JDBCUtils.safeGetLong(dbResult, "collencoding");
        this.collate = JDBCUtils.safeGetString(dbResult, "collcollate");
        this.ctype = JDBCUtils.safeGetString(dbResult, "collctype");
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public PostgreSchema getSchema() {
        return schema;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 3)
    public PostgreRole getOwnerId(DBRProgressMonitor monitor) throws DBException {
        return database.getRoleById(monitor, ownerId);
    }

    @Property(viewable = true, order = 5)
    public String getProvider() {
        return provider;
    }

    @Property(viewable = true, order = 6)
    public long getEncodingId() {
        return encodingId;
    }

    @Property(viewable = true, order = 7)
    public String getCollate() {
        return collate;
    }

    @Property(viewable = true, order = 8)
    public String getCtype() {
        return ctype;
    }

    @Property(viewable = false, order = 50)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

}
