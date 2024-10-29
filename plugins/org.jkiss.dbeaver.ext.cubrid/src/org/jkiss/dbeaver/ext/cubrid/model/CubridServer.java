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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridServer implements DBSObject, DBPNamedObject2 {
    private String name;
    private String host;
    private String port;
    private String dbName;
    private String userName;
    private String password;
    private String properties;
    private CubridUser owner;
    private String description;
    private CubridDataSource container;
    private boolean persisted;

    public CubridServer(@NotNull CubridDataSource container, @NotNull JDBCResultSet dbResult) {
        this.container = container;
        this.name = JDBCUtils.safeGetString(dbResult, "link_name");
        this.host = JDBCUtils.safeGetString(dbResult, "host");
        this.port = JDBCUtils.safeGetString(dbResult, "port");
        this.dbName = JDBCUtils.safeGetString(dbResult, "db_name");
        this.userName = JDBCUtils.safeGetString(dbResult, "user_name");
        this.properties = JDBCUtils.safeGetString(dbResult, "properties");
        this.owner = (CubridUser) container.getSchema(JDBCUtils.safeGetString(dbResult, "owner"));
        this.description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
        this.persisted = true;
    }

    public CubridServer(@NotNull CubridDataSource container, String name) {
        this.container = container;
        this.name = name;
        this.host = CubridConstants.DEFAULT_HOST;
        this.port = CubridConstants.DEFAULT_PORT;
        this.owner = (CubridUser) container.getSchema(getDataSource().getContainer().getConnectionConfiguration().getUserName().toUpperCase());
        this.persisted = false;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Property(viewable = true, order = 10)
    public CubridUser getOwner() {
        return owner;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 20)
    public String getHost() {
        return host;
    }

    public void setHost(@NotNull String host) {
        this.host = host;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 30)
    public String getPort() {
        return port;
    }

    public void setPort(@NotNull String port) {
        this.port = port;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 40)
    public String getDbName() {
        return dbName;
    }

    public void setDbName(@NotNull String dbName) {
        this.dbName = dbName;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 50)
    public String getUserName() {
        return userName;
    }

    public void setUserName(@NotNull String userName) {
        this.userName = userName;
    }

    @Nullable
    @Property(viewable = true, password = true, editable = true, updatable = true, order = 60)
    public String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 70)
    public String getProperties() {
        return properties;
    }

    public void setProperties(@Nullable String properties) {
        this.properties = properties;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 80)
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NotNull
    @Override
    public boolean isPersisted() {
        return persisted;
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
