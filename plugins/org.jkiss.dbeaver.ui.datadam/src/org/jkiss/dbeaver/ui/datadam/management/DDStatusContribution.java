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

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.impl.GlobalPropertyTester;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DDStatusContribution extends ContributionItem {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public DDStatusContribution() {
        super("org.jkiss.dbeaver.ui.datadam.monitor");
    }

    public static class Factory extends ExtensionContributionFactory {
        @Override
        public void createContributionItems(@NotNull IServiceLocator serviceLocator, @NotNull IContributionRoot additions) {
            additions.addContributionItem(new DDStatusContribution(), new Expression() {
                @Override
                @NotNull
                public EvaluationResult evaluate(@NotNull IEvaluationContext context) {
                    return EvaluationResult.valueOf(DDProductConfig.isIntegrationEnabled());
                }

                @Override
                public void collectExpressionInfo(@NotNull ExpressionInfo info) {
                    info.addAccessedPropertyName(GlobalPropertyTester.NAMESPACE + "." + GlobalPropertyTester.PROP_HAS_PREFERENCE);
                }
            });
        }
    }

    @Override
    public void fill(@NotNull ToolBar parent, int index) {
        ToolItem item = index < 0 ? new ToolItem(parent, SWT.DROP_DOWN) : new ToolItem(parent, SWT.DROP_DOWN, index);
        item.setText("DataDam");
        MenuManager menu = new MenuManager();
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(manager -> manager.add(new DDManagementMenu()));
        var contextMenu = menu.createContextMenu(parent);
        parent.setMenu(contextMenu);
        item.addListener(SWT.Selection, event -> {
            var bounds = item.getBounds();
            contextMenu.setLocation(parent.toDisplay(bounds.x, bounds.y + bounds.height));
            contextMenu.setVisible(true);
        });
        var display = parent.getDisplay();
        Runnable refreshTask = new Runnable() {
            @Override
            public void run() {
                if (!item.isDisposed()) {
                    refresh(item);
                    display.timerExec(1_000, this);
                }
            }
        };
        item.addDisposeListener(event -> {
            display.timerExec(-1, refreshTask);
            menu.dispose();
        });
        refreshTask.run();
    }

    private void refresh(@NotNull ToolItem item) {
        if (item.isDisposed()) {
            return;
        }
        String status = DDTrackingUIMessages.status_unknown;
        DBIcon icon = DDTrackingUIIcons.STATUS_UNKNOWN;
        String details = "";
        if (!DDProductConfig.isIntegrationEnabled()) {
            status = DDTrackingUIMessages.status_disabled;
        } else if (DBDamAccessService.getInstance().getStatus() != DBDamAccessService.Status.SIGNED_IN) {
            status = DDTrackingUIMessages.status_not_logged_in;
        } else {
            var monitor = DDManagementActions.getMonitor();
            if (monitor != null) {
                var snapshot = monitor.snapshot();
                status = switch (snapshot.state()) {
                    case CONNECTED -> DDTrackingUIMessages.status_connected;
                    case CONNECTING -> DDTrackingUIMessages.status_connecting;
                    case DISCONNECTED, CLOSED -> DDTrackingUIMessages.status_disconnected;
                };
                icon = switch (snapshot.state()) {
                    case CONNECTED -> UIIcon.BULLET_GREEN;
                    case CONNECTING -> DDTrackingUIIcons.STATUS_UNKNOWN;
                    case DISCONNECTED, CLOSED -> UIIcon.BULLET_RED;
                };
                if (snapshot.connectedAt() != null) {
                    details += "\n" + NLS.bind(DDTrackingUIMessages.connected_at, TIME_FORMAT.format(snapshot.connectedAt()));
                    details += "\n" + NLS.bind(DDTrackingUIMessages.connection_duration, snapshot.duration().toSeconds());
                }
                details += "\n" + NLS.bind(DDTrackingUIMessages.bytes_sent, snapshot.bytesSent());
                details += "\n" + NLS.bind(DDTrackingUIMessages.bytes_received, snapshot.bytesReceived());
                if (snapshot.error() != null) {
                    details += "\n" + snapshot.error();
                }
            }
        }
        item.setImage(DBeaverIcons.getImage(icon));
        item.setToolTipText("DataDam: " + status + details);
    }
}
