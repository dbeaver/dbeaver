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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
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

public final class ResultSetFilterDialog extends BaseDialog {
    private final DBCExecutionContext executionContext;
    private final IResultSetFilterManager filterManager;
    private final String query;

    private final List<MutableQueryFilter> filters = new ArrayList<>();
    private int selection;

    public ResultSetFilterDialog(
        @Nullable Shell parentShell,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<QMQueryFilter> filters,
        @NotNull IResultSetFilterManager filterManager,
        @NotNull String query
    ) {
        super(parentShell, "Table filters", null);
        this.executionContext = executionContext;
        this.filterManager = filterManager;
        this.query = query;

        for (QMQueryFilter filter : filters) {
            this.filters.add(new MutableQueryFilter(filter));
        }

        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
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
                var criteria = searchText.getText().trim();
                if (criteria.isEmpty()) {
                    return true;
                }
                var filter = (MutableQueryFilter) element;
                if (filter.deleted) {
                    // Don't show deleted filters
                    return false;
                }
                return filter.original == null
                    || filter.text.toLowerCase(Locale.ROOT).contains(criteria)
                    || filter.title.toLowerCase(Locale.ROOT).contains(criteria);
            }
        });
        viewer.addSelectionChangedListener(e -> {
            var filter = (MutableQueryFilter) e.getStructuredSelection().getFirstElement();
            selection = filters.indexOf(filter);
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

        return composite;
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
            .hint(600, 300)
            .applyTo(table);

        var controller = new ViewerColumnController<Object, MutableQueryFilter>("ResultSetFilterDialogTable", viewer);
        controller.addColumn(
            "Expression",
            "Expression of the filter",
            SWT.LEFT,
            true,
            true,
            MutableQueryFilter::getText,
            new TextGetSetEditingSupport<>(viewer, MutableQueryFilter::getText, MutableQueryFilter::setText) {
                @Override
                protected boolean canEdit(@NotNull Object element) {
                    return ((MutableQueryFilter) element).original == null;
                }
            }
        );
        controller.addColumn(
            "Title",
            "Title of the filter",
            SWT.LEFT,
            true,
            true,
            MutableQueryFilter::getTitle,
            new TextGetSetEditingSupport<>(viewer, MutableQueryFilter::getTitle, MutableQueryFilter::setTitle)
        );
        controller.addColumn(
            "Last used",
            "The last time the filter was used",
            SWT.LEFT,
            true,
            false,
            e -> e.original != null ? formatInstant(e.original.lastUsed()) : "N/A",
            null
        );
        controller.addColumn(
            "Times used",
            "The number of times the filter was used",
            SWT.LEFT,
            false,
            false,
            e -> e.original != null ? NumberFormat.getInstance().format(e.original.useCount()) : "N/A",
            null
        );
        controller.createColumns(false);

        var manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(m -> {
            if (controller.isClickOnHeader()) {
                controller.fillConfigMenu(m);
            }
        });
        table.setMenu(manager.createContextMenu(table));
        table.addDisposeListener(e -> manager.dispose());

        return viewer;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Use Selected", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        persistFilters();
        super.okPressed();
    }

    @Nullable
    QMQueryFilter getSelectedFilter() {
        if (selection >= 0 && selection < filters.size()) {
            return filters.get(selection).original;
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
            if (filter.original != null) {
                filterManager.deleteQueryFilterValue(executionContext, filter.original);
            }
        } else if (filter.modified) {
            if (filter.original != null) {
                filterManager.deleteQueryFilterValue(executionContext, filter.original);
                var newFilter = new QMQueryFilter(
                    filter.original.query(),
                    filter.text,
                    filter.title.isEmpty() ? null : filter.title,
                    filter.original.lastUsed(),
                    filter.original.useCount()
                );
                filterManager.saveQueryFilterValue(executionContext, newFilter);
            } else if (!filter.text.isBlank()) {
                var newFilter = new QMQueryFilter(query, filter.text, filter.title, null, 0);
                filterManager.saveQueryFilterValue(executionContext, newFilter);
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
        private final QMQueryFilter original;
        private String text;
        private String title;
        private boolean modified;
        private boolean deleted;

        MutableQueryFilter(@NotNull QMQueryFilter original) {
            this.original = original;
            this.text = original.text();
            this.title = CommonUtils.notEmpty(original.title());
        }

        MutableQueryFilter() {
            this.original = null;
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
