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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTupleType;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;

public class ClickhouseTupleValue implements DBDComposite, DBDValueCloneable {
    private final ClickhouseTupleType type;
    private Object[] values;
    private boolean modified;

    public ClickhouseTupleValue(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ClickhouseTupleType type,
        @Nullable Object[] values
    ) throws DBCException {
        this.type = type;

        if (values != null) {
            this.values = new Object[values.length];

            for (int i = 0; i < values.length; i++) {
                final Object value = values[i];

                if (value instanceof DBDValueCloneable) {
                    this.values[i] = ((DBDValueCloneable) value).cloneValue(monitor);
                } else {
                    this.values[i] = value;
                }
            }
        }
    }

    @Override
    public DBSDataType getDataType() {
        return type;
    }

    @NotNull
    @Override
    public DBSAttributeBase[] getAttributes() {
        return type.getAttributes().toArray(DBSAttributeBase[]::new);
    }

    @Nullable
    @Override
    public Object getAttributeValue(@NotNull DBSAttributeBase attribute) {
        if (values == null) {
            return null;
        } else {
            return values[attribute.getOrdinalPosition()];
        }
    }

    @Override
    public void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value) {
        if (values == null) {
            values = new Object[type.getAttributes().size()];
        }

        values[attribute.getOrdinalPosition()] = value;
        modified = true;
    }

    @Override
    public Object getRawValue() {
        return values;
    }

    @Override
    public boolean isNull() {
        return values == null;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void release() {
        values = null;
    }

    @Override
    public DBDValueCloneable cloneValue(DBRProgressMonitor monitor) throws DBCException {
        return new ClickhouseTupleValue(monitor, type, values);
    }
}
