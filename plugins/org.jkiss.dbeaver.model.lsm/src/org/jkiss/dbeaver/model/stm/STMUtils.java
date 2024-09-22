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
package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.function.Function;

public class STMUtils {

    @NotNull
    public static List<STMTreeNode> expandSubtree(
        @NotNull STMTreeNode root,
        @Nullable Set<String> toExpand,
        @NotNull Set<String> toCollect
    ) {
        List<STMTreeNode> result = new ArrayList<>();
        Stack<STMTreeNode> stack = new Stack<>();
        stack.add(root);

        while (!stack.isEmpty()) {
            STMTreeNode node = stack.pop();
            String nodeName = node.getNodeName();
            
            if (toCollect.contains(nodeName)) {
                result.add(node);
            } else if (toExpand == null || toExpand.contains(nodeName)) {
                for (int i = node.getChildCount() - 1; i >= 0; i--) {
                    stack.push((STMTreeNode) node.getChild(i));
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<STMTreeTermNode> expandTerms(@NotNull STMTreeNode root) {
        List<STMTreeTermNode> result = new ArrayList<>();
        Stack<STMTreeNode> stack = new Stack<>();
        stack.add(root);

        while (stack.size() > 0) {
            STMTreeNode node = stack.pop();

            if (node instanceof STMTreeTermNode term) {
                result.add(term);
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    stack.push((STMTreeNode) node.getChild(i));
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<String> expandTermStrings(@NotNull STMTreeNode root) {
        List<String> result = new ArrayList<>();
        Stack<STMTreeNode> stack = new Stack<>();
        stack.add(root);

        while (stack.size() > 0) {
            STMTreeNode node = stack.pop();

            if (node instanceof STMTreeTermNode term) {
                result.add(term.getText());
            } else {
                for (int i = 0; i < node.getChildCount(); i++) {
                    stack.push((STMTreeNode) node.getChild(i));
                }
            }
        }
        return result;
    }


    /**
     * Perform binary search by the key in the sorted collection
     *
     * @param list - list to search in
     * @param keyGetter - Function to apply to the element of the collection before comparison
     * @param key - key to search for
     * @param comparator - elements comparator
     * @param <T> - type of the element
     * @param <K> - type of the return value of keyGetter
     * @return index of the element, if it is found or (-(insertion point) - 1).
     *     The insertion point is defined as the point at which the key would be inserted into the list:
     *         the index of the first element greater than the key,
     *         or list.size() if all elements in the list are less than the specified key.
     *     Note that this guarantees that the return value will be >= 0 if and only if the key is found.
     */
    public static <T, K> int binarySearchByKey(
        @NotNull List<T> list,
        @NotNull Function<T, K> keyGetter,
        @NotNull K key,
        @NotNull Comparator<K> comparator
    ) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            K midVal = keyGetter.apply(list.get(mid));
            int cmp = comparator.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }

    /**
     * Insert element to the sorted collection
     *
     * @param list - collection to insert to
     * @param keyGetter - Function to apply to the element of the collection before comparison
     * @param value - value to insert
     * @param comparator - elements comparator
     * @param <T> - type of the element
     * @param <K> - type of the return value of keyGetter
     *
     * @return a list with inserted value
     */
    public static <T, K> List<T> orderedInsert(
        @Nullable List<T> list,
        @NotNull Function<T, K> keyGetter,
        @NotNull T value,
        @NotNull Comparator<K> comparator
    ) {
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.isEmpty()) {
            list.add(value);
        } else {
            K key = keyGetter.apply(value);
            K lastKey = keyGetter.apply(list.get(list.size() - 1));
            if (comparator.compare(key, lastKey) > 0) {
                list.add(value);
            } else {
                int index = binarySearchByKey(list, keyGetter, key, comparator);
                if (index < 0) {
                    index = ~index;
                }
                list.add(index, value);
            }
        }
        return list;
    }

    /**
     * Provides a new list containing leftColumns list elements and then rightColumns list elements
     */
    @NotNull
    public static <T> List<T> combineLists(
        @NotNull List<T> leftColumns,
        @NotNull List<T> rightColumns
    ) {
        List<T> symbols = new ArrayList<>(leftColumns.size() + rightColumns.size());
        symbols.addAll(leftColumns);
        symbols.addAll(rightColumns);
        return symbols;
    }

    /**
     * Returns text covered by the provided node
     */
    @NotNull
    public static String getTextContent(Tree node) {
        String result = null;
        if (node instanceof STMTreeNode stmNode) {
            result = stmNode.getTextContent();
        } else {
            Tree first = node;
            Tree last = node;

            while (!(first instanceof TerminalNode) && first.getChildCount() > 0) {
                first = first.getChild(0);
            }
            while (!(last instanceof TerminalNode) && last.getChildCount() > 0) {
                last = last.getChild(last.getChildCount() - 1);
            }
            if (first instanceof TerminalNode a && last instanceof TerminalNode b) {
                Interval textRange = Interval.of(a.getSymbol().getStartIndex(), b.getSymbol().getStopIndex());
                result = b.getSymbol().getTokenSource().getInputStream().getText(textRange);
            }
        }
        return CommonUtils.notEmpty(result);
    }
}
