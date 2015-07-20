/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

/**
 * MySQLPrivilege
 */
public class MySQLPrivilege implements DBAPrivilege
{
    static final Log log = Log.getLog(MySQLPrivilege.class);

    public static final String GRANT_PRIVILEGE = "Grant Option";
    public static final String ALL_PRIVILEGES = "All Privileges";

    public static enum Kind {
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

        if (context.contains("Admin")) {
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
