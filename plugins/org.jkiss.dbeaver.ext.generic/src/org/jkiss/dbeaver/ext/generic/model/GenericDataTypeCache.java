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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;

/**
 * GenericDataTypeCache
 */
public class GenericDataTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, GenericDataType>
{

    public GenericDataTypeCache(GenericStructContainer owner) {
        super(owner);

        // Ignore abstract types. There can be multiple numeric types with the same name
        // but different scale/precision properties
        ignoredTypes.add("NUMBER");
        ignoredTypes.add("NUMERIC");
    }

    @NotNull
    @Override
    protected GenericDataType makeDataType(@NotNull JDBCResultSet dbResult, String name, int valueType) {
        return new GenericDataType(
                owner,
                valueType,
                name,
                JDBCUtils.safeGetString(dbResult, JDBCConstants.LOCAL_TYPE_NAME),
                JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.UNSIGNED_ATTRIBUTE),
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.SEARCHABLE) != 0,
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION),
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.MINIMUM_SCALE),
                JDBCUtils.safeGetInt(dbResult, JDBCConstants.MAXIMUM_SCALE));
    }
}
