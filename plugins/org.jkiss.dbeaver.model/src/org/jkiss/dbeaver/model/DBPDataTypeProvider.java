/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
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
    @NotNull
    DBPDataKind resolveDataKind(@NotNull String typeName, int typeID);

    /**
     * Resolve data type by it's full name.
     * @param monitor progress monitor
     * @param typeFullName full qualified type name
     * @return data type or null if type not found
     * @throws DBException on any DB access error
     */
    DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName)
        throws DBException;

    /**
     * Retrieves list of supported datatypes.
     * @return list of types
     */
    Collection<? extends DBSDataType> getLocalDataTypes();

    /**
     * Gets data type with specified name
     *
     * @param typeName type name
     * @return data type of null
     */
    DBSDataType getLocalDataType(String typeName);

    /**
     * Gets data type with specified type id
     *
     * @return data type of null
     */
    DBSDataType getLocalDataType(int typeID);

    /**
     * Returns name of default data type for specified data kind
     * @param dataKind data kind
     * @return data type name or null if data kind not supported
     */
    String getDefaultDataTypeName(@NotNull DBPDataKind dataKind);
}
