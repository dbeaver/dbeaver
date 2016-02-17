/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
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

    @NotNull
    @Override
    public Collection<DBSDataType> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable JDBCDataSource jdbcDataSource) throws DBException
    {
        return getCachedObjects();
    }

    @Override
    public DBSDataType getObject(@NotNull DBRProgressMonitor monitor, @Nullable JDBCDataSource jdbcDataSource, @NotNull String name) throws DBException
    {
        return getCachedObject(name);
    }

    public DBSDataType getDataType(String name, int valueType)
    {
        return null;
    }

}
