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

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.Collection;

/**
 * Data type provider
 */
public interface DBPDataTypeProvider
{
    /**
     * Determines data kind by type name and/or ID
     * @param typeName type name
     * @param typeID type ID
     * @return data kind or null if type can't be resolved
     */
    DBPDataKind resolveDataKind(String typeName, int typeID);

    /**
     * Resolve data type by it's full name.
     * @param monitor progress monitor
     * @param typeFullName full qualified type name
     * @return data type or null if type not found
     * @throws DBException on any DB access error
     */
    DBSDataType resolveDataType(DBRProgressMonitor monitor, String typeFullName)
        throws DBException;

    /**
     * Retrieves list of supported datatypes
     * @return list of types
     */
    Collection<? extends DBSDataType> getDataTypes();

    /**
     * Gets data type with specified name
     *
     * @param typeName type name
     * @return data type of null
     */
    DBSDataType getDataType(String typeName);

    /**
     * Returns name of default data type for specified data kind
     * @param dataKind data kind
     * @return data type name or null if data kind not supported
     */
    String getDefaultDataTypeName(DBPDataKind dataKind);
}
