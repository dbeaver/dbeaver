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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * GenericDataType
 */
public class GenericDataTypeArray extends GenericDataType
{
    private DBSDataType componentType;

    public GenericDataTypeArray(GenericStructContainer owner, int valueType, String name, @Nullable String remarks, DBSDataType componentDataType) {
        super(owner, valueType, name, remarks, false, false, 0, 0, 0);
        this.componentType = componentDataType;
    }

    public GenericDataTypeArray(GenericStructContainer owner, DBSTypedObject typed) {
        super(owner, typed);
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return componentType;
    }

    public void setComponentType(DBSDataType componentType) {
        this.componentType = componentType;
    }
}
