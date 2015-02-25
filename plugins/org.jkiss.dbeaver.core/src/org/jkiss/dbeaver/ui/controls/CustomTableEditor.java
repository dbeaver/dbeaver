/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls;

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
    private int columnIndex;

    public CustomTableEditor(Table table) {
        this.table = table;

        tableEditor = new TableEditor(table);
        tableEditor.horizontalAlignment = SWT.CENTER;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;
        tableEditor.minimumWidth = 50;

        table.addMouseListener(this);
        table.addTraverseListener(this);
    }

    @Override
    public void mouseDoubleClick(MouseEvent e) {

    }

    @Override
    public void mouseDown(MouseEvent e) {

    }

    @Override
    public void mouseUp(MouseEvent e) {
        TableItem item = table.getItem(new Point(e.x, e.y));
        if (item != null) {
            columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            showEditor(item);
        }
    }

    public void showEditor(TableItem item) {
        closeEditor();
        table.showItem(item);
        final Control editor = createEditor(table, columnIndex, item);
        if (editor == null) {
            return;
        }
        editor.setFocus();
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                saveEditorValue(editor, columnIndex, tableEditor.getItem());
                closeEditor();
            }
        });
        editor.addTraverseListener(this);
        tableEditor.setEditor(editor, item, columnIndex);
    }

    public void closeEditor() {
        Control oldEditor = this.tableEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
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
                closeEditor();
            } else {
                TableItem[] selection = table.getSelection();
                if (selection != null && selection.length >= 1) {
                    showEditor(selection[0]);
                }
            }
            e.doit = false;
            e.detail = SWT.TRAVERSE_NONE;
        } else if (e.detail == SWT.TRAVERSE_TAB_NEXT && editor != null) {
            if (tableEditor.getItem() != null) {
                if (columnIndex < table.getColumnCount() - 1) {
                    columnIndex++;
                } else {
                    columnIndex = 0;
                }
                showEditor(tableEditor.getItem());
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
            }
        } else if (e.detail == SWT.TRAVERSE_ESCAPE && editor != null) {
            closeEditor();
            e.doit = false;
            e.detail = SWT.TRAVERSE_NONE;
        }
    }

    protected abstract Control createEditor(Table table, int index, TableItem item);

    protected abstract void saveEditorValue(Control control, int index, TableItem item);

}
