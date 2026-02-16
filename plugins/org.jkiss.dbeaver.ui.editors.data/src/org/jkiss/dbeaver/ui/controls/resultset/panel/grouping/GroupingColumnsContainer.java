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

import java.util.*;
import java.util.function.Supplier;

public class GroupingColumnsContainer {

    @NotNull
    private final List<SQLGroupingAttribute> groupAttributes = new ArrayList<>();

    @NotNull
    private final List<String> groupingFunctions = new ArrayList<>();

    //-1 if percent function is not present
    private int percentFunctionInGroupingFunctionsIndex = -1;

    public void addGroupingFunction(@NotNull String function) {
        if (isPercentFunctionPresent()
            && percentFunctionInGroupingFunctionsIndex == groupingFunctionsSize() - 1) {
            groupingFunctions.add(percentFunctionInGroupingFunctionsIndex, function);
            percentFunctionInGroupingFunctionsIndex++;
        } else {
            groupingFunctions.add(function);
        }
    }

    public void addPercentFunction(@NotNull String percentFunction) {
        addGroupingFunction(percentFunction);
        percentFunctionInGroupingFunctionsIndex = groupingFunctions.size() - 1;
    }

    public void addAttribute(@NotNull SQLGroupingAttribute attribute) {
        if (!groupAttributes.contains(attribute)) {
            groupAttributes.add(attribute);
        }
    }


    public void clear() {
        groupAttributes.clear();
        removePercentFunction();
        groupingFunctions.clear();
    }

    public boolean removePercentFunction() {
        if (percentFunctionInGroupingFunctionsIndex >= 0) {
            groupingFunctions.remove(percentFunctionInGroupingFunctionsIndex);
            percentFunctionInGroupingFunctionsIndex = -1;
            return true;
        }
        return false;
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
    public List<String> getGroupFunctions() {
        return new ArrayList<>(groupingFunctions);
    }

    @NotNull
    public List<String> getUserDefinedFunctions() {
        List<String> userFunctions = new ArrayList<>(groupingFunctions);
        if (isPercentFunctionPresent()) {
            userFunctions.remove(percentFunctionInGroupingFunctionsIndex);
        }
        return userFunctions;
    }

    @NotNull
    public List<SQLGroupingAttribute> getGroupAttributes() {
        return new ArrayList<>(groupAttributes);
    }

    public int getPercentFunctionIndex() {
        return percentFunctionInGroupingFunctionsIndex < 0
            ? percentFunctionInGroupingFunctionsIndex
            : groupAttributes.size() + percentFunctionInGroupingFunctionsIndex;
    }

    public boolean isPercentFunctionPresent() {
        return percentFunctionInGroupingFunctionsIndex >= 0;
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
            int percentFunctionIndexInMoved = findPercentFunctionIndex(functionsToMove);
            if (percentFunctionIndexInMoved >= 0) {
                percentFunctionInGroupingFunctionsIndex = funcOverColumn + percentFunctionIndexInMoved;
            } else if (funcOverColumn <= percentFunctionInGroupingFunctionsIndex) {
                percentFunctionInGroupingFunctionsIndex += countPercentFunctionIndexDelta(functionsToMove);
            }
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

    private int countPercentFunctionIndexDelta(@NotNull Collection<Integer> functionIndexesToMove) {
        int countLaterIndexes = 0;
        int countEarlierIndexes = 0;
        for (Integer index : functionIndexesToMove) {
            if (index > percentFunctionInGroupingFunctionsIndex) {
                countLaterIndexes++;
            } else if (index < percentFunctionInGroupingFunctionsIndex) {
                countEarlierIndexes++;
            }
        }
        return countLaterIndexes - countEarlierIndexes;
    }

    private InstanceType instanceTypeByIndex(int index) {
        return index < attributesSize()
            ? InstanceType.ATTRIBUTE
            : index == getPercentFunctionIndex()
                ? InstanceType.PERCENT_GROUPING_FUNCTION
                : InstanceType.GROUPING_FUNCTION;

    }

    private boolean removeFunction(int functionIndex) {
        if (percentFunctionInGroupingFunctionsIndex == functionIndex) {
            return removePercentFunction();
        } else {
            boolean wasRemoved = groupingFunctions.remove(functionIndex) != null;
            if (percentFunctionInGroupingFunctionsIndex >= functionIndex && wasRemoved) {
                percentFunctionInGroupingFunctionsIndex--;
            }
            return wasRemoved;
        }
    }

    private int fullIndexToFunctionIndex(int index) {
        int groupingFunctionIndex = index - attributesSize();
        if (groupingFunctionIndex < 0 || groupingFunctionIndex >= groupingFunctions.size()) {
            throw new IndexOutOfBoundsException(
                "Not found index in grouping func. Attributes size [%d] Grouping function size [%d] index to convert [%d]"
                    .formatted(attributesSize(), groupingFunctionsSize(), index));
        }
        return groupingFunctionIndex;
    }

    private int findPercentFunctionIndex(@NotNull Collection<Integer> indexes) {
        int foundIndex = 0;
        for (Integer index : indexes) {
            if (index.equals(percentFunctionInGroupingFunctionsIndex)) {
                return foundIndex;
            }
            foundIndex++;
        }
        return -1;
    }

    @NotNull
    public RemoveColumnStrategy createRemoveStrategy(int index) {
        return new RemoveColumnStrategy(index);
    }

    public class RemoveColumnStrategy {

        private final Supplier<Boolean> removeFunction;

        private final InstanceType type;

        public RemoveColumnStrategy(int index) {
            this.type = instanceTypeByIndex(index);
            this.removeFunction = defineRemoveStrategy(index);
        }

        public boolean removeColumn() {
            return canBeRemoved() && removeFunction.get();
        }

        @NotNull
        public InstanceType getType() {
            return type;
        }

        public boolean canBeRemoved() {
            return switch (type) {
                case ATTRIBUTE -> groupAttributes.size() > 1;
                case GROUPING_FUNCTION -> groupingFunctions.size() > 1;
                case PERCENT_GROUPING_FUNCTION -> isPercentFunctionPresent();
            };
        }

        private Supplier<Boolean> defineRemoveStrategy(int index) {
            return switch (type) {
                case ATTRIBUTE -> () -> groupAttributes.remove(index) != null;
                case GROUPING_FUNCTION, PERCENT_GROUPING_FUNCTION -> () -> removeFunction(fullIndexToFunctionIndex(index));
            };
        }

    }

    public enum InstanceType {
        ATTRIBUTE,
        GROUPING_FUNCTION,
        PERCENT_GROUPING_FUNCTION;
    }
}
