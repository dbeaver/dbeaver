/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeArray;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

class ClickhouseDataTypeCache extends GenericDataTypeCache {

    public ClickhouseDataTypeCache(GenericStructContainer container) {
        super(container);
    }

    @Override
    protected void addCustomObjects(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer owner, @NotNull List<GenericDataType> genericDataTypes) {
        if (DBUtils.findObject(genericDataTypes, "Int128") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "Int128", "Int128", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "Int256") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "Int256", "Int256", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "UInt128") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "UInt128", "UInt128", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "UInt256") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.NUMERIC, "UInt256", "UInt256", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "Decimal") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.DECIMAL, "Decimal", "Decimal", false, false, 0, 0, 0));
        }
        if (DBUtils.findObject(genericDataTypes, "Bool") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.BOOLEAN, "Bool", "Bool", false, false, 0, 0, 0));
        }
        // Add array data types
        for (GenericDataType dt : new ArrayList<>(genericDataTypes)) {
            genericDataTypes.add(new GenericDataTypeArray(dt.getParentObject(), Types.ARRAY, "Array(" + dt.getName() + ")", "Array of " + dt.getName(), dt));
        }
        // Driver error - missing data types
        if (DBUtils.findObject(genericDataTypes, "DateTime64") == null) {
            genericDataTypes.add(new GenericDataType(this.owner, Types.TIMESTAMP, "DateTime64", "DateTime64", false, false, 0, 0, 0));
        }
    }

}
