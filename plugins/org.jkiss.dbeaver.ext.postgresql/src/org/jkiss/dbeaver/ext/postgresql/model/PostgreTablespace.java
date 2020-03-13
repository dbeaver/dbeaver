/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PostgreTablespace
 */
public class PostgreTablespace extends PostgreInformation implements PostgreScriptObject {

    private long oid;
    private String name;
    private String loc;
    private long ownerId;
    private String options;

    public PostgreTablespace(PostgreDatabase database, ResultSet dbResult)
        throws SQLException {
        super(database);
        this.loadInfo(dbResult);
    }

    public PostgreTablespace(PostgreDatabase database) {
        super(database);
        this.oid = 0;
        this.name = "newtablespace";
        this.ownerId = 0;
        this.options = "";
        this.loc = "";
    }

    private void loadInfo(ResultSet dbResult) {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "spcname");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "spcowner");
        Object opts[] = JDBCUtils.safeGetArray(dbResult, "spcoptions");
        this.options = opts == null ? "" : Stream.of(opts).map(Object::toString).collect(Collectors.joining(","));
        this.loc = JDBCUtils.safeGetString(dbResult, "loc");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 2)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(viewable = true, order = 3)
    public String getLoc() {
        return loc;
    }

    @Property(viewable = true, order = 4)
    public String getOptions() {
        return options;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {

        StringBuilder sb = new StringBuilder("CREATE TABLESPACE ");
        sb.append(getName()).append(" OWNER ").append(getOwner(monitor).getName()).append(" LOCATION '")
            .append(getLoc()).append("'");
        if (getOptions() != null && !getOptions().isEmpty()) {
            sb.append(" WITH (").append(getOptions()).append(")");
        }

        return sb.toString();
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }


}

