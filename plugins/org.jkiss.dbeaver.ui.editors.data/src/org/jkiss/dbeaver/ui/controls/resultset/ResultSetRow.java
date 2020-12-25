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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDValue;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Row data
 */
public class ResultSetRow {

    public static final byte STATE_NORMAL = 1;
    public static final byte STATE_ADDED = 2;
    public static final byte STATE_REMOVED = 3;

    public static class ColorInfo {
        @Nullable
        public Color rowForeground;
        @Nullable
        public Color rowBackground;
        @Nullable
        public Color[] cellFgColors;
        @Nullable
        public Color[] cellBgColors;
    }

    // Physical row number
    private int rowNumber;
    // Row number in grid
    private int visualNumber;
    // Column values
    @NotNull
    public Object[] values;
    @Nullable
    public Map<DBDAttributeBinding, Object> changes;
    // Row state
    private byte state;
    @Nullable
    public Map<DBDValue, CollectionElementData> collections;
    @Nullable
    public ColorInfo colorInfo;

    ResultSetRow(int rowNumber, @NotNull Object[] values) {
        this.rowNumber = rowNumber;
        this.visualNumber = rowNumber;
        this.values = values;
        this.state = STATE_NORMAL;
    }

    @NotNull
    public Object[] getValues() {
        return values;
    }

    public boolean isChanged() {
        return changes != null && !changes.isEmpty();
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getVisualNumber() {
        return visualNumber;
    }

    public void setVisualNumber(int visualNumber) {
        this.visualNumber = visualNumber;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    boolean isChanged(DBDAttributeBinding attr) {
        return changes != null && changes.containsKey(attr);
    }

    public void addChange(DBDAttributeBinding attr, @Nullable Object oldValue) {
        if (changes == null) {
            changes = new IdentityHashMap<>();
        }
        changes.put(attr, oldValue);
    }

    public void resetChange(DBDAttributeBinding attr) {
        assert changes != null;
        changes.remove(attr);
        if (changes.isEmpty()) {
            changes = null;
        }
    }

    void release() {
        for (Object value : values) {
            DBUtils.releaseValue(value);
        }
        if (changes != null) {
            for (Object oldValue : changes.values()) {
                DBUtils.releaseValue(oldValue);
            }
        }
    }

    @NotNull
    public CollectionElementData getCollectionData(DBDAttributeBinding binding, DBDCollection collection) {
        if (collections == null) {
            collections = new HashMap<>();
        }
        CollectionElementData ced = collections.get(collection);
        if (ced == null) {
            ced = new CollectionElementData(binding, collection);
            collections.put(collection, ced);
        }
        return ced;
    }

    @Override
    public String toString() {
        return String.valueOf(rowNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResultSetRow) {
            ResultSetRow row = (ResultSetRow)obj;
            return
                this.rowNumber == row.rowNumber &&
                this.visualNumber == row.visualNumber;

        }
        return super.equals(obj);
    }
}
