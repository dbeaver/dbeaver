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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * AbstractAttribute
 */
public abstract class AbstractDataType<DS extends DBPDataSource> implements DBSDataType
{
    private final DS dataSource;

    public AbstractDataType(DS dataSource) {
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    public String getName() {
        return getTypeName();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public DS getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public int getMinScale() {
        return 0;
    }

    @Override
    public int getMaxScale() {
        return 0;
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators() {
        return DBUtils.getDefaultOperators(this);
    }

    @Override
    public int getPrecision() {
        return 0;
    }

    @Override
    public long getMaxLength() {
        return 0;
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return null;
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBCException {
        return null;
    }

    @Override
    public String toString() {
        return getTypeName();
    }
}
