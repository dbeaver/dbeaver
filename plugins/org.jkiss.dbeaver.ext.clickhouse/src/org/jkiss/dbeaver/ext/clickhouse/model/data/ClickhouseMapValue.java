/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseDataSource;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseMapType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.data.AbstractDatabaseList;
import org.jkiss.dbeaver.model.struct.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickhouseMapValue extends AbstractDatabaseList {
    private final ClickhouseDataSource dataSource;
    private final ClickhouseMapType mapType;
    private final DBSAttributeBase[] entryAttributes;
    private List<EntryComposite> contents;
    private boolean modified;

    public ClickhouseMapValue(
        @NotNull ClickhouseDataSource dataSource,
        @NotNull ClickhouseMapType type,
        @NotNull List<DBSAttributeBase> entryAttributes,
        @NotNull Map<?, ?> contents
    ) {
        this.dataSource = dataSource;
        this.mapType = type;
        this.entryAttributes = entryAttributes.toArray(DBSAttributeBase[]::new);
        this.contents = contents.entrySet().stream()
            .map(entry -> new EntryComposite(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public DBSDataType getComponentType() {
        return mapType.getComponentType();
    }

    @NotNull
    @Override
    public DBDValueHandler getComponentValueHandler() {
        return DBUtils.findValueHandler(dataSource, mapType.getComponentType());
    }

    @Override
    public int getItemCount() {
        return contents != null ? contents.size() : 0;
    }

    @Nullable
    @Override
    public Object getItem(int index) {
        return contents.get(index);
    }

    @Override
    public void setItem(int index, @Nullable Object value) {
        contents.set(index, (EntryComposite) value);
        modified = true;
    }

    @Override
    public void setContents(@NotNull Object[] contents) {
        this.contents.clear();
        this.modified = true;

        for (Object content : contents) {
            this.contents.add((EntryComposite) content);
        }
    }

    @Nullable
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

    @Override
    public String toString() {
        return contents.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
    }

    private class EntryComposite implements DBDComposite {
        private final Object key;
        private Object value;

        public EntryComposite(@Nullable Object key, @Nullable Object value) {
            this.key = key;
            this.value = value;
        }

        @NotNull
        @Override
        public DBSDataType getDataType() {
            return mapType.getComponentType();
        }

        @NotNull
        @Override
        public DBSAttributeBase[] getAttributes() {
            return entryAttributes;
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

        @Nullable
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

        @Override
        public String toString() {
            return String.format("{%s : %s}", key, value);
        }
    }
}
