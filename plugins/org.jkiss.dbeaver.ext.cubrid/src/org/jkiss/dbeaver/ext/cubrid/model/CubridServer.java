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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridServer implements DBSObject {
    private String linkName;
    private String host;
    private String port;
    private String dbName;
    private String userName;
    private String properties;
    private String owner;
    private String description;
    private CubridDataSource container;

    public CubridServer(@NotNull CubridDataSource container, @NotNull JDBCResultSet dbResult) {
        this.container = container;
        this.linkName = JDBCUtils.safeGetString(dbResult, "link_name");
        this.host = JDBCUtils.safeGetString(dbResult, "host");
        this.port = JDBCUtils.safeGetString(dbResult, "port");
        this.dbName = JDBCUtils.safeGetString(dbResult, "db_name");
        this.userName = JDBCUtils.safeGetString(dbResult, "user_name");
        this.properties = JDBCUtils.safeGetString(dbResult, "properties");
        this.owner = JDBCUtils.safeGetString(dbResult, "owner");
        this.description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public String getName() {
        return linkName;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getHost() {
        return host;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public String getPort() {
        return port;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public String getDbName() {
        return dbName;
    }

    @NotNull
    @Property(viewable = true, order = 5)
    public String getUserName() {
        return userName;
    }

    @Nullable
    @Property(viewable = true, order = 6)
    public String getProperties() {
        return properties;
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public String getOwner() {
        return owner;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 8)
    public String getDescription() {
        return description;
    }

    @NotNull
    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return container;
    }

    @Override
    public DBPDataSource getDataSource() {
        return container;
    }
}
