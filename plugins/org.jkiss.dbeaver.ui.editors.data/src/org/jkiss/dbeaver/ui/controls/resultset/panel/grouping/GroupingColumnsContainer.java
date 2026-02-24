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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.GroupingColumn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class GroupingColumnsContainer {

    @NotNull
    private final List<GroupingColumn> groupColumns = new ArrayList<>();

    private final GroupingDataContainer dataContainer;

    public GroupingColumnsContainer(@NotNull GroupingDataContainer dataContainer) {
        this.dataContainer = dataContainer;
    }

    public void addColumn(@NotNull GroupingColumn column) {
        if (column.canBeAdded()) {
            groupColumns.add(column);
        }
    }

    @NotNull
    public GroupingColumn getColumn(int index) {
        return groupColumns.get(index);
    }

    public <T extends GroupingColumn> List<T> getColumnsByType(Class<T> type) {
        return groupColumns
            .stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    @NotNull
    public List<GroupingColumn> getColumns() {
        return new ArrayList<>(groupColumns);
    }

    public List<Integer> getIndexesByType(@NotNull Class<? extends GroupingColumn> columnType) {
        List<Integer> columnsOfType = new ArrayList<>();
        for (int i = 0; i < groupColumns.size(); i++) {
            if (columnType.isInstance(groupColumns.get(i))) {
                columnsOfType.add(i);
            }
        }
        return columnsOfType;
    }

    public boolean removeColumnsByIndexes(@NotNull List<Integer> indexesToRemove) {
        TreeSet<Integer> indexesSortedDesc = new TreeSet<>(Comparator.reverseOrder());
        indexesSortedDesc.addAll(indexesToRemove);
        return removeColumnsByIndexesSortedDesc(indexesSortedDesc);
    }

    public boolean removeColumnsByIndexesSortedDesc(@NotNull TreeSet<Integer> columnsToRemove) {
        boolean isRemoved = false;
        for (Integer index : columnsToRemove) {
            if (removeColumn(index)) {
                isRemoved = true;
            }
        }
        return isRemoved;
    }

    public void clear() {
        groupColumns.clear();
    }

    public boolean isEmpty() {
        return groupColumns.isEmpty();
    }

    public int size() {
        return groupColumns.size();
    }


    public void moveColumns(int overColumnIndex, @NotNull List<Integer> indexesToMove) {
        if (overColumnIndex < 0) {
            overColumnIndex = 0;
        } else if (overColumnIndex >= groupColumns.size()) {
            overColumnIndex = groupColumns.size() - 1;
        }

        TreeSet<Integer> indexesSortedDesc = new TreeSet<>(Comparator.reverseOrder());
        indexesSortedDesc.addAll(indexesToMove);

        List<GroupingColumn> removedElements = new ArrayList<>(indexesSortedDesc.size());
        for (Integer index : indexesSortedDesc) {
            removedElements.addFirst(groupColumns.get(index));
        }
        groupColumns.addAll(overColumnIndex, removedElements);
    }


    private boolean removeColumn(int index) {
        return groupColumns.get(index).canBeRemoved()
            && groupColumns.remove(index).afterDeleteAction(index);
    }
}
