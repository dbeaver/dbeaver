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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Places SWT controls in viewer cells without creating controls for offscreen rows. */
public final class CellControlColumn {
    private CellControlColumn() {
    }

    public static <T, C extends Control> void attach(
        @NotNull TreeViewer viewer,
        @NotNull TreeViewerColumn column,
        @NotNull Class<T> elementType,
        @NotNull BiFunction<Composite, T, C> createControl,
        @NotNull BiConsumer<C, T> updateControl,
        @NotNull BooleanSupplier enabled
    ) {
        createLabelProvider(column);
        Tree tree = viewer.getTree();
        TreeSupport<T, C> support = new TreeSupport<>(
            tree, column.getColumn(), tree.indexOf(column.getColumn()), elementType, createControl, updateControl, enabled);
        support.install();
        support.listen(column.getColumn(), SWT.Resize, event -> support.scheduleRefresh());
        support.listen(tree, SWT.Expand, event -> support.scheduleRefresh());
        support.listen(tree, SWT.Collapse, event -> support.scheduleRefresh());
    }

    public static <T, C extends Control> void attach(
        @NotNull TableViewer viewer,
        @NotNull TableViewerColumn column,
        @NotNull Class<T> elementType,
        @NotNull BiFunction<Composite, T, C> createControl,
        @NotNull BiConsumer<C, T> updateControl,
        @NotNull BooleanSupplier enabled
    ) {
        createLabelProvider(column);
        Table table = viewer.getTable();
        TableSupport<T, C> support = new TableSupport<>(
            table, column.getColumn(), table.indexOf(column.getColumn()), elementType, createControl, updateControl, enabled);
        support.install();
        support.listen(column.getColumn(), SWT.Resize, event -> support.scheduleRefresh());
    }

