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
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.GroupingColumn;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.GroupingFunctionColumn;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.SQLGroupingAttributeGroupingColumn;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.TransformerGroupingFunctionColumn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class GroupingColumnsContainer {

    @NotNull
    private final List<SQLGroupingAttributeGroupingColumn> attributes = new ArrayList<>();

    @NotNull
    private final List<GroupingFunctionColumn> groupFunctions = new ArrayList<>();

    private final GroupingDataContainer dataContainer;

    public GroupingColumnsContainer(@NotNull GroupingDataContainer dataContainer) {
        this.dataContainer = dataContainer;
    }

    public void addFunction(@NotNull GroupingFunctionColumn functionColumn) {
        if (!functionColumn.mustBeUniqueByName() || isUniqueFunctionByName(functionColumn)) {
            groupFunctions.add(functionColumn);
        }
    }

    public boolean removeFunctionByName(@NotNull String name) {
        for (int i = 0; i < groupFunctions.size(); i++) {
            if (groupFunctions.get(i).getUniqueId().equals(name)) {
                return groupFunctions.remove(i).afterDeleteAction();
            }
        }
        return false;
    }

    public void bindTransformers() {
        dataContainer.clearTransformers();
        for (int i = 0; i < groupFunctions.size(); i++) {
            if (groupFunctions.get(i) instanceof TransformerGroupingFunctionColumn transformerGroupingFunctionColumn) {
                dataContainer.setAttributeTransformer(functionIndexToFullIndex(i), transformerGroupingFunctionColumn.getTransformer());
            }
        }
    }

    public List<GroupingFunctionColumn> getFunctionColumns() {
        return groupFunctions;
    }

    public boolean addAttribute(@NotNull SQLGroupingAttributeGroupingColumn sqlGroupingAttributeColumn) {
        if (!containAttribute(sqlGroupingAttributeColumn.getSqlGroupingAttribute())) {
            return attributes.add(sqlGroupingAttributeColumn);
        } else {
            return false;
        }
    }

    public boolean containAttribute(@NotNull SQLGroupingAttribute attribute) {
        return attributes
            .stream()
            .map(SQLGroupingAttributeGroupingColumn::getSqlGroupingAttribute)
            .anyMatch(attribute::equals);
    }

    public boolean canColumnBeRemoved(int index) {
        return isValidIndex(index)
            && (isAttributeIndex(index) ? attributes.size() > 1 : groupFunctions.size() > 1);
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

    public List<SQLGroupingAttribute> getSqlAttributes() {
        return attributes
            .stream()
            .map(SQLGroupingAttributeGroupingColumn::getSqlGroupingAttribute)
            .toList();
    }

    public void clear() {
        groupFunctions.clear();
        attributes.clear();
        dataContainer.clearTransformers();
    }

    public boolean isEmpty() {
        return groupFunctions.isEmpty() || attributes.isEmpty();
    }

    public void moveColumns(int overColumnIndex, @NotNull List<Integer> indexesToMove) {
        TreeSet<Integer> attributesToMove = new TreeSet<>(Comparator.reverseOrder());
        TreeSet<Integer> functionsToMove = new TreeSet<>(Comparator.reverseOrder());
        for (Integer index : indexesToMove) {
            if (!isValidIndex(index)) {
                throw new IndexOutOfBoundsException("Illegal index [%d]. Attributes size [%d], function size [%d]".formatted(
                    index,
                    attributes.size(),
                    groupFunctions.size()
                ));
            }
            if (isAttributeIndex(index)) {
                attributesToMove.add(index);
            } else {
                functionsToMove.add(fullIndexToFunctionIndex(index));
            }
        }
        // for now functions can only be placed after attributes
        if (!attributesToMove.isEmpty()) {
            int attrOverColumn = isAttributeIndex(overColumnIndex) ? overColumnIndex : groupFunctions.size() - 1;
            moveElements(attributes, attrOverColumn, attributesToMove);
        }
        if (!functionsToMove.isEmpty()) {
            int funcOverColumn = isAttributeIndex(overColumnIndex) ? 0 : fullIndexToFunctionIndex(overColumnIndex);
            moveElements(groupFunctions, funcOverColumn, functionsToMove);
        }
    }

    private static <T> void moveElements(
        @NotNull List<T> listToModify,
        int toIndex,
        @NotNull TreeSet<Integer> indexesToMoveSortedInDescOrder
    ) {
        List<T> removedElements = new ArrayList<>(indexesToMoveSortedInDescOrder.size());
        for (Integer index : indexesToMoveSortedInDescOrder) {
            removedElements.addFirst(listToModify.remove(index.intValue()));
        }
        if (toIndex >= listToModify.size()) {
            listToModify.addAll(removedElements);
        } else {
            listToModify.addAll(toIndex, removedElements);
        }
    }

    private boolean isUniqueFunctionByName(@NotNull GroupingFunctionColumn functionColumn) {
        return groupFunctions
            .stream()
            .noneMatch(functionColumn::matchById);
    }

    private boolean removeColumn(int index) {
        return canColumnBeRemoved(index)
            && removeColumnNoCheck(index).afterDeleteAction();
    }

    private int functionIndexToFullIndex(int index) {
        return attributes.size() + index;
    }

    private int fullIndexToFunctionIndex(int index) {
        return index - attributes.size();
    }

    @NotNull
    private GroupingColumn removeColumnNoCheck(int index) {
        if (isAttributeIndex(index)) {
            return attributes.remove(index);
        } else {
            return groupFunctions.remove(fullIndexToFunctionIndex(index));
        }
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < attributes.size() + groupFunctions.size();
    }

    private boolean isAttributeIndex(int index) {
        return index < attributes.size();
    }

}
