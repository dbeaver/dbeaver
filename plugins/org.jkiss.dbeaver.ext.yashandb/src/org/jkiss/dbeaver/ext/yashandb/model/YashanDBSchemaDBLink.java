/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Date;

/**
 * DB Link
 */
public class YashanDBSchemaDBLink extends YashanDBSchemaObject {

    private static final Log log = Log.getLog(YashanDBSchemaDBLink.class);

    String name;
    private String userName;
    private String host;
    private Date created;
    private String password;
    private String owner;

    protected YashanDBSchemaDBLink(DBRProgressMonitor progressMonitor, YashanDBSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "DB_LINK"), true);
        this.name=JDBCUtils.safeGetString(dbResult, "DB_LINK");
        this.userName = JDBCUtils.safeGetString(dbResult, "USERNAME");
        this.host = JDBCUtils.safeGetString(dbResult, "HOST");
        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.password="******";

    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1, editable = true)
    public String getName() {
        return name;
    }

    @Property(viewable = true,order = 2)
    public String getOwner(){
        return owner;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public String getUserName() {
        return userName;
    }

    @Property(order = 4, editable = true, updatable = true,viewable = true)
    public String getPassword() {
        return password;
    }

    @Property(viewable = true, editable = true, order = 5)
    public String getHost() {
        return host;
    }

    @Property(viewable = true, order = 6)
    public Date getCreated() {
        return created;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public static Object resolveObject(DBRProgressMonitor monitor, YashanDBSchema schema, String dbLink) throws DBException
    {
        if (CommonUtils.isEmpty(dbLink)) {
            return null;
        }
        final YashanDBSchemaDBLink object = schema.schemaDBLinkCache.getObject(monitor, schema, dbLink);
        if (object == null) {
            log.warn("DB Link '" + dbLink + "' not found in schema '" + schema.getName() + "'");
            return dbLink;
        }
        return object;
    }
}
