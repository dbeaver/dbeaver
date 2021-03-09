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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.utils.RuntimeUtils;

//See #9872. Seems to be a bug in Eclipse, the other gtk app we tried works as expected.
public class LinuxKeyboardArrowsListener implements KeyListener {
    private final Tree tree;

    @Nullable
    private TreeItem item;

    private boolean wasExpanded;

    private LinuxKeyboardArrowsListener(Tree tree) {
        this.tree = tree;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.keyCode != SWT.ARROW_LEFT) {
            return;
        }
        TreeItem[] items = tree.getSelection();
        if (items.length != 1) {
            return;
        }
        item = items[0];
        wasExpanded = item.getExpanded();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.keyCode != SWT.ARROW_LEFT || wasExpanded || item == null) {
            return;
        }
        TreeItem parentItem = item.getParentItem();
        if (parentItem == null) {
            return;
        }
        tree.setSelection(parentItem);
        tree.showSelection();
        item = null;
    }

    public static void installOn(Tree tree) {
        if (RuntimeUtils.isLinux()) {
            tree.addKeyListener(new LinuxKeyboardArrowsListener(tree));
        }
    }
}
