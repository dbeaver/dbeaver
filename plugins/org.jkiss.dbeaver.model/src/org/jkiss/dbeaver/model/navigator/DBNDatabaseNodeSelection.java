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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class DBNDatabaseNodeSelection {
    private final Set<DBNDatabaseNode> selectedNodes = new LinkedHashSet<>();
    private final Map<DBNNode, Integer> selectedDescendantCounts = new HashMap<>();
    private final Predicate<DBNDatabaseNode> filter;

    public DBNDatabaseNodeSelection(@NotNull Predicate<DBNDatabaseNode> filter) {
        this.filter = filter;
    }

    public void setChecked(@NotNull DBNDatabaseNode node, boolean checked) {
        if (!filter.test(node)) {
            return;
        }
        if (checked) {
            if (!isFullyChecked(node)) {
                removeSelectedDescendants(node);
                addSelectedNode(node);
            }
        } else {
            removeSelectedDescendants(node);
            removeSelectedNode(node);
            for (DBNNode parent = node.getParentNode(); parent instanceof DBNDatabaseNode ancestor; parent = parent.getParentNode()) {
                if (removeSelectedNode(ancestor)) {
                    // only ancestors of an already displayed node are split; their children are cached
                    DBNDatabaseNode child = node;
                    while (child != ancestor && child.getParentNode() instanceof DBNDatabaseNode container) {
                        DBNDatabaseNode[] siblings = container.getChildNodes();
                        if (siblings != null) {
                            for (DBNDatabaseNode sibling : siblings) {
                                if (sibling != child && filter.test(sibling)) {
                                    addSelectedNode(sibling);
                                }
                            }
                        }
                        child = container;
                    }
                    break;
                }
            }
        }
    }

    public boolean isChecked(@NotNull DBNDatabaseNode node) {
        return isFullyChecked(node) || selectedDescendantCounts.containsKey(node);
    }

    public boolean isGrayed(@NotNull DBNDatabaseNode node) {
        return selectedDescendantCounts.containsKey(node) && !isSubtreeFullyChecked(node);
    }

    @NotNull
    public List<DBNDatabaseNode> getSelectedNodes() {
        return List.copyOf(selectedNodes);
    }

    private void addSelectedNode(@NotNull DBNDatabaseNode node) {
        if (selectedNodes.add(node)) {
            for (DBNNode parent = node.getParentNode(); parent != null; parent = parent.getParentNode()) {
                selectedDescendantCounts.merge(parent, 1, Integer::sum);
            }
        }
    }

    private boolean removeSelectedNode(@NotNull DBNDatabaseNode node) {
        if (!selectedNodes.remove(node)) {
            return false;
        }
        for (DBNNode parent = node.getParentNode(); parent != null; parent = parent.getParentNode()) {
            selectedDescendantCounts.computeIfPresent(parent, (ancestor, count) -> count == 1 ? null : count - 1);
        }
        return true;
    }

    private void removeSelectedDescendants(@NotNull DBNDatabaseNode node) {
        if (selectedDescendantCounts.containsKey(node)) {
            selectedNodes.stream().filter(selected -> isDescendant(selected, node)).toList().forEach(this::removeSelectedNode);
        }
    }

    private boolean isSubtreeFullyChecked(@NotNull DBNDatabaseNode node) {
        if (isFullyChecked(node)) {
            return true;
        }
        if (!selectedDescendantCounts.containsKey(node)) {
            return false;
        }
        DBNDatabaseNode[] children = node.getChildNodes();
        if (children == null) {
            return false;
        }
        boolean hasAcceptedChild = false;
        for (DBNDatabaseNode child : children) {
            if (filter.test(child)) {
                hasAcceptedChild = true;
                if (!isSubtreeFullyChecked(child)) {
                    return false;
                }
            }
        }
        return hasAcceptedChild;
    }

    private boolean isFullyChecked(@NotNull DBNDatabaseNode node) {
        for (DBNNode current = node; current != null; current = current.getParentNode()) {
            if (selectedNodes.contains(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDescendant(@NotNull DBNNode node, @NotNull DBNNode ancestor) {
        for (DBNNode parent = node.getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent == ancestor) {
                return true;
            }
        }
        return false;
    }
}
