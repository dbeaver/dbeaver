/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.jkiss.code.NotNull;

import java.util.Objects;

/**
 * GridCell
 */
public class GridCell implements IGridCell {
    public final IGridColumn col;
    public final IGridRow row;

    public GridCell(@NotNull IGridColumn col, @NotNull IGridRow row) {
        this.col = col;
        this.row = row;
    }

    @Override
    public IGridRow getRow() {
        return row;
    }

    @Override
    public IGridColumn getColumn() {
        return col;
    }

    public String toString() {
        return col + ":" + row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridCell gridCell = (GridCell) o;
        return col.equals(gridCell.col) && row.equals(gridCell.row);
    }

    @Override
    public int hashCode() {
        return Objects.hash(col, row);
    }
}
