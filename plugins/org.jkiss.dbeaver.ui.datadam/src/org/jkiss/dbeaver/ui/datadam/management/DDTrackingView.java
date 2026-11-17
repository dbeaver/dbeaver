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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStandardLabel;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDManagementMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class DDTrackingView extends ViewPart {
    public static final String ID = "org.jkiss.dbeaver.ui.datadam.tracking";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());
    private TableViewer viewer;
    private List<DDConnectionMonitor.Event> displayed = List.of();
    private final Runnable refreshTask = this::refresh;

    @Override
    public void createPartControl(@NotNull Composite parent) {
        parent.setLayout(new FillLayout());
        viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        BaseThemeSettings.instance.addPropertyListener(BaseEditorColors.COLOR_ERROR, settings -> viewer.refresh(), table);
        createColumn(DDManagementMessages.event_time, 190, event -> TIME_FORMAT.format(event.time()));
        createColumn(DDManagementMessages.event_type, 170, event -> eventLabel(event.type()));
        createColumn(DDManagementMessages.event_details, 600, event -> event.type() == DDConnectionMonitor.EventType.MCP_REQUEST
            ? event.details().lines().findFirst().orElse("") : event.details());
        viewer.addOpenListener(event -> showDetails());
        table.addDisposeListener(event -> table.getDisplay().timerExec(-1, refreshTask));
        Action clear = new Action(DDManagementMessages.clear_events, DBeaverIcons.getImageDescriptor(UIIcon.CLEAN)) {
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
                    UIUtils.setClipboardContents(viewer.getControl().getDisplay(), TextTransfer.getInstance(), selected.details());
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
        Action preferences = new Action(DDManagementMessages.configuration,
            DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
            @Override
            public void run() {
                DDManagementActions.configure();
            }
        };
        MenuManager menu = new MenuManager();
        menu.add(details);
        menu.add(copy);
        menu.add(clear);
        menu.add(new Separator());
        menu.add(preferences);
        viewer.getControl().setMenu(menu.createContextMenu(viewer.getControl()));
        viewer.getControl().addDisposeListener(event -> menu.dispose());
        getViewSite().setSelectionProvider(viewer);
        getViewSite().registerContextMenu(menu, viewer);
        getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
    }

    private void createColumn(@NotNull String title, int width, @NotNull Function<DDConnectionMonitor.Event, String> text) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            @Nullable
            public Color getBackground(@NotNull Object element) {
                return ((DDConnectionMonitor.Event) element).type() == DDConnectionMonitor.EventType.ERROR
                    ? BaseThemeSettings.instance.colorError : null;
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
        });
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
        Table table = viewer.getTable();
        int top = table.getTopIndex();
        table.setRedraw(false);
        try {
            viewer.setInput(events);
            viewer.setSelection(selection);
            table.setTopIndex(top);
            displayed = events;
        } finally {
            table.setRedraw(true);
        }
    }

    private void showDetails() {
        var event = (DDConnectionMonitor.Event) viewer.getStructuredSelection().getFirstElement();
        if (event == null) {
            return;
        }
        new BaseDialog(viewer.getControl().getShell(), DDManagementMessages.event_details, null) {
            @Override
            @NotNull
            protected Composite createDialogArea(@NotNull Composite parent) {
                Composite area = super.createDialogArea(parent);
                Text text = new Text(area, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
                GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
                data.widthHint = 600;
                data.heightHint = 240;
                text.setLayoutData(data);
                text.setText(TIME_FORMAT.format(event.time()) + "\n" + eventLabel(event.type()) + "\n\n" + event.details());
                return area;
            }

            @Override
            protected void createButtonsForButtonBar(@NotNull Composite parent) {
                createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            }

        }.open();
    }

    @NotNull
    static String eventLabel(@NotNull DDConnectionMonitor.EventType type) {
        return switch (type) {
            case CONNECTING -> DDManagementMessages.status_connecting;
            case CONNECTED -> DDManagementMessages.status_connected;
            case DISCONNECTED, CLOSED -> DDManagementMessages.status_disconnected;
            case SIGNED_IN -> DDManagementMessages.signed_in;
            case SIGNED_OUT -> DDManagementMessages.signed_out;
            case INVALID_TOKEN -> DDManagementMessages.invalid_token;
            case ERROR -> DDManagementMessages.error;
            case MCP_REQUEST -> DDManagementMessages.mcp_request;
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
