/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
 * DB2 Database and Instance parameters
 * 
 * @author Denis Forveille
 */
public class DB2Parameter implements DBSObject {

    private DB2DataSource dataSource;

    private String name;
    private String value;
    private String flags;
    private String defferedValue;
    private String defferedValueFlags;
    private String dataType;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2Parameter(DB2DataSource dataSource, ResultSet dbResult)
    {
        this.dataSource = dataSource;

        this.name = JDBCUtils.safeGetString(dbResult, "NAME");
        this.value = JDBCUtils.safeGetString(dbResult, "VALUE");
        this.flags = JDBCUtils.safeGetString(dbResult, "VALUE_FLAGS");
        this.defferedValue = JDBCUtils.safeGetString(dbResult, "DEFERRED_VALUE");
        // DB2 v10.1 this.defferedValueFlags = JDBCUtils.safeGetString(dbResult, "DEFERRED_VALUE_FLAGS ");
        this.dataType = JDBCUtils.safeGetString(dbResult, "DATATYPE");
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
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public String getValue()
    {
        return value;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getFlags()
    {
        return flags;
    }

    @Property(viewable = true, editable = false, order = 4)
    public String getDefferedValue()
    {
        return defferedValue;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getDefferedValueFlags()
    {
        return defferedValueFlags;
    }

    @Property(viewable = true, editable = false, order = 6)
    public String getDataType()
    {
        return dataType;
    }

}
