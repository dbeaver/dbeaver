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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseDataSource;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseMapType;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.data.AbstractDatabaseList;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.impl.struct.AbstractStructDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickhouseMapValue extends AbstractDatabaseList {
    private final ClickhouseDataSource dataSource;
    private final ClickhouseMapType mapType;
    private final EntryType entryType;
    private final EntryAttribute[] attributes;
    private List<EntryComposite> contents;
    private boolean modified;

    public ClickhouseMapValue(
        @NotNull ClickhouseDataSource dataSource,
        @NotNull ClickhouseMapType type,
        @NotNull Map<?, ?> contents
    ) {
        this.dataSource = dataSource;
        this.mapType = type;
        this.entryType = new EntryType(dataSource);
        this.attributes = new EntryAttribute[]{
            new EntryAttribute(entryType, "Key", type.getKeyType(), 0),
            new EntryAttribute(entryType, "Value", type.getValueType(), 1)
        };
        this.contents = contents.entrySet().stream()
            .map(entry -> new EntryComposite(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public DBSDataType getComponentType() {
        return entryType;
    }

    @NotNull
    @Override
    public DBDValueHandler getComponentValueHandler() {
        return DBUtils.findValueHandler(dataSource, entryType);
    }

    @Override
    public int getItemCount() {
        return contents != null ? contents.size() : 0;
    }

    @Override
    public Object getItem(int index) {
        return contents.get(index);
    }

    @Override
    public void setItem(int index, Object value) {
        contents.set(index, (EntryComposite) value);
        modified = true;
    }

    @Override
    public void setContents(Object[] contents) {
        this.contents.clear();
        this.modified = true;

        for (Object content : contents) {
            this.contents.add((EntryComposite) content);
        }
    }

    @Override
    public Object getRawValue() {
        return contents;
    }

    @Override
    public boolean isNull() {
        return contents == null;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void release() {
        contents = null;
    }

    private class EntryType extends AbstractStructDataType<ClickhouseDataSource> implements DBSEntity {
        public EntryType(@NotNull ClickhouseDataSource dataSource) {
            super(dataSource);
        }

        @NotNull
        @Override
        public String getTypeName() {
            return mapType.getFullTypeName();
        }

        @Override
        public int getTypeID() {
            return mapType.getTypeID();
        }

        @NotNull
        @Override
        public DBPDataKind getDataKind() {
            return DBPDataKind.STRUCT;
        }

        @NotNull
        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.VIRTUAL_ENTITY;
        }

        @Nullable
        @Override
        public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
            return List.of();
        }
    }

    private static class EntryAttribute extends AbstractAttribute implements DBSEntityAttribute {
        private final EntryType parent;
        private final DBSDataType type;

        public EntryAttribute(@NotNull EntryType parent, @NotNull String name, @NotNull DBSDataType type, int position) {
            super(name, type.getFullTypeName(), type.getTypeID(), position, 0, null, null, true, false);
            this.parent = parent;
            this.type = type;
        }

        @Nullable
        @Override
        public String getDefaultValue() {
            return null;
        }

        @NotNull
        @Override
        public DBSEntity getParentObject() {
            return parent;
        }

        @Override
        public DBPDataSource getDataSource() {
            return parent.getDataSource();
        }

        @NotNull
        @Override
        public DBPDataKind getDataKind() {
            return type.getDataKind();
        }
    }

    private class EntryComposite implements DBDComposite {
        private final Object key;
        private Object value;

        public EntryComposite(@Nullable Object key, @Nullable Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public DBSDataType getDataType() {
            return entryType;
        }

        @NotNull
        @Override
        public DBSAttributeBase[] getAttributes() {
            return attributes;
        }

        @Nullable
        @Override
        public Object getAttributeValue(@NotNull DBSAttributeBase attribute) {
            if (attribute.getOrdinalPosition() == 0) {
                return key;
            } else {
                return value;
            }
        }

        @Override
        public void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value) throws DBCException {
            if (attribute.getOrdinalPosition() == 0) {
                throw new DBCException("Key is read-only");
            } else {
                this.value = value;
                modified = true;
            }
        }

        @Override
        public Object getRawValue() {
            return value;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isModified() {
            return modified;
        }

        @Override
        public void release() {
            // do nothing
        }
    }
}
