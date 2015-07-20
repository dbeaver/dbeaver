/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.info;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * DB2 XML Strings
 * 
 * @author Denis Forveille
 */
public class DB2XMLString implements DBSObject {

    private DB2DataSource dataSource;

    private Integer stringId;
    private String string;
    private String stringUTF8;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2XMLString(DB2DataSource dataSource, ResultSet dbResult)
    {
        this.dataSource = dataSource;

        this.stringId = JDBCUtils.safeGetInteger(dbResult, "STRINGID");
        this.string = JDBCUtils.safeGetString(dbResult, "STRING");
        this.stringUTF8 = JDBCUtils.safeGetString(dbResult, "STRING_UTF8");
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @Override
    public boolean isPersisted()
    {
        return false;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return stringId.toString();
    }

    @Property(viewable = true, order = 2)
    public String getString()
    {
        return string;
    }

    @Property(viewable = false, order = 3)
    public String getStringUTF8()
    {
        return stringUTF8;
    }

}
