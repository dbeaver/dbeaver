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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.controls.CustomCheckboxCellEditor;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** A preference-styled boolean column without permanent SWT controls above selected rows. */
public final class McpBooleanColumn {
    private McpBooleanColumn() {
    }

    public static void create(
        @NotNull TreeViewer viewer,
        @NotNull String title,
        @NotNull String tooltip,
        @NotNull Function<DBPDataSourceContainer, Boolean> getValue,
        @NotNull BiConsumer<DBPDataSourceContainer, Boolean> setValue,
        @NotNull BooleanSupplier visible
    ) {
        TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.CENTER);
        column.getColumn().setText(title);
        column.getColumn().setWidth(100);
        column.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(Object element) {
                return "";
            }

            @Nullable
            @Override
            public String getToolTipText(Object element) {
                return element instanceof DBPDataSourceContainer ? tooltip : null;
            }
        });

        Tree tree = viewer.getTree();
        int columnIndex = tree.indexOf(column.getColumn());
        ObjectViewerRenderer renderer = new ObjectViewerRenderer(viewer, false) {
            @Nullable
            @Override
            public Object getCellValue(Object element, int index) {
                return index == columnIndex && element instanceof DBPDataSourceContainer connection
                    ? getValue.apply(connection) : null;
            }
        };
        tree.addDisposeListener(event -> renderer.dispose());
        tree.addListener(SWT.PaintItem, event -> paintCell(event, columnIndex, getValue, visible, renderer));
        column.setEditingSupport(new EditingSupport(viewer) {
            @NotNull
            @Override
            protected CellEditor getCellEditor(Object element) {
                return new CustomCheckboxCellEditor(tree, true);
            }

            @Override
            protected boolean canEdit(Object element) {
                return element instanceof DBPDataSourceContainer && visible.getAsBoolean();
            }

            @NotNull
            @Override
            protected Object getValue(Object element) {
                return getValue.apply((DBPDataSourceContainer) element);
            }

            @Override
            protected void setValue(Object element, Object value) {
                if (element instanceof DBPDataSourceContainer connection && value instanceof Boolean checked && visible.getAsBoolean()) {
                    setValue.accept(connection, checked);
                    tree.redraw();
                }
            }
        });
    }

    private static void paintCell(
        @NotNull Event event,
        int columnIndex,
        @NotNull Function<DBPDataSourceContainer, Boolean> getValue,
        @NotNull BooleanSupplier visible,
        @NotNull ObjectViewerRenderer renderer
    ) {
        if (event.index == columnIndex && visible.getAsBoolean() && event.item instanceof TreeItem item
            && item.getData() instanceof DBPDataSourceContainer connection) {
            renderer.paintCell(event, connection, getValue.apply(connection), item, Boolean.class, columnIndex,
                true, (event.detail & SWT.SELECTED) != 0);
        }
    }
}
