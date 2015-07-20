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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;

/**
 * Meta object
 */
public interface DBSObject extends DBPNamedObject, DBPPersistedObject
{

    /**
     * Object description
     *
     * @return object description or null
     */
    @Nullable
    String getDescription();

    /**
     * Parent object
     *
     * @return parent object or null
     */
    @Nullable
	DBSObject getParentObject();

    /**
     * Datasource which this object belongs.
     * It can be null if object was detached from data source.
     * @return datasource reference or null
     */
    @NotNull
    DBPDataSource getDataSource();

}
