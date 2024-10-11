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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRCreator;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * UI Utils
 */
public class UIWidgets {
    private static final Log log = Log.getLog(UIWidgets.class);

    public static Combo createDelimiterCombo(Composite group, String label, String[] options, String defDelimiter, boolean multiDelims) {
        UIUtils.createControlLabel(group, label);
        Combo combo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String option : options) {
            combo.add(CommonUtils.escapeDisplayString(option));
        }
        if (!multiDelims) {
            if (!ArrayUtils.contains(options, defDelimiter)) {
                combo.add(CommonUtils.escapeDisplayString(defDelimiter));
            }
            String[] items = combo.getItems();
            for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                String delim = CommonUtils.unescapeDisplayString(items[i]);
                if (delim.equals(defDelimiter)) {
                    combo.select(i);
                    break;
                }
            }
        } else {
            combo.setText(CommonUtils.escapeDisplayString(defDelimiter));
        }
        return combo;
    }

    public static void fillDefaultTableContextMenu(IContributionManager menu, final Table table) {
        if (table.getColumnCount() > 1) {
            menu.add(new Action(NLS.bind(UIMessages.utils_actions_copy_label, table.getColumn(0).getText())) {
                @Override
                public void run() {
                    StringBuilder text = new StringBuilder();
                    for (TableItem item : table.getSelection()) {
                        if (!text.isEmpty()) text.append("\n");
                        text.append(item.getText(0));
                    }
                    if (text.isEmpty()) {
                        return;
                    }
                    UIUtils.setClipboardContents(table.getDisplay(), TextTransfer.getInstance(), text.toString());
                }
            });
        }
        menu.add(new Action(UIMessages.utils_actions_copy_all_label) {
            @Override
            public void run() {
                StringBuilder text = new StringBuilder();
                int columnCount = table.getColumnCount();
                for (TableItem item : table.getSelection()) {
                    if (!text.isEmpty()) text.append("\n");
                    if (columnCount > 0) {
                        for (int i = 0; i < columnCount; i++) {
                            if (i > 0) text.append("\t");
                            text.append(item.getText(i));
                        }
                    } else {
                        text.append(item.getText());
                    }
                }
                if (text.isEmpty()) {
                    return;
                }
                UIUtils.setClipboardContents(table.getDisplay(), TextTransfer.getInstance(), text.toString());
            }
        });
    }

    public static void fillDefaultTreeContextMenu(IContributionManager menu, final Tree tree) {
        if (tree.getColumnCount() > 1) {
            menu.add(new Action("Copy " + tree.getColumn(0).getText()) {
                @Override
                public void run() {
                    StringBuilder text = new StringBuilder();
                    for (TreeItem item : tree.getSelection()) {
                        if (!text.isEmpty()) text.append("\n");
                        text.append(item.getText(0));
                    }
                    if (text.isEmpty()) {
                        return;
                    }
                    UIUtils.setClipboardContents(tree.getDisplay(), TextTransfer.getInstance(), text.toString());
                }
            });
        }
        menu.add(new Action(UIMessages.utils_actions_copy_all_label) {
            @Override
            public void run() {
                StringBuilder text = new StringBuilder();
                int columnCount = tree.getColumnCount();
                for (TreeItem item : tree.getSelection()) {
                    if (!text.isEmpty()) text.append("\n");
                    if (columnCount > 0) {
                        for (int i = 0; i < columnCount; i++) {
                            if (i > 0) text.append("\t");
                            text.append(item.getText(i));
                        }
                    } else {
                        text.append(item.getText());
                    }
                }
                if (text.isEmpty()) {
                    return;
                }
                UIUtils.setClipboardContents(tree.getDisplay(), TextTransfer.getInstance(), text.toString());
            }
        });
        //menu.add(ActionFactory.SELECT_ALL.create(UIUtils.getActiveWorkbenchWindow()));
    }

    public static void createTableContextMenu(@NotNull final Table table, @Nullable DBRCreator<Boolean, IContributionManager> menuCreator) {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            if (menuCreator != null) {
                if (!menuCreator.createObject(menuMgr)) {
                    return;
                }
            }
            fillDefaultTableContextMenu(manager, table);
        });
        menuMgr.setRemoveAllWhenShown(true);
        table.setMenu(menuMgr.createContextMenu(table));
        table.addDisposeListener(e -> menuMgr.dispose());
    }

    public static void setControlContextMenu(Control control, IMenuListener menuListener) {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(menuListener);
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menuMgr.createContextMenu(control));
        control.addDisposeListener(e -> menuMgr.dispose());
    }
}
