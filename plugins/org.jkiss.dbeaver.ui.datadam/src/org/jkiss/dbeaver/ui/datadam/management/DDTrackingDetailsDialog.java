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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStandardLabel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class DDTrackingDetailsDialog extends AbstractPopupPanel {
    private final DDConnectionMonitor.Event event;
    private final String time;
    private final String type;

    DDTrackingDetailsDialog(
        @NotNull Shell shell, @NotNull DDConnectionMonitor.Event event, @NotNull String time, @NotNull String type
    ) {
        super(shell, DDTrackingUIMessages.event_details);
        setModeless(true);
        setBlockOnOpen(false);
        this.event = event;
        this.time = time;
        this.type = type;
    }

    @Override
    @NotNull
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        var request = event.request();
        Text text = new Text(area, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
        boolean response = event.response() != null;
        GridData data = new GridData(SWT.FILL, request == null || response ? SWT.FILL : SWT.TOP, true, request == null || response);
        data.widthHint = 600;
        data.heightHint = request == null || response ? 240 : 80;
        text.setLayoutData(data);
        text.setText(time + "\n" + type
            + (event.requestId() == 0 ? "" : "\n" + DDTrackingUIMessages.mcp_request_id + ": " + event.requestId())
            + "\n\n" + (request == null || response ? event.fullDetails() : request.operation()));
        if (request != null && !response) {
            createParameters(area, request.parameters());
        }
        closeOnFocusLost();
        return area;
    }

    private void createParameters(@NotNull Composite parent, @NotNull Map<String, String> parameters) {
        TableViewer viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        var table = viewer.getTable();
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 600;
        data.heightHint = 240;
        table.setLayoutData(data);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        createColumn(viewer, UIMessages.properties_name, 180, Map.Entry::getKey);
        createColumn(viewer, UIMessages.properties_value, 400, Map.Entry::getValue);
        viewer.setInput(parameters.entrySet());
        Action copy = new Action(UIStandardLabel.COPY.getLabel(), DBeaverIcons.getImageDescriptor(UIIcon.COPY)) {
            @Override
            public void run() {
                String values = viewer.getStructuredSelection().stream()
                    .map(element -> ((Map.Entry<?, ?>) element).getValue().toString())
                    .collect(Collectors.joining(System.lineSeparator()));
                UIUtils.setClipboardContents(table.getDisplay(), TextTransfer.getInstance(), values);
            }
        };
        copy.setEnabled(false);
        viewer.addSelectionChangedListener(change -> copy.setEnabled(!viewer.getStructuredSelection().isEmpty()));
        MenuManager menu = new MenuManager();
        menu.add(copy);
        table.setMenu(menu.createContextMenu(table));
        table.addDisposeListener(dispose -> menu.dispose());
        table.addListener(SWT.KeyDown, key -> {
            if ((key.stateMask & SWT.MOD1) != 0 && key.keyCode == 'c' && copy.isEnabled()) {
                copy.run();
                key.doit = false;
            }
        });
    }

    private void createColumn(
        @NotNull TableViewer viewer, @NotNull String title, int width, @NotNull Function<Map.Entry<String, String>, String> text
    ) {
        var column = new TableViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            @NotNull
            @SuppressWarnings("unchecked")
            public String getText(@NotNull Object element) {
                return text.apply((Map.Entry<String, String>) element);
            }
        });
    }
}
