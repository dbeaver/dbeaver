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
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.findandreplace.FindReplaceMessages;
import org.eclipse.ui.internal.findandreplace.HistoryStore;
import org.jkiss.code.NotNull;

import java.util.function.Consumer;

/**
 * Menu dropdown for the search history in the find/replace overlay
 * <p>
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/SearchHistoryMenu.java">eclipse.platform.ui</a>
 */
class SearchHistoryMenu extends Dialog {
    @NotNull
    private final Consumer<String> historyEntrySelectedCallback;
    @NotNull
    private final HistoryStore history;
    @NotNull
    private final ShellListener shellFocusListener = new ShellAdapter() {
        @Override
        public void shellDeactivated(ShellEvent e) {
            if (!getShell().isDisposed()) {
                getShell().getDisplay().asyncExec(SearchHistoryMenu.this::close);
            }
        }
    };
    private Point location;
    private int width;
    private Table table;
    private TableColumn column;
    private int selectedIndexInTable = -1;

    public SearchHistoryMenu(@NotNull Shell parent, @NotNull HistoryStore history, @NotNull Consumer<String> historyEntrySelectedCallback) {
        super(parent);
        this.setShellStyle(SWT.NONE);
        this.setBlockOnOpen(false);

        this.historyEntrySelectedCallback = historyEntrySelectedCallback;
        this.history = history;
    }

    public void setPosition(int x, int y, int width) {
        this.location = new Point(x, y);
        this.width = width;
    }

    @NotNull
    @Override
    public Control createContents(@NotNull Composite parent) {
        this.table = new Table(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(this.table);
        this.column = new TableColumn(this.table, SWT.NONE);

        if (history.isEmpty()) {
            TableItem item = new TableItem(this.table, SWT.NONE);
            item.setText(FindReplaceMessages.SearchHistoryMenu_SEARCH_HISTORY_EMPTY_STRING);
            this. table.setEnabled(false);
        } else {
            for (String entry : this.history.get()) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(entry);
            }
        }

        this.attachTableListeners();
        this.getShell().pack();
        this.getShell().layout();
        return table;
    }

    @Override
    public int open() {
        int code = super.open();

        this.getShell().addShellListener(this.shellFocusListener);
        this.positionShell();

        return code;
    }

    private void moveSelectionInTable(int indexShift) {
        this.selectedIndexInTable += indexShift;
        if (this.selectedIndexInTable < 0) {
            this.selectedIndexInTable = this.table.getItemCount() - 1;
        } else if (this.selectedIndexInTable > this.table.getItemCount() - 1) {
            this.selectedIndexInTable = 0;
        }
        this.table.setSelection(this.selectedIndexInTable);
        this.historyEntrySelectedCallback.accept(this.table.getSelection()[0].getText());
    }

    private void attachTableListeners() {
        this.table.addListener(SWT.MouseMove, event -> {
            Point point = new Point(event.x, event.y);
            TableItem item = this.table.getItem(point);
            if (item != null) {
                this.table.setSelection(item);
                this.selectedIndexInTable = this.table.getSelectionIndex();
            }
        });
        this.table.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.keyCode == SWT.ARROW_DOWN) {
                this.moveSelectionInTable(1);
                e.doit = false;
            } else if (e.keyCode == SWT.ARROW_UP) {
                this.moveSelectionInTable(-1);
                e.doit = false;
            } else if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                this.notifyParentOfSelectionInput();
                this.close();
            }
        }));
        table.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            this.notifyParentOfSelectionInput();
        }));
        this.table.addMouseListener(MouseListener.mouseDownAdapter(e -> {
            this.table.notifyListeners(SWT.Selection, null);
            this.close();
        }));
    }

    private void notifyParentOfSelectionInput() {
        TableItem[] selection = this.table.getSelection();
        if (selection.length == 0) {
            this.historyEntrySelectedCallback.accept(null);
            return;
        }
        String text = selection[0].getText();
        if (text != null) {
            this.historyEntrySelectedCallback.accept(text);
        }
        this.historyEntrySelectedCallback.accept(null);
    }

    private void positionShell() {
        if (this.location != null && table.getItemCount() != 0) {
            this.getShell().setBounds(
                this.location.x, this.location.y, this.width,
                Math.min(this.table.getItemHeight() * 7, this.table.getItemHeight() * this.table.getItemCount() + 2)
            );
        }
        int columnWidth = this.table.getSize().x;
        if (this.table.getVerticalBar() != null && this.table.getVerticalBar().isVisible()) {
            columnWidth = this.table.getSize().x - this.table.getVerticalBar().getSize().x;
        }
        this.column.setWidth(columnWidth);
    }
}
