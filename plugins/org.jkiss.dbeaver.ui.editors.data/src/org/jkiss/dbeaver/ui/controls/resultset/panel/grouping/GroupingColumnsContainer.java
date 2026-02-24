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
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column.GroupingFunctionDescriptor;

import java.util.*;
import java.util.function.Supplier;

public class GroupingColumnsContainer {

    @NotNull
    private final List<SQLGroupingAttribute> groupAttributes = new ArrayList<>();

    @NotNull
    private final List<GroupingFunctionDescriptor> groupingFunctions = new ArrayList<>();

    public void addGroupingFunction(@NotNull String function) {
        addSpecialFunction(new SimpleSQLFunctionDescriptor(function));
    }

    public void addSpecialFunction(@NotNull GroupingFunctionDescriptor specialFunction) {
        if (specialFunction.canBeAdded()) {
            groupingFunctions.add(specialFunction);
        }
    }

    public boolean removeFunctionByType(Class<? extends GroupingFunctionDescriptor> functionType){
        List<Integer> functionsToRemove =
    }



    private boolean removeFunction(int functionIndex) {
        return groupingFunctions
            .remove(functionIndex)
            .afterDeleteAction(groupFunctionIndexToFullIndex(functionIndex));
    }

    public void addAttribute(@NotNull SQLGroupingAttribute attribute) {
        if (!groupAttributes.contains(attribute)) {
            groupAttributes.add(attribute);
        }
    }

    public void clear() {
        groupAttributes.clear();
        groupingFunctions.clear();
    }

    public boolean isEmpty() {
        return groupAttributes.isEmpty() || isFunctionsEmpty();
    }

    public boolean isFunctionsEmpty() {
        return groupingFunctions.isEmpty();
    }

    public int groupingFunctionsSize() {
        return groupingFunctions.size();
    }

    public int attributesSize() {
        return groupAttributes.size();
    }

    @NotNull
    public List<GroupingFunctionDescriptor> getGroupingFunctions() {
        return groupingFunctions;
    }

    @NotNull
    public List<String> getGroupFunctionsSql() {
        List<String> groupingSqlFunctionsSql = new ArrayList<>();
        for (int i = 0; i < groupingFunctionsSize(); i++) {
            GroupingFunctionDescriptor desc = groupingFunctions.get(i);
            groupingSqlFunctionsSql.add(desc.provideSqlFunction(groupFunctionIndexToFullIndex(i)));
        }
        return groupingSqlFunctionsSql;
    }

    @NotNull
    public List<String> getUserDefinedFunctions() {
        List<String> groupingSqlFunctionsSql = new ArrayList<>();
        for (int i = 0; i < groupingFunctionsSize(); i++) {
            GroupingFunctionDescriptor desc = groupingFunctions.get(i);
            if (desc instanceof SimpleSQLFunctionDescriptor) {
                groupingSqlFunctionsSql.add(desc.provideSqlFunction(groupFunctionIndexToFullIndex(i)));
            }
        }
        return groupingSqlFunctionsSql;
    }

    @NotNull
    public List<SQLGroupingAttribute> getGroupAttributes() {
        return new ArrayList<>(groupAttributes);
    }

    public void moveColumns(int overColumnIndex, @NotNull List<Integer> indexesToMove) {
        SortedSet<Integer> attributesToMove = new TreeSet<>(Comparator.reverseOrder());
        SortedSet<Integer> functionsToMove = new TreeSet<>(Comparator.reverseOrder());
        for (Integer index : indexesToMove) {
            if (instanceTypeByIndex(index) == InstanceType.ATTRIBUTE) {
                attributesToMove.add(index);
            } else {
                functionsToMove.add(fullIndexToFunctionIndex(index));
            }
        }
        // for now functions can only be placed after attributes
        if (!attributesToMove.isEmpty()) {
            int attrOverColumn = overColumnIndex >= attributesSize() ? attributesSize() - 1 : overColumnIndex;
            moveElements(groupAttributes, attrOverColumn, attributesToMove);
        }
        if (!functionsToMove.isEmpty()) {
            int funcOverColumn = overColumnIndex >= attributesSize() ? fullIndexToFunctionIndex(overColumnIndex) : 0;
            moveElements(groupingFunctions, funcOverColumn, functionsToMove);
        }
    }

    private static <T> void moveElements(
        @NotNull List<T> listToModify,
        int toIndex,
        @NotNull SortedSet<Integer> indexesToMoveSortedInDescOrder
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

    private InstanceType instanceTypeByIndex(int index) {
        return index < attributesSize()
            ? InstanceType.ATTRIBUTE
            : InstanceType.GROUPING_FUNCTION;
    }

    private int fullIndexToFunctionIndex(int index) {
        int groupingFunctionIndex = index - attributesSize();
        if (groupingFunctionIndex < 0 || groupingFunctionIndex >= groupingFunctions.size()) {
            return -1;
        }
        return groupingFunctionIndex;
    }

    private int groupFunctionIndexToFullIndex(int groupIndex) {
        return groupAttributes.size() + groupIndex;
    }

    @NotNull
    public RemoveColumnStrategy createRemoveStrategy(int index) {
        return new RemoveColumnStrategy(index);
    }

    public class RemoveColumnStrategy {

        private final Supplier<Boolean> removeColumnOperation;

        private final InstanceType type;

        private final int index;

        public RemoveColumnStrategy(int index) {
            this.index = index;
            this.type = instanceTypeByIndex(this.index);
            this.removeColumnOperation = defineRemoveStrategy();
        }

        public boolean removeColumn() {
            return canBeRemoved() && removeColumnOperation.get();
        }

        public boolean canBeRemoved() {
            return switch (type) {
                case ATTRIBUTE -> groupAttributes.size() > 1;
                case GROUPING_FUNCTION -> canFunctionBeRemoved();
            };
        }

        private boolean canFunctionBeRemoved() {
            int functionIndex = fullIndexToFunctionIndex(index);
            return functionIndex >= 0
                && groupingFunctions.get(functionIndex).canBeRemoved();
        }

        private Supplier<Boolean> defineRemoveStrategy() {
            return switch (type) {
                case ATTRIBUTE -> () -> groupAttributes.remove(index) != null;
                case GROUPING_FUNCTION -> () -> removeFunction(fullIndexToFunctionIndex(index));
            };
        }

    }

    private enum InstanceType {
        ATTRIBUTE,
        GROUPING_FUNCTION;
    }

    private class SimpleSQLFunctionDescriptor implements GroupingFunctionDescriptor {

        private final String sql;

        public SimpleSQLFunctionDescriptor(@NotNull String sql) {
            this.sql = sql;
        }

        @Override
        public String provideSqlFunction(int indexInDataContainer) {
            return sql;
        }

        @Override
        public boolean canBeAdded() {
            return true;
        }

        @Override
        public boolean canBeRemoved() {
            return groupingFunctions.size() > 1;
        }
    }
}
