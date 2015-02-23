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

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

public abstract class JDBCLazyDataTypeCache extends AbstractObjectCache<JDBCDataSource, DBSDataType> {
    protected final DBSObject owner;

    public JDBCLazyDataTypeCache(DBSObject owner)
    {
        this.owner = owner;
    }

    protected abstract DBSDataType createDataType(
        int valueType,
        String name,
        String remarks,
        boolean unsigned,
        boolean searchable,
        int precision,
        int minScale,
        int maxScale);

    @Override
    public Collection<DBSDataType> getObjects(DBRProgressMonitor monitor, JDBCDataSource jdbcDataSource) throws DBException
    {
        return getCachedObjects();
    }

    @Override
    public DBSDataType getObject(DBRProgressMonitor monitor, JDBCDataSource jdbcDataSource, String name) throws DBException
    {
        return getCachedObject(name);
    }

    public DBSDataType getDataType(String name, int valueType)
    {
        return null;
    }

}
