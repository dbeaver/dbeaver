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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
import java.util.List;

public final class ResultSetFilterDialog extends BaseDialog {
    private static final Log log = Log.getLog(ResultSetFilterDialog.class);

    private final DBCExecutionContext executionContext;
    private final IResultSetFilterManager filterManager;
    private final String query;

    public ResultSetFilterDialog(
        @Nullable Shell parentShell,
        @NotNull DBCExecutionContext executionContext,
        @NotNull IResultSetFilterManager filterManager,
        @NotNull String query
    ) {
        super(parentShell, "Table filters", null);
        this.executionContext = executionContext;
        this.filterManager = filterManager;
        this.query = query;

        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        var composite = super.createDialogArea(parent);

        var searchText = new Text(composite, SWT.SEARCH | SWT.ICON_SEARCH);
        searchText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        searchText.setMessage("Enter expression or title to search");

        List<MutableQueryFilter> filters;
        try {
            filters = loadFilters();
        } catch (DBException e) {
            log.error("Error loading filters history", e);
            DBWorkbench.getPlatformUI().showError("Error loading filters", "An error occurred while loading filters history", e);
            filters = List.of();
        }

        createTable(composite, filters);

        var toolBar = new ToolBar(composite, SWT.FLAT);
        UIUtils.createToolItem(
            toolBar,
            "Add",
            UIIcon.ROW_ADD,
            SelectionListener.widgetSelectedAdapter(e -> System.out.println("Add"))
        );
        UIUtils.createToolItem(
            toolBar,
            "Remove",
            UIIcon.ROW_DELETE,
            SelectionListener.widgetSelectedAdapter(e -> System.out.println("Remove"))
        );

        return composite;
    }

    private static void createTable(@NotNull Composite composite, @NotNull List<MutableQueryFilter> filters) {
        var viewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        viewer.setContentProvider(new ListContentProvider());
        viewer.setInput(filters);

        var table = viewer.getTable();
        table.setHeaderVisible(true);

        GridDataFactory.fillDefaults()
            .grab(true, false)
            .hint(600, 300)
            .applyTo(table);

        var controller = new ViewerColumnController<Object, MutableQueryFilter>("ResultSetFilterDialogTable", viewer);
        controller.addColumn(
            "Expression",
            "Expression of the filter",
            SWT.LEFT,
            true,
            true,
            MutableQueryFilter::getFilter,
            new TextGetSetEditingSupport<>(viewer, MutableQueryFilter::getFilter, MutableQueryFilter::setFilter)
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
            e -> formatInstant(e.original.lastUsed()),
            null
        );
        controller.addColumn(
            "Times used",
            "The number of times the filter was used",
            SWT.LEFT,
            false,
            false,
            e -> NumberFormat.getInstance().format(e.original.useCount()),
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
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Use Selected", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @NotNull
    private List<MutableQueryFilter> loadFilters() throws DBException {
        return filterManager.getQueryFilterHistory(executionContext, query).stream()
            .map(MutableQueryFilter::new)
            .toList();
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
        private String filter;
        private String title;

        MutableQueryFilter(@NotNull QMQueryFilter original) {
            this.original = original;
            this.filter = original.filter();
            this.title = CommonUtils.notEmpty(original.title());
        }

        @NotNull
        String getFilter() {
            return filter;
        }

        void setFilter(@NotNull String filter) {
            this.filter = filter;
        }

        @NotNull
        String getTitle() {
            return title;
        }

        void setTitle(@NotNull String title) {
            this.title = title;
        }
    }
}
