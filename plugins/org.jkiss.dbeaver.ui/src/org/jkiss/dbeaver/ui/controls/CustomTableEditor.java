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

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Table editor
 */
public abstract class CustomTableEditor implements MouseListener, TraverseListener {

    private final Table table;
    private final TableEditor tableEditor;
    private ContentProposalAdapter proposalAdapter;
    private int columnIndex;
    protected int firstTraverseIndex = -1, lastTraverseIndex = -1;
    protected boolean editOnEnter = true;

    public CustomTableEditor(Table table) {
        this(table, null);
    }

    public CustomTableEditor(Table table, ContentProposalAdapter proposalAdapter) {
        this.table = table;
        this.proposalAdapter = proposalAdapter;

        tableEditor = new TableEditor(table);
        tableEditor.horizontalAlignment = SWT.CENTER;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;
        tableEditor.minimumWidth = 50;

        table.addMouseListener(this);
        table.addTraverseListener(this);
    }

    public void setProposalAdapter(ContentProposalAdapter proposalAdapter) {
        this.proposalAdapter = proposalAdapter;
    }

    @Override
    public void mouseDoubleClick(MouseEvent e) {

    }

    @Override
    public void mouseDown(MouseEvent e) {

    }

    @Override
    public void mouseUp(MouseEvent e) {
        if (e.button != 1) {
            // Only on left click
            return;
        }
        final TableItem item = table.getItem(new Point(e.x, e.y));
        if (item != null) {
            columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            UIUtils.asyncExec(() -> showEditor(item));
        }
    }

    public void showEditor(TableItem item, int index) {
        this.columnIndex = index;
        showEditor(item);
    }

    public void showEditor(TableItem item) {
        closeEditor();
        table.showItem(item);
        final Control editor = createEditor(table, columnIndex, item);
        if (editor == null) {
            return;
        }
        tableEditor.minimumHeight = editor.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        editor.setFocus();
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                onFocusLost(editor);
            }
        });
        UIUtils.installMacOSFocusLostSubstitution(editor, () -> onFocusLost(editor));
        editor.addTraverseListener(this);
        tableEditor.setEditor(editor, item, columnIndex);
    }

    private void onFocusLost(Control editor) {
        saveEditorValue(editor, columnIndex, tableEditor.getItem());
        if (!isProposalPopupActive()) {
            closeEditor();
        }
    }

    private boolean isProposalPopupActive() {
        return proposalAdapter != null && proposalAdapter.isProposalPopupOpen();
    }

    public void closeEditor() {
        Control editor = tableEditor.getEditor();
        if (editor != null) {
            editor.dispose();
        }
    }

    @Override
    public void keyTraversed(TraverseEvent e)
    {
        Control editor = tableEditor.getEditor();
        if (editor != null && editor.isDisposed()) {
            editor = null;
        }
        if (e.detail == SWT.TRAVERSE_RETURN) {
            if (editor != null) {
                saveEditorValue(editor, columnIndex, tableEditor.getItem());
                if (!isProposalPopupActive()) {
                    this.closeEditor();
                }
            } else if (editOnEnter) {
                TableItem[] selection = table.getSelection();
                if (selection != null && selection.length >= 1) {
                    showEditor(selection[0]);
                }
            }
            if (editOnEnter) {
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
            }
        } else if (e.detail == SWT.TRAVERSE_TAB_NEXT && editor != null) {
            TableItem item = tableEditor.getItem();
            if (item != null) {
                saveEditorValue(editor, columnIndex, item);
                if (!isProposalPopupActive()) {
                    this.closeEditor();
                }

                int lastColumn = lastTraverseIndex > 0 ? lastTraverseIndex : table.getColumnCount() - 1;
                if (columnIndex < lastColumn) {
                    columnIndex++;
                } else {
                    item = UIUtils.getNextTableItem(table, tableEditor.getItem());
                    if (item == null && table.getItemCount() > 0) {
                        item = table.getItem(0);
                    }
                    if (item != null) {
                        columnIndex = firstTraverseIndex > 0 ? firstTraverseIndex : 0;
                    } else {
                        return;
                    }
                }
                showEditor(item);
                table.setSelection(item);
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
            }
        } else if (e.detail == SWT.TRAVERSE_ESCAPE && editor != null) {
            if (!isProposalPopupActive()) {
                this.closeEditor();
            }
            e.doit = false;
            e.detail = SWT.TRAVERSE_NONE;
        }
    }

    protected abstract Control createEditor(Table table, int index, TableItem item);

    protected abstract void saveEditorValue(Control control, int index, TableItem item);

}
