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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.QMQueryFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.TextGetSetEditingSupport;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StringUtils;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

final class ResultSetFilterDialog extends AbstractPopupPanel {
    private final DBCExecutionContext executionContext;
    private final IResultSetFilterManager filterManager;
    private final String query;

    private final List<QMQueryFilter> filters;
    private int selection;

    ResultSetFilterDialog(
        @Nullable Shell parentShell,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<QMQueryFilter> filters,
        @NotNull IResultSetFilterManager filterManager,
        @NotNull String query
    ) {
        super(parentShell, "Table filters");
        this.executionContext = executionContext;
        this.filterManager = filterManager;
        this.query = query;

        this.filters = filters;

        setModeless(true);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        var composite = super.createDialogArea(parent);
        ((GridLayout) composite.getLayout()).numColumns = 2;

        var searchText = new Text(composite, SWT.SEARCH | SWT.ICON_SEARCH);
        searchText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        searchText.setMessage("Enter expression or title to search");

        var toolBar = new ToolBar(composite, SWT.FLAT);
        TableViewer viewer = createTable(composite, filters);
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(@NotNull Viewer viewer, @NotNull Object parentElement, @NotNull Object element) {
                var filter = (QMQueryFilter) element;
                var criteria = searchText.getText().trim();
                persistFilter(filter);
                return criteria.isEmpty()
                    || StringUtils.containsIgnoreCase(filter.text(), criteria)
                    || (filter.title() != null && StringUtils.containsIgnoreCase(filter.title(), criteria));
            }
        });
        viewer.addSelectionChangedListener(e -> {
            var filter = (QMQueryFilter) e.getStructuredSelection().getFirstElement();
            selection = filters.indexOf(filter);
        });
        viewer.getTable().addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(selectionEvent ->
            okPressed()));
        viewer.getTable().addMouseListener(MouseListener.mouseDoubleClickAdapter(event -> {
            if (event.widget instanceof Table t &&
                t.getSelection().length > 0 &&
                UIUtils.getColumnAtPos(t.getSelection()[0], event.x, event.y) == 0
            ) {
                okPressed();
            }
        }));

        searchText.addModifyListener(e -> viewer.refresh());

        UIUtils.createToolItem(
            toolBar,
            "Remove selected filter",
            UIIcon.ROW_DELETE,
            SelectionListener.widgetSelectedAdapter(e -> {
                var filter = (QMQueryFilter) viewer.getStructuredSelection().getFirstElement();
                if (filter != null) {
                    deleteFilter(filter);
                    filters.remove(filter);
                    viewer.refresh();
                }
            })
        );

        closeOnFocusLost(searchText, toolBar, viewer.getTable());

        return composite;
    }

    @Override
    protected boolean needsButtonBar() {
        return true;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
    }

    @NotNull
    private TableViewer createTable(@NotNull Composite composite, @NotNull List<QMQueryFilter> filters) {
        var viewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        viewer.setContentProvider(new ListContentProvider());
        viewer.setInput(filters);

        var table = viewer.getTable();
        table.setHeaderVisible(true);

        GridDataFactory.fillDefaults()
            .grab(true, true)
            .span(2, 1)
            .hint(800, 300)
            .applyTo(table);

        var exprColumn = new TableViewerColumn(viewer, SWT.LEFT);
        exprColumn.getColumn().setText("Expression");
        exprColumn.getColumn().setToolTipText("Expression of the filter");
        exprColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((QMQueryFilter) element).text();
            }
        });

        var titleColumn = new TableViewerColumn(viewer, SWT.LEFT);
        titleColumn.getColumn().setText("Title");
        titleColumn.getColumn().setToolTipText("Title of the filter");
        titleColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return CommonUtils.notEmpty(((QMQueryFilter) element).title());
            }
        });
        titleColumn.setEditingSupport(new TextGetSetEditingSupport<QMQueryFilter>(viewer,
            f -> CommonUtils.notEmpty(f.title()),
            (f, s) -> {
                if (f != null && !CommonUtils.isEmpty(s)) {
                    f.setTitle(s);
                    persistFilter(f);
                }
        }));

        var lastUsedColumn = new TableViewerColumn(viewer, SWT.LEFT);
        lastUsedColumn.getColumn().setText("Last used");
        lastUsedColumn.getColumn().setToolTipText("The last time the filter was used");
        lastUsedColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                var filter = (QMQueryFilter) element;
                return filter.lastUsed() != null ? formatInstant(filter.lastUsed()) : "N/A";
            }
        });

        var timesUsedColumn = new TableViewerColumn(viewer, SWT.LEFT);
        timesUsedColumn.getColumn().setText("Times used");
        timesUsedColumn.getColumn().setToolTipText("The number of times the filter was used");
        timesUsedColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                var filter = (QMQueryFilter) element;
                return NumberFormat.getInstance().format(filter.useCount());
            }
        });

        UIUtils.asyncExec(() -> {
            float[] weights = {0.45f, 0.25f, 0.2f, 0.10f};
            int totalWeight = table.getClientArea().width;
            int remainingWeight = totalWeight;
            for (int i = 0; i < weights.length; i++) {
                int columnWidth = i != weights.length - 1 ? (int) (totalWeight * weights[i]) : remainingWeight;
                var column = viewer.getTable().getColumn(i);
                column.setWidth(columnWidth);
                column.setMoveable(false);
                remainingWeight -= columnWidth;
            }
        });

        return viewer;
    }

    @Nullable
    QMQueryFilter getSelectedFilter() {
        if (selection >= 0 && selection < filters.size()) {
            return filters.get(selection);
        } else {
            return null;
        }
    }

    private void persistFilter(@NotNull QMQueryFilter filter) {
        try {
            filterManager.saveQueryFilterValue(executionContext, filter);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Save filter failed", null, e);
        }
    }

    private void deleteFilter(@NotNull QMQueryFilter filter) {
        try {
            filterManager.deleteQueryFilterValue(executionContext, filter);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Save filter failed", null, e);
        }
    }

    @NotNull
    private static String formatInstant(@Nullable Instant instant) {
        if (instant == null) {
            return "Never used";
        } else {
            var localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            if (localDateTime.getDayOfYear() == LocalDateTime.now().getDayOfYear()) {
                // Same day, show only time
                return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(localDateTime);
            } else {
                return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(localDateTime);
            }
        }
    }

}
