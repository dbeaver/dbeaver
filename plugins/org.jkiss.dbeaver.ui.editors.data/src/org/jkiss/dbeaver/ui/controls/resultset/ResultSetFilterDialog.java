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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
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

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ResultSetFilterDialog extends AbstractPopupPanel {
    private final DBCExecutionContext executionContext;
    private final IResultSetFilterManager filterManager;
    private final String query;

    private final List<MutableQueryFilter> filters = new ArrayList<>();
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

        for (QMQueryFilter filter : filters) {
            this.filters.add(new MutableQueryFilter(filter));
        }

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
        var viewer = createTable(composite, filters);
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(@NotNull Viewer viewer, @NotNull Object parentElement, @NotNull Object element) {
                var filter = (MutableQueryFilter) element;
                if (filter.deleted) {
                    // Don't show deleted filters
                    return false;
                }
                if (filter.persisted == null) {
                    // Always show new filters
                    return true;
                }
                var criteria = searchText.getText().trim();
                return criteria.isEmpty()
                    || filter.text.toLowerCase(Locale.ROOT).contains(criteria)
                    || filter.title.toLowerCase(Locale.ROOT).contains(criteria);
            }
        });
        viewer.addSelectionChangedListener(e -> {
            var filter = (MutableQueryFilter) e.getStructuredSelection().getFirstElement();
            selection = filters.indexOf(filter);

            var button = getButton(IDialogConstants.OK_ID);
            if (button != null) {
                button.setText(filter != null ? "Use Selected" : IDialogConstants.OK_LABEL);
            }
        });

        searchText.addModifyListener(e -> viewer.refresh());

        UIUtils.createToolItem(
            toolBar,
            "Add new filter",
            UIIcon.ROW_ADD,
            SelectionListener.widgetSelectedAdapter(e -> {
                var filter = new MutableQueryFilter();
                filters.add(filter);
                viewer.refresh();
                viewer.editElement(filter, 0);
            })
        );
        UIUtils.createToolItem(
            toolBar,
            "Remove selected filter",
            UIIcon.ROW_DELETE,
            SelectionListener.widgetSelectedAdapter(e -> {
                var filter = (MutableQueryFilter) viewer.getStructuredSelection().getFirstElement();
                if (filter != null) {
                    filter.deleted = true;
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
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        persistFilters();
        super.okPressed();
    }

    @NotNull
    private static TableViewer createTable(@NotNull Composite composite, @NotNull List<MutableQueryFilter> filters) {
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
                return ((MutableQueryFilter) element).getText();
            }
        });
        exprColumn.setEditingSupport(new TextGetSetEditingSupport<>(viewer, MutableQueryFilter::getText, MutableQueryFilter::setText) {
            @Override
            protected boolean canEdit(@NotNull Object element) {
                return ((MutableQueryFilter) element).persisted == null;
            }
        });

        var titleColumn = new TableViewerColumn(viewer, SWT.LEFT);
        titleColumn.getColumn().setText("Title");
        titleColumn.getColumn().setToolTipText("Title of the filter");
        titleColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((MutableQueryFilter) element).getTitle();
            }
        });
        titleColumn.setEditingSupport(new TextGetSetEditingSupport<>(viewer, MutableQueryFilter::getTitle, MutableQueryFilter::setTitle));

        var lastUsedColumn = new TableViewerColumn(viewer, SWT.LEFT);
        lastUsedColumn.getColumn().setText("Last used");
        lastUsedColumn.getColumn().setToolTipText("The last time the filter was used");
        lastUsedColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                var filter = (MutableQueryFilter) element;
                return filter.persisted != null ? formatInstant(filter.persisted.lastUsed()) : "N/A";
            }
        });

        var timesUsedColumn = new TableViewerColumn(viewer, SWT.LEFT);
        timesUsedColumn.getColumn().setText("Times used");
        timesUsedColumn.getColumn().setToolTipText("The number of times the filter was used");
        timesUsedColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                var filter = (MutableQueryFilter) element;
                return filter.persisted != null ? NumberFormat.getInstance().format(filter.persisted.useCount()) : "N/A";
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
            return filters.get(selection).persisted;
        } else {
            return null;
        }
    }

    private void persistFilters() {
        for (MutableQueryFilter filter : filters) {
            try {
                persistFilter(filter);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    "Error persisting filter",
                    "An error occurred while persisting filter '" + filter.getTitle() + "': " + e.getMessage()
                );
            }
        }
    }

    private void persistFilter(@NotNull MutableQueryFilter filter) throws DBException {
        if (filter.deleted) {
            if (filter.persisted != null) {
                filterManager.deleteQueryFilterValue(executionContext, filter.persisted);
                filter.persisted = null;
            }
        } else if (filter.modified) {
            if (filter.persisted != null) {
                filterManager.deleteQueryFilterValue(executionContext, filter.persisted);
                var newFilter = new QMQueryFilter(
                    filter.persisted.query(),
                    filter.text,
                    filter.title.isEmpty() ? null : filter.title,
                    filter.persisted.lastUsed(),
                    filter.persisted.useCount()
                );
                filterManager.saveQueryFilterValue(executionContext, newFilter);
            } else if (!filter.text.isBlank()) {
                var persisted = new QMQueryFilter(query, filter.text, filter.title, null, 0);
                filter.persisted = persisted;
                filterManager.saveQueryFilterValue(executionContext, persisted);
            }
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

    private static class MutableQueryFilter {
        private QMQueryFilter persisted;
        private String text;
        private String title;
        private boolean modified;
        private boolean deleted;

        MutableQueryFilter(@NotNull QMQueryFilter persisted) {
            this.persisted = persisted;
            this.text = persisted.text();
            this.title = CommonUtils.notEmpty(persisted.title());
        }

        MutableQueryFilter() {
            this.persisted = null;
            this.text = "";
            this.title = "";
        }

        @NotNull
        String getText() {
            return text;
        }

        void setText(@NotNull String text) {
            if (!this.text.equals(text)) {
                this.text = text;
                this.modified = true;
            }
        }

        @NotNull
        String getTitle() {
            return title;
        }

        void setTitle(@NotNull String title) {
            if (!this.title.equals(title)) {
                this.title = title;
                this.modified = true;
            }
        }
    }
}
