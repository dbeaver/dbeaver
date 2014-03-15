/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Row data
 */
public class RowData {

    static final byte STATE_NORMAL = 1;
    static final byte STATE_ADDED = 2;
    static final byte STATE_REMOVED = 3;

    // Physical row number
    public int rowNumber;
    // Row number in grid
    public int visualNumber;
    // Column values
    @NotNull
    public Object[] values;
    // Original column values
    @Nullable
    public Object[] oldValues;
    @Nullable
    public boolean[] changedValues;
    // Use info
    @Nullable
    public Object info;
    // Row state
    public byte state;

    RowData(int rowNumber, @NotNull Object[] values, @Nullable Object info) {
        this.rowNumber = rowNumber;
        this.visualNumber = rowNumber;
        this.values = values;
        this.info = info;
        this.state = STATE_NORMAL;
    }

    public boolean isChanged() {
        if (changedValues != null) {
            for (boolean cv : changedValues) {
                if (cv) {
                    return true;
                }
            }
        }
        return false;
    }
}
