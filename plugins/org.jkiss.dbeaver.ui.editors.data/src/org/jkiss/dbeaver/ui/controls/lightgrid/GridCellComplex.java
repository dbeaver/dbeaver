/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.code.Nullable;

public class GridCellComplex implements IGridCell {
    private final Object col;
    private final LightGrid.VirtualRow row;

    public GridCellComplex(@Nullable Object col, @NotNull LightGrid.VirtualRow row) {
        this.col = col;
        this.row = row;
    }

    @Nullable
    @Override
    public Object getRowElement() {
        return row.node.source;
    }

    @Nullable
    @Override
    public Object getColumnElement() {
        return col;
    }

    @Nullable
    public Object getData() {
        return row.columns.get(col);
    }

    public boolean isValid() {
        return row.columns.containsKey(col);
    }
}
