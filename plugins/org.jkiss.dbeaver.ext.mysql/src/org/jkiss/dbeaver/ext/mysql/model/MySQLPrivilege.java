/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * MySQLPrivilege
 */
public class MySQLPrivilege implements DBAPrivilege
{
    private static final Log log = Log.getLog(MySQLPrivilege.class);

    public static final String GRANT_PRIVILEGE = "Grant Option";
    public static final String ALL_PRIVILEGES = "All Privileges";

    public static final Map<String, String> BAD_PRIV_NAME_MAP = new HashMap<>();

    static {
        BAD_PRIV_NAME_MAP.put("Delete versioning rows", "Delete history");
    }

    public enum Kind {
        OBJECTS,
        DDL,
        ADMIN,
        MISC
    }

    private MySQLDataSource dataSource;
    private String name;
    private String context;
    private String comment;
    private Kind kind;
    
    public MySQLPrivilege(MySQLDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(resultSet, "privilege");
        this.context = JDBCUtils.safeGetString(resultSet, "context");
        this.comment = JDBCUtils.safeGetString(resultSet, "comment");

        if (context.contains("Admin") || context.contains("server")) {
            kind = Kind.ADMIN;
        } else if (context.contains("Databases")) {
            kind = Kind.DDL;
        } else if (context.contains("Tables")) {
            kind = Kind.OBJECTS;
        } else {
            kind = Kind.MISC;
        }
    }

    public Kind getKind()
    {
        return kind;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    public String getFixedPrivilegeName() {
        String fixedName = BAD_PRIV_NAME_MAP.get(name);
        if (fixedName != null) {
            return fixedName;
        }
        return name;
    }

    @Property(viewable = true, order = 2)
    public String getContext()
    {
        return context;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    @Nullable
    @Override
    public String getDescription() {
        return comment;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public JDBCDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public boolean isGrantOption()
    {
        return name.equalsIgnoreCase(GRANT_PRIVILEGE);
    }
}