    public static void updateSelectionColors(
        @NotNull Control cellControl,
        @NotNull StructuredViewer viewer,
        @NotNull Object element
    ) {
        Control list = viewer.getControl();
        boolean selected = viewer.getStructuredSelection().toList().contains(element);
        Color background = selected ? list.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION) : list.getBackground();
        Color foreground = selected ? list.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT) : list.getForeground();
        if (cellControl.getBackground() != background) {
            cellControl.setBackground(background);
        }
        if (cellControl.getForeground() != foreground) {
            cellControl.setForeground(foreground);
        }
    }

    private static void createLabelProvider(@NotNull ViewerColumn column) {
        column.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(Object element) {
                return "";
            }
        });
    }

    private abstract static class Support<T, I extends Item, C extends Control> {
        @NotNull
        protected final Scrollable control;
        @NotNull
        private final Item column;
        protected final int columnIndex;
        @NotNull
        private final Class<T> elementType;
        @NotNull
        private final BiFunction<Composite, T, C> createControl;
        @NotNull
        private final BiConsumer<C, T> updateControl;
        @NotNull
        private final BooleanSupplier enabled;
        private final Map<I, CellControlEditor<T, C>> editors = new IdentityHashMap<>();
        private final List<Runnable> removers = new ArrayList<>();
        private boolean refreshScheduled;
        private boolean disposed;

        protected Support(
            @NotNull Scrollable control,
            @NotNull Item column,
            int columnIndex,
            @NotNull Class<T> elementType,
            @NotNull BiFunction<Composite, T, C> createControl,
            @NotNull BiConsumer<C, T> updateControl,
            @NotNull BooleanSupplier enabled
        ) {
            this.control = control;
            this.column = column;
            this.columnIndex = columnIndex;
            this.elementType = elementType;
            this.createControl = createControl;
            this.updateControl = updateControl;
            this.enabled = enabled;
        }

        protected void install() {
            listen(control, SWT.Paint, event -> scheduleRefresh());
            listen(control, SWT.Resize, event -> scheduleRefresh());
            listen(control, SWT.Selection, event -> scheduleRefresh());
            addScrollListener(control.getVerticalBar());
            addScrollListener(control.getHorizontalBar());
            control.addDisposeListener(event -> dispose());
            column.addDisposeListener(event -> dispose());
            scheduleRefresh();
        }

        protected void listen(@NotNull Widget widget, int eventType, @NotNull Listener listener) {
            widget.addListener(eventType, listener);
            removers.add(() -> {
                if (!widget.isDisposed()) {
                    widget.removeListener(eventType, listener);
                }
            });
        }

        private void addScrollListener(@Nullable ScrollBar bar) {
            if (bar != null) {
                listen(bar, SWT.Selection, event -> scheduleRefresh());
            }
        }

        protected void scheduleRefresh() {
            if (refreshScheduled || disposed || control.isDisposed() || column.isDisposed()) {
                return;
            }
            refreshScheduled = true;
            UIUtils.asyncExec(() -> {
                refreshScheduled = false;
                if (!disposed && !control.isDisposed() && !column.isDisposed()) {
                    refreshControls();
                }
            });
        }

        private void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            for (Runnable remove : removers) {
                remove.run();
            }
            removers.clear();
            for (CellControlEditor<T, C> editor : new ArrayList<>(editors.values())) {
                editor.dispose();
            }
            editors.clear();
        }

        private void refreshControls() {
            Set<I> visible = Collections.newSetFromMap(new IdentityHashMap<>());
            if (enabled.getAsBoolean() && getColumnWidth() > 0) {
                Rectangle viewport = control.getClientArea();
                viewport.y += getHeaderHeight();
                viewport.height = Math.max(0, viewport.height - getHeaderHeight());
                visitVisibleItems(viewport, item -> {
                    Object data = item.getData();
                    if (!elementType.isInstance(data)) {
                        return;
                    }
                    T element = elementType.cast(data);
                    visible.add(item);
                    CellControlEditor<T, C> editor = editors.get(item);
                    if (editor == null || editor.control().isDisposed() || editor.element() != element) {
                        removeEditor(item);
                        createEditor(item, element);
                    } else {
                        updateControl.accept(editor.control(), element);
                        editor.editor().layout();
                    }
                });
            }
            for (I item : new ArrayList<>(editors.keySet())) {
                if (!visible.contains(item)) {
                    removeEditor(item);
                }
            }
        }

        private void createEditor(@NotNull I item, @NotNull T element) {
            C cellControl = createControl.apply((Composite) control, element);
            updateControl.accept(cellControl, element);
            ControlEditor editor = createCellEditor(item, cellControl);
            editors.put(item, new CellControlEditor<>(editor, cellControl, element));
            item.addDisposeListener(event -> removeEditor(item));
        }

        private void removeEditor(@NotNull I item) {
            CellControlEditor<T, C> editor = editors.remove(item);
            if (editor != null) {
                editor.dispose();
            }
        }

        protected abstract int getHeaderHeight();

        protected abstract int getColumnWidth();

        protected abstract void visitVisibleItems(@NotNull Rectangle viewport, @NotNull Consumer<I> consumer);

        @NotNull
        protected abstract ControlEditor createCellEditor(@NotNull I item, @NotNull C cellControl);

        private record CellControlEditor<T, C extends Control>(
            @NotNull ControlEditor editor,
            @NotNull C control,
            @NotNull T element
        ) {
            private void dispose() {
                if (!control.isDisposed()) {
                    control.dispose();
                }
                editor.dispose();
            }
        }
    }

    private static final class TreeSupport<T, C extends Control> extends Support<T, TreeItem, C> {
        @NotNull
        private final Tree tree;

        private TreeSupport(
            @NotNull Tree tree,
            @NotNull Item column,
            int columnIndex,
            @NotNull Class<T> elementType,
            @NotNull BiFunction<Composite, T, C> createControl,
            @NotNull BiConsumer<C, T> updateControl,
            @NotNull BooleanSupplier enabled
        ) {
            super(tree, column, columnIndex, elementType, createControl, updateControl, enabled);
            this.tree = tree;
        }

        @Override
        protected int getHeaderHeight() {
            return tree.getHeaderHeight();
        }

        @Override
        protected int getColumnWidth() {
            return tree.getColumn(columnIndex).getWidth();
        }

        @Override
        protected void visitVisibleItems(@NotNull Rectangle viewport, @NotNull Consumer<TreeItem> consumer) {
            visitItems(tree.getItems(), viewport, consumer);
        }

        private void visitItems(
            @NotNull TreeItem[] items,
            @NotNull Rectangle viewport,
            @NotNull Consumer<TreeItem> consumer
        ) {
            for (TreeItem item : items) {
                if (item.getBounds(columnIndex).intersects(viewport)) {
                    consumer.accept(item);
                }
                if (item.getExpanded()) {
                    visitItems(item.getItems(), viewport, consumer);
                }
            }
        }

        @NotNull
        @Override
        protected ControlEditor createCellEditor(@NotNull TreeItem item, @NotNull C cellControl) {
            TreeEditor editor = new TreeEditor(tree);
            configureEditor(editor, tree.getItemHeight(), cellControl);
            editor.setEditor(cellControl, item, columnIndex);
            return editor;
        }
    }

    private static final class TableSupport<T, C extends Control> extends Support<T, TableItem, C> {
        @NotNull
        private final Table table;

        private TableSupport(
            @NotNull Table table,
            @NotNull Item column,
            int columnIndex,
            @NotNull Class<T> elementType,
            @NotNull BiFunction<Composite, T, C> createControl,
            @NotNull BiConsumer<C, T> updateControl,
            @NotNull BooleanSupplier enabled
        ) {
            super(table, column, columnIndex, elementType, createControl, updateControl, enabled);
            this.table = table;
        }

        @Override
        protected int getHeaderHeight() {
            return table.getHeaderHeight();
        }

        @Override
        protected int getColumnWidth() {
            return table.getColumn(columnIndex).getWidth();
        }

        @Override
        protected void visitVisibleItems(@NotNull Rectangle viewport, @NotNull Consumer<TableItem> consumer) {
            int first = table.getTopIndex();
            int last = Math.min(table.getItemCount(), first + viewport.height / Math.max(1, table.getItemHeight()) + 2);
            for (int index = first; index < last; index++) {
                TableItem item = table.getItem(index);
                if (item.getBounds(columnIndex).intersects(viewport)) {
                    consumer.accept(item);
                }
            }
        }

        @NotNull
        @Override
        protected ControlEditor createCellEditor(@NotNull TableItem item, @NotNull C cellControl) {
            TableEditor editor = new TableEditor(table);
            configureEditor(editor, table.getItemHeight(), cellControl);
            editor.setEditor(cellControl, item, columnIndex);
            return editor;
        }
    }

    private static void configureEditor(@NotNull ControlEditor editor, int itemHeight, @NotNull Control cellControl) {
        editor.horizontalAlignment = SWT.CENTER;
        editor.verticalAlignment = SWT.CENTER;
        editor.grabVertical = false;
        editor.minimumWidth = Math.max(itemHeight, cellControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
        editor.minimumHeight = itemHeight;
    }
}
