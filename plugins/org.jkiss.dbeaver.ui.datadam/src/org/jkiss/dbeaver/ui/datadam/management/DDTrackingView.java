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
package org.jkiss.dbeaver.ui.datadam.management;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStandardLabel;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class DDTrackingView extends ViewPart {
    public static final String ID = "org.jkiss.dbeaver.ui.datadam.tracking";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    private TableViewer viewer;
    private String filterPattern = "";
    private List<DDConnectionMonitor.Event> displayed = List.of();
    private final Runnable refreshTask = this::refresh;

    @Override
    public void createPartControl(@NotNull Composite parent) {
        parent.setLayout(new FillLayout());
        Composite content = UIUtils.createPlaceholder(parent, 1, 0);
        Text filterText = new Text(content, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        filterText.setMessage(WorkbenchMessages.FilteredTree_FilterMessage);
        UIUtils.installAndUpdateMainFont(filterText);
        viewer = new TableViewer(content, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(@NotNull Viewer source, @Nullable Object parentElement, @NotNull Object element) {
                var event = (DDConnectionMonitor.Event) element;
                if (filterPattern.isEmpty() || StringUtils.containsIgnoreCase(event.details(), filterPattern)
                    || event.response() != null && StringUtils.containsIgnoreCase(event.response().body(), filterPattern)
                    || StringUtils.containsIgnoreCase(formatResult(event), filterPattern)
                    || StringUtils.containsIgnoreCase(eventLabel(event.type()), filterPattern)) {
                    return true;
                }
                var connection = getConnection(event);
                return connection != null && StringUtils.containsIgnoreCase(connection.getName(), filterPattern);
            }
        });
        Table table = viewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Runnable applyFilter = () -> {
            if (!table.isDisposed()) {
                filterPattern = filterText.getText().trim();
                viewer.refresh();
            }
        };
        filterText.addModifyListener(event -> {
            filterText.getDisplay().timerExec(-1, applyFilter);
            filterText.getDisplay().timerExec(200, applyFilter);
        });
        filterText.addListener(SWT.DefaultSelection, event -> {
            if (event.detail == SWT.ICON_CANCEL) {
                filterText.setText("");
            }
        });
        table.addDisposeListener(event -> table.getDisplay().timerExec(-1, applyFilter));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        BaseThemeSettings.instance.addPropertyListener(BaseEditorColors.COLOR_ERROR, settings -> viewer.refresh(), table);
        BaseThemeSettings.instance.addPropertyListener(BaseEditorColors.COLOR_WARNING, settings -> viewer.refresh(), table);
        createColumn(DDTrackingUIMessages.event_time, 165, event -> TIME_FORMAT.format(event.time()));
        createColumn(DDTrackingUIMessages.event_type, 170, event -> eventLabel(event.type()));
        createColumn(DDTrackingUIMessages.mcp_request_id, 100, event -> event.requestId() == 0 ? "" : Long.toString(event.requestId()));
        createColumn(DDTrackingUIMessages.mcp_result_code, 180, DDTrackingView::formatResult);
        createColumn(DDTrackingUIMessages.event_details, 600, event -> event.requestId() != 0
            ? event.details().lines().findFirst().orElse("")
                + (event.response() == null || event.response().error() == null ? "" : ": " + event.response().error())
            : event.details());
        createColumn(DDTrackingUIMessages.event_database, 200, new EventLabelProvider(event -> {
            var connection = getConnection(event);
            return connection == null ? "" : connection.getName();
        }) {
            @Override
            @Nullable
            public Image getImage(@NotNull Object element) {
                var connection = getConnection((DDConnectionMonitor.Event) element);
                return connection == null ? null : DBeaverIcons.getImage(DBValueFormatting.getObjectImage(connection, false));
            }
        });
        viewer.addDoubleClickListener(event -> showDetails());
        table.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) {
                showDetails();
                event.doit = false;
            }
        });
        table.addDisposeListener(event -> table.getDisplay().timerExec(-1, refreshTask));
        Action clear = new Action(DDTrackingUIMessages.clear_events, DBeaverIcons.getImageDescriptor(UIIcon.CLEAN)) {
            @Override
            public void run() {
                var monitor = DDManagementActions.getMonitor();
                if (monitor != null) {
                    monitor.clearEvents();
                    refreshRows(monitor);
                }
            }
        };
        getViewSite().getActionBars().getToolBarManager().add(clear);
        createContextMenu(clear);
        refresh();
    }

    private void createContextMenu(@NotNull Action clear) {
        Action details = new Action(UIStandardLabel.SHOW_DETAILS.getLabel(), DBeaverIcons.getImageDescriptor(UIIcon.RS_DETAILS)) {
            @Override
            public void run() {
                showDetails();
            }
        };
        details.setEnabled(false);
        Action copy = new Action(UIStandardLabel.COPY.getLabel(), DBeaverIcons.getImageDescriptor(UIIcon.COPY)) {
            @Override
            public void run() {
                var selected = (DDConnectionMonitor.Event) viewer.getStructuredSelection().getFirstElement();
                if (selected != null && !selected.details().isEmpty()) {
                    UIUtils.setClipboardContents(viewer.getControl().getDisplay(), TextTransfer.getInstance(),
                        DDTrackingDetailsDialog.formatDetails(selected));
                }
            }
        };
        copy.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
        copy.setEnabled(false);
        viewer.addSelectionChangedListener(event -> {
            var selected = (DDConnectionMonitor.Event) viewer.getStructuredSelection().getFirstElement();
            details.setEnabled(selected != null);
            copy.setEnabled(selected != null && !selected.details().isEmpty());
        });
        Action preferences = new Action(DDTrackingUIMessages.configuration,
            DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
            @Override
            public void run() {
                DDManagementActions.configure();
            }
        };
        MenuManager menu = new MenuManager();
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(manager -> {
            manager.add(details);
            manager.add(copy);
            var selected = (DDConnectionMonitor.Event) viewer.getStructuredSelection().getFirstElement();
            var connection = selected == null ? null : getConnection(selected);
            if (connection != null) {
                manager.add(new Action(DDTrackingUIMessages.show_in_navigator) {
                    @Override
                    public void run() {
                        var project = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(connection.getProject());
                        if (project != null) {
                            var node = project.getDatabases().getDataSource(connection);
                            if (node != null) {
                                NavigatorUtils.showNodeInNavigator(node);
                            }
                        }
                    }
                });
            }
            manager.add(clear);
            manager.add(new Separator());
            manager.add(preferences);
        });
        viewer.getControl().setMenu(menu.createContextMenu(viewer.getControl()));
        viewer.getControl().addDisposeListener(event -> menu.dispose());
        getViewSite().setSelectionProvider(viewer);
        getViewSite().registerContextMenu(menu, viewer);
        getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
    }

    private void createColumn(@NotNull String title, int width, @NotNull Function<DDConnectionMonitor.Event, String> text) {
        createColumn(title, width, new EventLabelProvider(text));
    }

    private void createColumn(@NotNull String title, int width, @NotNull ColumnLabelProvider labels) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(labels);
    }

    private static class EventLabelProvider extends ColumnLabelProvider {
        private final Function<DDConnectionMonitor.Event, String> text;

        private EventLabelProvider(@NotNull Function<DDConnectionMonitor.Event, String> text) {
            this.text = text;
        }

        @Override
        @Nullable
        public Color getBackground(@NotNull Object element) {
            var event = (DDConnectionMonitor.Event) element;
            if (event.isError()) {
                return BaseThemeSettings.instance.colorError;
            }
            return event.isPending() ? BaseThemeSettings.instance.colorWarning : null;
        }

        @Override
        @Nullable
        public Color getForeground(@NotNull Object element) {
            Color background = getBackground(element);
            return background == null ? null : UIStyles.getContrastColor(background);
        }

        @Override
        @NotNull
        public String getText(@NotNull Object element) {
            return text.apply((DDConnectionMonitor.Event) element);
        }
    }

    @Nullable
    private DBPDataSourceContainer getConnection(@NotNull DDConnectionMonitor.Event event) {
        var request = event.request();
        if (request == null || request.projectId() == null || request.datasourceId() == null) {
            return null;
        }
        for (var project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            if (request.projectId().equals(project.getId()) && project.isOpen() && project.isRegistryLoaded()) {
                return project.getDataSourceRegistry().getDataSource(request.datasourceId());
            }
        }
        return null;
    }

    private void refresh() {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        var monitor = DDManagementActions.getMonitor();
        if (monitor != null) {
            refreshRows(monitor);
        }
        viewer.getControl().getDisplay().timerExec(1_000, refreshTask);
    }

    private void refreshRows(@NotNull DDConnectionMonitor monitor) {
        var events = monitor.getEvents();
        if (events.equals(displayed)) {
            return;
        }
        var selection = viewer.getSelection();
        var selected = (DDConnectionMonitor.Event) viewer.getStructuredSelection().getFirstElement();
        Table table = viewer.getTable();
        int top = table.getTopIndex();
        table.setRedraw(false);
        try {
            viewer.setInput(events);
            if (selected != null && selected.requestId() != 0) {
                var updated = events.stream().filter(event -> event.requestId() == selected.requestId()).findFirst().orElse(null);
                viewer.setSelection(updated == null ? StructuredSelection.EMPTY : new StructuredSelection(updated));
            } else {
                viewer.setSelection(selection);
            }
            table.setTopIndex(top);
            displayed = events;
        } finally {
            table.setRedraw(true);
        }
    }

    @NotNull
    private static String formatResult(@NotNull DDConnectionMonitor.Event event) {
        if (event.isPending()) {
            return DDTrackingUIMessages.mcp_pending;
        }
        var response = event.response();
        return response == null ? "" : NLS.bind(DDTrackingUIMessages.mcp_result_with_time,
            response.code(), response.processingTime().toMillis());
    }

    private void showDetails() {
        var event = (DDConnectionMonitor.Event) viewer.getStructuredSelection().getFirstElement();
        if (event == null) {
            return;
        }
        new DDTrackingDetailsDialog(viewer.getControl().getShell(), event, eventLabel(event.type())).open();
    }

    @NotNull
    static String eventLabel(@NotNull DDConnectionMonitor.EventType type) {
        return switch (type) {
            case CONNECTING -> DDTrackingUIMessages.status_connecting;
            case CONNECTED -> DDTrackingUIMessages.status_connected;
            case DISCONNECTED, CLOSED -> DDTrackingUIMessages.status_disconnected;
            case SIGNED_IN -> DDTrackingUIMessages.signed_in;
            case SIGNED_OUT -> DDTrackingUIMessages.signed_out;
            case INVALID_TOKEN -> DDTrackingUIMessages.invalid_token;
            case ERROR -> DDTrackingUIMessages.error;
            case MCP_REQUEST -> DDTrackingUIMessages.mcp_request;
            case MCP_RESPONSE -> DDTrackingUIMessages.mcp_response;
            case NAME_CHANGED -> DDTrackingUIMessages.name_changed;
        };
    }

    public static void show() {
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ID);
        } catch (PartInitException e) {
            DDManagementActions.showError(e);
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
