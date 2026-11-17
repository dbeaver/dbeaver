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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IStatusBarContributor;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.datadam.DDDesktopInitializer;
import org.jkiss.dbeaver.ui.datadam.internal.DDManagementMessages;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DDStatusContribution implements IStatusBarContributor {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    @Nullable
    private Composite control;
    private Label statusIcon;

    @Override
    public void createControls(@NotNull Composite parent) {
        DDDesktopInitializer.initialize();
        Composite widget = createWidget(parent);
        widget.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
        var display = widget.getDisplay();
        Runnable refreshTask = new Runnable() {
            @Override
            public void run() {
                if (!widget.isDisposed()) {
                    if (DDProductConfig.isIntegrationEnabled()) {
                        refresh();
                    }
                    display.timerExec(1_000, this);
                }
            }
        };
        parent.addDisposeListener(event -> display.timerExec(-1, refreshTask));
        refreshTask.run();
    }

    @NotNull
    private Composite createWidget(@NotNull Composite parent) {
        control = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 4;
        control.setLayout(layout);
        Label label = new Label(control, SWT.NONE);
        label.setText("DataDam");
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
        statusIcon = new Label(control, SWT.NONE);
        statusIcon.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
        MenuManager menu = new MenuManager();
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(manager -> manager.add(new DDManagementMenu()));
        var contextMenu = menu.createContextMenu(control);
        for (Control part : new Control[] { control, label, statusIcon }) {
            part.setMenu(contextMenu);
            part.addListener(SWT.MouseDoubleClick, event -> DDTrackingView.show());
        }
        control.addDisposeListener(event -> menu.dispose());
        refresh();
        return control;
    }

    private void refresh() {
        if (control == null || control.isDisposed()) {
            return;
        }
        var monitor = DDManagementActions.getMonitor();
        String status = DDManagementMessages.status_unknown;
        DBIcon icon = DDTrackingUIIcons.STATUS_UNKNOWN;
        String details = "";
        if (!DDProductConfig.isIntegrationEnabled()) {
            status = DDManagementMessages.status_disabled;
        } else if (monitor != null) {
            var snapshot = monitor.snapshot();
            status = switch (snapshot.state()) {
                case CONNECTED -> DDManagementMessages.status_connected;
                case CONNECTING -> DDManagementMessages.status_connecting;
                case DISCONNECTED, CLOSED -> DDManagementMessages.status_disconnected;
            };
            icon = switch (snapshot.state()) {
                case CONNECTED -> UIIcon.BULLET_GREEN;
                case CONNECTING -> DDTrackingUIIcons.STATUS_UNKNOWN;
                case DISCONNECTED, CLOSED -> UIIcon.BULLET_RED;
            };
            if (snapshot.connectedAt() != null) {
                details += "\n" + NLS.bind(DDManagementMessages.connected_at, TIME_FORMAT.format(snapshot.connectedAt()));
                details += "\n" + NLS.bind(DDManagementMessages.connection_duration, snapshot.duration().toSeconds());
            }
            details += "\n" + NLS.bind(DDManagementMessages.bytes_sent, snapshot.bytesSent());
            details += "\n" + NLS.bind(DDManagementMessages.bytes_received, snapshot.bytesReceived());
            if (snapshot.error() != null) {
                details += "\n" + snapshot.error();
            }
        }
        statusIcon.setImage(DBeaverIcons.getImage(icon));
        String tooltip = "DataDam: " + status + details;
        control.setToolTipText(tooltip);
        for (Control part : control.getChildren()) {
            part.setToolTipText(tooltip);
        }
    }
}
