/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQLCollation
 */
public class MySQLCollation extends MySQLInformation {

    private MySQLCharset charset;
    private int id;
    private String name;
    private boolean isDefault;
    private boolean isCompiled;
    private int sortLength;

    public MySQLCollation(MySQLCharset charset, ResultSet dbResult)
        throws SQLException
    {
        super(charset.getDataSource());
        this.charset = charset;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION);
        this.id = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ID);
        this.isDefault = "Yes".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT));
        this.isCompiled = "Yes".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COMPILED));
        this.sortLength = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SORT_LENGTH);
    }

    @Property(viewable = true, order = 1)
    public MySQLCharset getCharset()
    {
        return charset;
    }

    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 3)
    public int getId()
    {
        return id;
    }

    @Property(viewable = true, order = 4)
    public boolean isDefault()
    {
        return isDefault;
    }

    @Property(viewable = true, order = 5)
    public boolean isCompiled()
    {
        return isCompiled;
    }

    @Property(viewable = true, order = 6)
    public int getSortLength()
    {
        return sortLength;
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
        return charset;
    }
}
