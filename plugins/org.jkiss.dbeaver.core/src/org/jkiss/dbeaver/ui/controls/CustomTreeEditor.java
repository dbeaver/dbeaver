/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Tree editor
 */
public abstract class CustomTreeEditor implements MouseListener, TraverseListener {

    private final Tree tree;
    private final TreeEditor treeEditor;
    private int columnIndex;
    protected int firstTraverseIndex = -1, lastTraverseIndex = -1;

    public CustomTreeEditor(Tree tree) {
        this.tree = tree;

        treeEditor = new TreeEditor(tree);
        treeEditor.horizontalAlignment = SWT.CENTER;
        treeEditor.verticalAlignment = SWT.TOP;
        treeEditor.grabHorizontal = true;
        treeEditor.minimumWidth = 50;

        tree.addMouseListener(this);
        tree.addTraverseListener(this);
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
        final TreeItem item = tree.getItem(new Point(e.x, e.y));
        if (item != null) {
            columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
            item.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    showEditor(item);
                }
            });
        }
    }

    public void showEditor(TreeItem item) {
        closeEditor();
        tree.showItem(item);
        final Control editor = createEditor(tree, columnIndex, item);
        if (editor == null) {
            return;
        }
        editor.setFocus();
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                saveEditorValue(editor, columnIndex, treeEditor.getItem());
                closeEditor();
            }
        });
        editor.addTraverseListener(this);
        treeEditor.setEditor(editor, item, columnIndex);
    }

    public void closeEditor() {
        Control oldEditor = this.treeEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    @Override
    public void keyTraversed(TraverseEvent e)
    {
        Control editor = treeEditor.getEditor();
        if (editor != null && editor.isDisposed()) {
            editor = null;
        }
        if (e.detail == SWT.TRAVERSE_RETURN) {
            if (editor != null) {
                saveEditorValue(editor, columnIndex, treeEditor.getItem());
                closeEditor();
            } else {
                TreeItem[] selection = tree.getSelection();
                if (selection != null && selection.length >= 1) {
                    showEditor(selection[0]);
                }
            }
            e.doit = false;
            e.detail = SWT.TRAVERSE_NONE;
        } else if (e.detail == SWT.TRAVERSE_TAB_NEXT && editor != null) {
            TreeItem item = treeEditor.getItem();
            if (item != null) {
                saveEditorValue(editor, columnIndex, item);
                closeEditor();

                int lastColumn = lastTraverseIndex > 0 ? lastTraverseIndex : tree.getColumnCount() - 1;
                if (columnIndex < lastColumn) {
                    columnIndex++;
                } else {
                    item = UIUtils.getNextTreeItem(tree, treeEditor.getItem());
                    if (item != null) {
                        columnIndex = firstTraverseIndex > 0 ? firstTraverseIndex : 0;
                    } else {
                        return;
                    }
                }
                showEditor(item);
                tree.setSelection(item);
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
            }
        } else if (e.detail == SWT.TRAVERSE_ESCAPE && editor != null) {
            closeEditor();
            e.doit = false;
            e.detail = SWT.TRAVERSE_NONE;
        }
    }

    protected abstract Control createEditor(Tree tree, int index, TreeItem item);

    protected abstract void saveEditorValue(Control control, int index, TreeItem item);

}
