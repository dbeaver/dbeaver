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
package org.jkiss.dbeaver.ext.databricks.model.types;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.data.BaseValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.*;
import java.util.function.Function;

public class DatabricksMapValueHandler extends BaseValueHandler {

    public static final DatabricksMapValueHandler INSTANCE = new DatabricksMapValueHandler();

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return JDBCCollection.class;
    }

    @Nullable
    @Override
    public Object fetchValueObject(
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException {
        return this.getValueFromObject(session, type, resultSet.getAttributeValue(index), false, false);
    }

    @Override
    public void bindValueObject(
        @NotNull DBCSession session,
        @NotNull DBCStatement statement,
        @NotNull DBSTypedObject type,
        int index,
        @Nullable Object value
    ) throws DBCException {
        throw new DBCFeatureNotSupportedException("Immediate MAP<,> value binding not supported");
    }

    @Nullable
    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (object == null) {
            return null;
        } else if (object instanceof Map<?, ?> m && session.getDataSource() instanceof DBPDataTypeProvider p) {
            try {
                if (p.resolveDataType(session.getProgressMonitor(), type.getFullTypeName()) instanceof DatabricksMapDataType t) {
                    DatabricksMapDataType.FakeEntryType entryType = t.getEntryType();

                    DBDValueHandler keyHandler = DBUtils.findValueHandler(session, t.getKeyType());
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(session, t.getValueType());
                    List<EntryComposite> items = new ArrayList<>();
                    for (Map.Entry<?, ?> kv : m.entrySet()) {
                        items.add(new EntryComposite(
                            Objects.requireNonNull(keyHandler.getValueFromObject(session, t.getKeyType(), kv.getKey(), false, true)),
                            valueHandler.getValueFromObject(session, t.getValueType(), kv.getValue(), false, true),
                            entryType
                        ));
                    }
                    items.sort(Comparator.comparing(Function.identity()));

                    DBDValueHandler entryHandler = DBUtils.findValueHandler(session, entryType);
                    return new JDBCCollection(new VoidProgressMonitor(), entryType, entryHandler, items.toArray());
                } else {
                    throw new DBCException("Failed to resolve MAP<,> type: " + type.getFullTypeName());
                }
            } catch (DBException e) {
                throw new DBCException("Failed to handle MAP<,> value of type " + object.getClass().getName() + ": " + object, e);
            }
        } else {
            throw new DBCException("Unsupported MAP<,> type: " + object.getClass().getName());
        }
    }

    private static class EntryComposite implements DBDComposite, Comparable<EntryComposite> {

        private final Object key;
        private Object value;
        private final DatabricksMapDataType.FakeEntryType itemType;
        private boolean modified;

        private EntryComposite(@NotNull Object key, @Nullable Object value, @NotNull DatabricksMapDataType.FakeEntryType itemType) {
            this.key = key;
            this.value = value;
            this.itemType = itemType;
            this.modified = false;
        }

        @NotNull
        @Override
        public DBSDataType getDataType() {
            return this.itemType;
        }

        @NotNull
        @Override
        public DBSAttributeBase[] getAttributes() {
            return this.itemType.getAttributes();
        }

        @Nullable
        @Override
        public Object getAttributeValue(@NotNull DBSAttributeBase attribute) throws DBCException {
            if (attribute.getOrdinalPosition() == 0) {
                return key;
            } else {
                return value;
            }
        }

        @Override
        public void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object attrValue) throws DBCException {
            if (attribute.getOrdinalPosition() == 0) {
                throw new IllegalArgumentException("Key is read-only");
            } else {
                value = attrValue;
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

        }

        @NotNull
        @Override
        public String toString() {
            return key + "=" + value;
        }

        @Override
        public int compareTo(@Nullable EntryComposite other) {
            return other == null
                ? 1
                : (
                    this.key instanceof Comparable<?> thisKey &&
                    other.key instanceof Comparable<?> otherKey &&
                    other.key.getClass() == this.key.getClass()
                        ? ((Comparable<Object>) thisKey).compareTo(otherKey)
                        : this.key.getClass().getName().compareTo(other.key.getClass().getName())
                );
        }
    }
}
