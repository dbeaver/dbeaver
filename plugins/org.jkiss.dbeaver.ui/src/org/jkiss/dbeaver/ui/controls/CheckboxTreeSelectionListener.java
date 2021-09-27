/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.NotNull;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public class CheckboxTreeSelectionListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
        if ((e.detail & SWT.CHECK) != SWT.CHECK || !(e.item instanceof TreeItem)) {
            return;
        }
        TreeItem target = (TreeItem) e.item;

        if (!target.getChecked() && target.getGrayed()) {
            // It's a hack. When we click on a HALFWAY_CHECKED TreeItem,
            // the checked property sets to false while the grayed property is still true.
            // If we click on the same item again, the TreeItem will be HALFWAY_CHECKED again (as well as all its subtree),
            // and we fall into an infinite loop: we can only cycle between HALFWAY_CHECKED and UNCHECKED states.
            // The good news is the solution is easy and simple.
            target.setGrayed(false);
        }
        CheckboxState targetState = getState(target);

        // All items of the subtree with target as its root should have the same state. Let's do it.
        // Keep in mind that target is never HALFWAY_CHECKED, that's why we don't worry about it.
        Queue<TreeItem> queue = new ArrayDeque<>(getChildren(target));
        while (!queue.isEmpty()) {
            TreeItem item = queue.remove();
            setState(item, targetState);
            queue.addAll(getChildren(item));
        }

        // Now we need to fix target's parents.
        for (TreeItem item = target; item.getParentItem() != null; item = item.getParentItem()) {
            CheckboxState itemState = getState(item);
            TreeItem parent = item.getParentItem();
            if (itemState == getState(parent)) {
                break;
            }
            if (itemState == CheckboxState.HALFWAY_CHECKED || getChildren(parent).stream().anyMatch(i -> getState(i) != itemState)) {
                setState(parent, CheckboxState.HALFWAY_CHECKED);
            } else {
                setState(parent, itemState);
            }
        }
    }

    @NotNull
    private static List<TreeItem> getChildren(@NotNull TreeItem treeItem) {
        return ArrayUtils.safeArray(treeItem.getItems());
    }

    @NotNull
    private static CheckboxState getState(@NotNull TreeItem treeItem) {
        if (!treeItem.getChecked()) {
            return CheckboxState.UNCHECKED;
        }
        if (treeItem.getGrayed()) {
            return CheckboxState.HALFWAY_CHECKED;
        }
        return CheckboxState.FULLY_CHECKED;
    }

    private static void setState(@NotNull TreeItem item, @NotNull CheckboxState state) {
        item.setChecked(state == CheckboxState.FULLY_CHECKED || state == CheckboxState.HALFWAY_CHECKED);
        item.setGrayed(state == CheckboxState.HALFWAY_CHECKED);
    }

    public static void fixTree(@NotNull Tree tree) {
        for (TreeItem root: tree.getItems()) {
            fixSubtree(root);
        }
    }

    private static void fixSubtree(@NotNull TreeItem treeItem) {
        Collection<TreeItem> children = getChildren(treeItem);
        if (children.isEmpty()) {
            return;
        }

        int numOfFullyChecked = 0;
        boolean hasHalfwayCheckedChild = false;
        for (TreeItem child: children) {
            fixSubtree(child);
            CheckboxState childState = getState(child);
            if (childState == CheckboxState.FULLY_CHECKED) {
                numOfFullyChecked++;
            }
            if (childState == CheckboxState.HALFWAY_CHECKED) {
                hasHalfwayCheckedChild = true;
            }
        }

        CheckboxState treeItemState;
        if (numOfFullyChecked == children.size()) {
            treeItemState = CheckboxState.FULLY_CHECKED;
        } else if (hasHalfwayCheckedChild || numOfFullyChecked > 0) {
            treeItemState = CheckboxState.HALFWAY_CHECKED;
        } else {
            treeItemState = CheckboxState.UNCHECKED;
        }
        setState(treeItem, treeItemState);
    }

    private enum CheckboxState {
        FULLY_CHECKED,
        HALFWAY_CHECKED, // The state when some children are FULLY_CHECKED while some aren't
        UNCHECKED,
    }
}
