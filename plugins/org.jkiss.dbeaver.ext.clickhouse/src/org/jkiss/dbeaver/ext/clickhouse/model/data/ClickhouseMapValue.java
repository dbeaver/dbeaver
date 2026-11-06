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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.data.AbstractDatabaseList;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickhouseMapValue extends AbstractDatabaseList implements DBDValueCloneable {
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
        // FIXME: The ClickHouse JDBC driver returns a Map and has already discarded duplicate keys here.
        // https://github.com/ClickHouse/clickhouse-java/issues/3047
        this.contents = contents.entrySet().stream()
            .map(entry -> new EntryComposite(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    private ClickhouseMapValue(@NotNull ClickhouseMapValue source, @NotNull DBRProgressMonitor monitor) throws DBCException {
        this.dataSource = source.dataSource;
        this.mapType = source.mapType;
        this.entryAttributes = source.entryAttributes;
        this.contents = new ArrayList<>(source.contents.size());
        for (EntryComposite entry : source.contents) {
            Object key = entry.key instanceof DBDValueCloneable cloneable ? cloneable.cloneValue(monitor) : entry.key;
            Object value = entry.value instanceof DBDValueCloneable cloneable ? cloneable.cloneValue(monitor) : entry.value;
            this.contents.add(new EntryComposite(key, value));
        }
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

    @NotNull
    ClickhouseMapType getMapType() {
        return mapType;
    }

    @NotNull
    List<Map.Entry<Object, Object>> getEntries() {
        List<Map.Entry<Object, Object>> entries = new ArrayList<>(contents.size());
        for (EntryComposite entry : contents) {
            entries.add(new AbstractMap.SimpleImmutableEntry<>(entry.key, entry.value));
        }
        return entries;
    }

    @NotNull
    Map<Object, Object> toJdbcMap() throws DBCException, SQLException {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (EntryComposite entry : contents) {
            Object key = ClickhouseStructValueHandler.toJdbcValue(entry.key, mapType.getKeyType());
            if (map.containsKey(key)) {
                throw new DBCException(
                    "ClickHouse JDBC driver loses duplicate Map keys (clickhouse-java#3047); cannot bind this value safely"
                );
            }
            map.put(key, ClickhouseStructValueHandler.toJdbcValue(entry.value, mapType.getValueType()));
        }
        return map;
    }

    @Override
    public boolean isNull() {
        return contents == null;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @NotNull
    @Override
    public ClickhouseMapValue cloneValue(@NotNull DBRProgressMonitor monitor) throws DBCException {
        return new ClickhouseMapValue(this, monitor);
    }

    @Override
    public void release() {
        contents = null;
    }

    @NotNull
    @Override
    public String toString() {
        if (contents == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        return contents.stream()
            .map(String::valueOf)
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

        @NotNull
        @Override
        public String toString() {
            return String.format("{%s : %s}", key, value);
        }
    }
}
