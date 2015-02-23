/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract DB2 schema object
 * 
 * @author Denis Forveille
 */
public abstract class DB2GlobalObject implements DBSObject, DBPSaveableObject {
    protected Log log = Log.getLog(DB2GlobalObject.class);

    private final DB2DataSource dataSource;
    private boolean persisted;

    // -----------------------
    // Constructors
    // -----------------------

    protected DB2GlobalObject(DB2DataSource dataSource, boolean persisted)
    {
        this.dataSource = dataSource;
        this.persisted = persisted;
    }

    // By default : no Description
    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    // -----------------------
    // Standard Getters/Setters
    // -----------------------

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}
