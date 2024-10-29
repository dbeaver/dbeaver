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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.utils.ArrayUtils;

import java.util.List;

/**
 * Resultset cell location
 */
public class ResultSetCellLocation {

    private final DBDAttributeBinding attribute;
    private final ResultSetRow row;
    private final int[] rowIndexes;

    public ResultSetCellLocation(@NotNull DBDAttributeBinding attribute, @NotNull ResultSetRow row) {
        this(attribute, row, null);
    }

    public ResultSetCellLocation(@NotNull DBDAttributeBinding attribute, @NotNull ResultSetRow row, @Nullable int[] rowIndexes) {
        this.attribute = getLeafAttribute(attribute, rowIndexes);
        this.row = row;
        this.rowIndexes = rowIndexes;
    }

    @NotNull
    public DBDAttributeBinding getAttribute() {
        return attribute;
    }

    @NotNull
    public ResultSetRow getRow() {
        return row;
    }

    @Nullable
    public int[] getRowIndexes() {
        return rowIndexes;
    }

    @NotNull
    public static DBDAttributeBinding getLeafAttribute(@NotNull DBDAttributeBinding attribute, @Nullable int[] indexes) {
        DBDAttributeBinding leaf = attribute;

        if (leaf.getDataKind() == DBPDataKind.STRUCT && !ArrayUtils.isEmpty(indexes)) {
            for (int index : indexes) {
                final List<DBDAttributeBinding> nestedBindings = leaf.getNestedBindings();
                if (nestedBindings == null || index >= nestedBindings.size()) {
                    break;
                }
                leaf = nestedBindings.get(index);
            }
        }

        return leaf;
    }
}
