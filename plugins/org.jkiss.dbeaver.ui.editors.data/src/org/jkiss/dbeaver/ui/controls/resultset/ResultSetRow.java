/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.data.DBDValueRow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Row data
 */
public class ResultSetRow implements DBDValueRow {

    public static final byte STATE_NORMAL = 1;
    public static final byte STATE_ADDED = 2;
    public static final byte STATE_REMOVED = 3;

    private final Map<DBDAttributeBinding, Object> changes = new HashMap<>();

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

    // Row state
    private byte state;
    @Nullable
    public ColorInfo colorInfo;

    ResultSetRow(int rowNumber, @NotNull Object[] values) {
        this.rowNumber = rowNumber;
        this.visualNumber = rowNumber;
        this.values = values;
        this.state = STATE_NORMAL;
    }

    @Override
    @NotNull
    public Object[] getValues() {
        return values;
    }

    public boolean isChanged() {
        return !changes.isEmpty();
    }

    public boolean isChanged(@Nullable DBDAttributeBinding attr) {
        return attr != null && changes.containsKey(attr);
    }

    public int getChangesCount() {
        return changes.size();
    }

    @NotNull
    public Collection<DBDAttributeBinding> getChangedAttributes() {
        return changes.keySet();
    }

    @Override
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

    public void addChange(@NotNull DBDAttributeBinding attr, @Nullable Object oldValue) {
        changes.put(attr, oldValue);
    }


    @Nullable
    public Object getChange(@NotNull DBDAttributeBinding attr) {
        return changes.get(attr);
    }

    @NotNull
    public Iterable<Map.Entry<DBDAttributeBinding, Object>> getChanges() {
        return () -> changes.entrySet().iterator();
    }

    public void clearChange(@NotNull DBDAttributeBinding attr) {
        changes.remove(attr);
        // We reset entire row changes. Cleanup all references on the same top attribute
        changes.entrySet().removeIf(entry -> attr.equals(entry.getValue()));
    }

    public void clearChanges() {
        changes.clear();
    }

    void release() {
        for (Object value : values) {
            DBUtils.releaseValue(value);
        }
        for (Object oldValue : changes.values()) {
            DBUtils.releaseValue(oldValue);
        }
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
