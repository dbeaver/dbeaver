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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStandardLabel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class DDTrackingDetailsDialog extends AbstractPopupPanel {
    private static final String PREF_SHOW_RESPONSE = "datadam.tracking.showResponse";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());

    private DDConnectionMonitor.Event event;
    private final String type;
    private final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
    private Text responseCodeText;
    private Text processingTimeText;
    private Text responseSizeText;
    private Composite errorRow;
    private Text errorText;
    private Button showResponseButton;
    private Composite responsePanel;
    private Text responseText;
    private boolean responseValueLoaded;

    DDTrackingDetailsDialog(
        @NotNull Shell shell, @NotNull DDConnectionMonitor.Event event, @NotNull String type
    ) {
        super(shell, DDTrackingUIMessages.event_details);
        setModeless(true);
        setBlockOnOpen(false);
        this.event = event;
        this.type = type;
    }

    @Override
    @NotNull
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite metadata = UIUtils.createComposite(area, 4);
        GridData metadataData = new GridData(SWT.FILL, SWT.TOP, true, false);
        metadataData.widthHint = 760;
        metadata.setLayoutData(metadataData);
        UIUtils.createLabelText(metadata, DDTrackingUIMessages.event_time, TIME_FORMAT.format(event.time()), SWT.READ_ONLY | SWT.BORDER);
        UIUtils.createLabelText(metadata, DDTrackingUIMessages.event_type, type, SWT.READ_ONLY | SWT.BORDER);
        if (event.requestId() == 0) {
            GridData detailsData = new GridData(SWT.FILL, SWT.FILL, true, true);
            detailsData.heightHint = 120;
            detailsData.horizontalSpan = 3;
            UIUtils.createLabelText(metadata, DDTrackingUIMessages.event_details, event.details(),
                SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER, detailsData);
        } else {
            createRequestDetails(area, metadata);
        }
        if (event.isPending()) {
            followResponse(area);
        }
        closeOnFocusLost();
        return area;
    }

    private void createRequestDetails(@NotNull Composite area, @NotNull Composite metadata) {
        UIUtils.createLabelText(metadata, DDTrackingUIMessages.mcp_request_id, Long.toString(event.requestId()),
            SWT.READ_ONLY | SWT.BORDER);
        String function = event.details().lines().findFirst().orElse("");
        int separator = function.indexOf(": ");
        UIUtils.createLabelText(metadata, DDTrackingUIMessages.mcp_function,
            separator < 0 ? function : function.substring(separator + 2), SWT.READ_ONLY | SWT.BORDER);
        responseCodeText = UIUtils.createLabelText(metadata, DDTrackingUIMessages.mcp_response_code, "", SWT.READ_ONLY | SWT.BORDER);
        processingTimeText = UIUtils.createLabelText(metadata, DDTrackingUIMessages.mcp_processing_time_label, "",
            SWT.READ_ONLY | SWT.BORDER);
        responseSizeText = UIUtils.createLabelText(metadata, DDTrackingUIMessages.mcp_response_size, "", SWT.READ_ONLY | SWT.BORDER);

        errorRow = UIUtils.createComposite(metadata, 2);
        GridData errorData = new GridData(SWT.FILL, SWT.TOP, true, false);
        errorData.horizontalSpan = 4;
        errorRow.setLayoutData(errorData);
        errorText = UIUtils.createLabelText(errorRow, DDTrackingUIMessages.error, "", SWT.READ_ONLY | SWT.BORDER);

        Composite parametersBar = UIUtils.createComposite(area, 2);
        parametersBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        UIUtils.createControlLabel(parametersBar, DDTrackingUIMessages.mcp_input_parameters)
            .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        showResponseButton = new Button(parametersBar, SWT.TOGGLE);
        showResponseButton.setText(DDTrackingUIMessages.mcp_show_response);
        showResponseButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        showResponseButton.setSelection(preferenceStore.getBoolean(PREF_SHOW_RESPONSE));
        showResponseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(change -> {
            preferenceStore.setValue(PREF_SHOW_RESPONSE, showResponseButton.getSelection());
            PrefUtils.savePreferenceStore(preferenceStore);
            updateResponse();
        }));

        var request = event.request();
        createParameters(area, request == null ? Map.of() : request.parameters());

        responsePanel = UIUtils.createComposite(area, 1);
        responsePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        responseText = new Text(responsePanel, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        responseText.setTabs(2);
        UIUtils.fixReadonlyTextBackground(responseText);
        UIUtils.applyMonospaceFont(responseText);
        GridData responseData = new GridData(SWT.FILL, SWT.FILL, true, true);
        responseData.widthHint = 600;
        responseData.heightHint = 200;
        responseText.setLayoutData(responseData);
        updateResponse();
    }

    private void updateResponse() {
        var response = event.response();
        responseCodeText.setText(response == null ? DDTrackingUIMessages.mcp_pending : response.code());
        processingTimeText.setText(response == null ? ""
            : NLS.bind(DDTrackingUIMessages.mcp_duration, response.processingTime().toMillis()));
        responseSizeText.setText(response == null ? "" : Integer.toString(response.body().getBytes(StandardCharsets.UTF_8).length));
        String error = response == null ? null : response.error();
        errorText.setText(error == null ? "" : error);
        UIUtils.setControlVisible(errorRow, error != null);
        UIUtils.setControlVisible(showResponseButton, response != null);

        boolean visible = response != null && showResponseButton.getSelection();
        boolean visibilityChanged = responsePanel.getVisible() != visible;
        if (visible && !responseValueLoaded) {
            responseText.setText(response.valueText());
            responseValueLoaded = true;
        }
        UIUtils.setControlVisible(responsePanel, visible);
        var shell = getShell();
        shell.layout(true, true);
        if (visibilityChanged && shell.isVisible()) {
            var preferred = shell.computeSize(shell.getSize().x, SWT.DEFAULT, true);
            shell.setSize(shell.getSize().x, Math.min(preferred.y, shell.getMonitor().getClientArea().height));
        }
    }

    @NotNull
    static String formatDetails(@NotNull DDConnectionMonitor.Event event) {
        if (event.requestId() == 0) {
            return event.fullDetails();
        }
        StringBuilder details = new StringBuilder(DDTrackingUIMessages.mcp_request).append('\n').append(event.details());
        var response = event.response();
        if (response == null) {
            return details.append("\n\n").append(DDTrackingUIMessages.mcp_pending).toString();
        }
        details.append("\n\n").append(DDTrackingUIMessages.mcp_result_code).append(": ").append(response.code());
        details.append('\n').append(NLS.bind(DDTrackingUIMessages.mcp_processing_time, response.processingTime().toMillis()));
        if (response.error() != null) {
            details.append('\n').append(DDTrackingUIMessages.error).append(": ").append(response.error());
        }
        return details.append("\n\n").append(DDTrackingUIMessages.mcp_response).append('\n').append(response.body()).toString();
    }

    private void followResponse(@NotNull Composite area) {
        var display = area.getDisplay();
        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                if (area.isDisposed()) {
                    return;
                }
                var monitor = DDManagementActions.getMonitor();
                var updated = monitor == null ? null : monitor.getRequestEvent(event.requestId());
                if (updated == null) {
                    return;
                }
                if (event != updated) {
                    event = updated;
                    updateResponse();
                }
                if (event.isPending()) {
                    display.timerExec(1_000, this);
                }
            }
        };
        area.addDisposeListener(dispose -> display.timerExec(-1, refresh));
        display.timerExec(1_000, refresh);
    }

    private void createParameters(@NotNull Composite parent, @NotNull Map<String, String> parameters) {
        TableViewer viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        var table = viewer.getTable();
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 600;
        data.heightHint = 180;
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
