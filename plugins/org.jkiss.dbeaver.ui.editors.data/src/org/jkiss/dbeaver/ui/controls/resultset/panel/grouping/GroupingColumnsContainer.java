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
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.function.Supplier;

public class GroupingColumnsContainer {

    @NotNull
    private final List<SQLGroupingAttribute> groupAttributes = new ArrayList<>();

    @NotNull
    private final List<Pair<String, Boolean>> groupingFunctions = new ArrayList<>();


    public void addGroupingFunction(@NotNull String function) {
        groupingFunctions.add(Pair.of(function, false));
    }

    public void addPercentFunction(@NotNull String percentFunction) {
        groupingFunctions.add(Pair.of(percentFunction, true));
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

    public boolean removePercentFunction() {
        return groupingFunctions.removeIf(this::isPercentFunction);
    }

    private boolean isPercentFunction(@NotNull Pair<String, Boolean> function) {
        return function.getSecond() != null
            && function.getSecond();
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
        return groupingFunctions
            .stream()
            .map(Pair::getFirst)
            .toList();
    }

    @NotNull
    public List<String> getUserDefinedFunctions() {
        return groupingFunctions
            .stream()
            .filter(p -> !isPercentFunction(p))
            .map(Pair::getFirst)
            .toList();
    }

    @NotNull
    public List<SQLGroupingAttribute> getGroupAttributes() {
        return new ArrayList<>(groupAttributes);
    }

    public int getPercentFunctionIndex() {
        int foundIndex = findPercentFunctionGroupingIndex();
        return foundIndex >= 0
            ? groupAttributes.size() + foundIndex
            : -1;
    }

    public boolean isPercentFunctionPresent() {
        return getPercentFunctionIndex() >= 0;
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
            : isPercentFunction(groupingFunctions.get(fullIndexToFunctionIndex(index)))
                ? InstanceType.PERCENT_GROUPING_FUNCTION
                : InstanceType.GROUPING_FUNCTION;

    }

    private boolean removeFunction(int functionIndex) {
        return groupingFunctions.remove(functionIndex) != null;
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

    private int findPercentFunctionGroupingIndex() {
        int foundIndex = 0;
        for (Pair<String, Boolean> function : groupingFunctions) {
            if (isPercentFunction(function)) {
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
