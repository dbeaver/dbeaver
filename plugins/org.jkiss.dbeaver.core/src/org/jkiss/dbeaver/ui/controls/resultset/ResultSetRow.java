/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    public Color foreground, background;

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

    void addChange(DBDAttributeBinding attr, @Nullable Object oldValue) {
        if (changes == null) {
            changes = new IdentityHashMap<>();
        }
        changes.put(attr, oldValue);
    }

    void resetChange(DBDAttributeBinding attr) {
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
}
