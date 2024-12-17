/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.txn;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSavepoint;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.qm.QMTransactionState;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;
import org.jkiss.dbeaver.ui.*;

/**
 * DataSource Toolbar
 */
public class TransactionMonitorToolbar {

    private static final int MONITOR_UPDATE_DELAY = 250;
    private static final Log log = Log.getLog(TransactionMonitorToolbar.class);

    private static final RGB RGB_DARK_YELLOW = new RGB (128, 128, 0);
    private static final RGB RGB_DARK_GREEN = new RGB (0, 255, 0);

    private IWorkbenchWindow workbenchWindow;

    private TransactionMonitorToolbar(IWorkbenchWindow workbenchWindow) {
        this.workbenchWindow = workbenchWindow;
    }

    private Control createControl(Composite parent) {
        final MonitorPanel monitorPanel = new MonitorPanel(parent);

        final IPartListener partListener = new AbstractPartListener() {
            @Override
            public void partActivated(IWorkbenchPart part) {
                if (part instanceof DBPContextProvider) {
                    monitorPanel.refresh();
                }
            }

            @Override
            public void partDeactivated(IWorkbenchPart part) {
                //monitorPanel.refresh();
            }
        };
        final IWorkbenchPage activePage = this.workbenchWindow.getActivePage();
        if (activePage != null) {
            activePage.addPartListener(partListener);
            monitorPanel.addDisposeListener(e -> activePage.removePartListener(partListener));
        }

        return monitorPanel;
    }

    private class RefreshJob extends Job {
        private final MonitorPanel monitorPanel;

        RefreshJob(MonitorPanel monitorPanel) {
            super("Refresh transaction monitor");
            setSystem(true);
            setUser(false);
            this.monitorPanel = monitorPanel;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                monitorPanel.updateTransactionsInfo(new DefaultProgressMonitor(monitor));
            } catch (Throwable e) {
                log.debug("Error updating transaction info: " + e.getMessage());
            }
            return Status.OK_STATUS;
        }
    }

    private class MonitorPanel extends Composite {

        private QMEventsHandler qmHandler;
        private RefreshJob refreshJob;
        private QMTransactionState txnState;

        MonitorPanel(Composite parent) {
            super(parent, SWT.BORDER);
            setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            addPaintListener(this::paint);

            setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            setToolTipText(CoreMessages.monitor_panel_transaction_monitor_tip);

            refreshJob = new RefreshJob(this);

            qmHandler = new QMEventsHandler(this);
            QMUtils.registerHandler(qmHandler);

            addDisposeListener(e -> {
                QMUtils.unregisterHandler(qmHandler);
                qmHandler = null;
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    TransactionLogDialog.showDialog(getShell(), getActiveExecutionContext());
                }
            });
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            final int fontHeight = UIUtils.getFontHeight(this);
            int panelWidth = fontHeight * 8;
            Point point = super.computeSize(wHint, hHint, changed);
            if (point.x < panelWidth) {
                point.x = panelWidth;
            }
            return point;
        }

        @Override
        public void setToolTipText(String string) {
            super.setToolTipText(string);
        }

        private void paint(PaintEvent e) {
            Color bg;

            ColorRegistry colorRegistry = workbenchWindow.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

            final int updateCount = txnState == null ? 0 : txnState.getUpdateCount();

            if (txnState == null || !txnState.isTransactionMode()) {
                bg = UIStyles.getDefaultTextBackground();
            } else if (updateCount == 0) {
                bg = colorRegistry.get(BaseEditorColors.COLOR_TRANSACTION);
            } else {
                // Use gradient depending on update count
                ISharedTextColors sharedColors = UIUtils.getSharedTextColors();

                Color colorReverted = colorRegistry.get(BaseEditorColors.COLOR_REVERTED);
                Color colorCommitted = colorRegistry.get(BaseEditorColors.COLOR_UNCOMMITTED);
                final RGB COLOR_FULL = colorReverted == null ? RGB_DARK_YELLOW : colorReverted.getRGB();
                final RGB COLOR_EMPTY = colorCommitted == null ? RGB_DARK_GREEN : colorCommitted.getRGB();

                int minCount = 0, maxCount = 400;
                int ratio = ((updateCount - minCount) * 100) / (maxCount - minCount);
                if (updateCount >= maxCount) {
                    bg = sharedColors.getColor(COLOR_FULL);
                } else {
                    final RGB rgb = UIUtils.blend(COLOR_FULL, COLOR_EMPTY, ratio);
                    bg = sharedColors.getColor(rgb);
                }
            }
            Rectangle bounds = getBounds();
            e.gc.setBackground(bg);
            e.gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            String count;
            if (txnState == null) {
                count = "N/A";
            } else if (!txnState.isTransactionMode()) {
                count = "Auto";
            } else if (updateCount > 0) {
                count = String.valueOf(updateCount);
            } else {
                count = "None";
            }
            final Point textSize = e.gc.textExtent(count);
            e.gc.setForeground(UIStyles.getDefaultTextForeground());
            e.gc.drawString(count, bounds.x + (bounds.width - textSize.x) / 2 - 2, bounds.y + (bounds.height - textSize.y) / 2 - 1);
        }

        public void refresh() {
            refreshJob.schedule(MONITOR_UPDATE_DELAY);
        }

        void updateTransactionsInfo(DBRProgressMonitor monitor) {
            monitor.beginTask("Extract active transaction info", 1);

            DBCExecutionContext executionContext = getActiveExecutionContext();

            this.txnState = executionContext == null ? null : QMUtils.getTransactionState(executionContext);
            monitor.done();

            // Update UI
            UIUtils.asyncExec(() -> {
                if (isDisposed()) {
                    return;
                }
                redraw();
                updateToolTipText();
            });
        }

        @Nullable
        private DBCExecutionContext getActiveExecutionContext() {
            if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
                return null;
            }
            DBCExecutionContext executionContext = null;
            final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
            if (activeEditor instanceof DBPContextProvider) {
                executionContext = ((DBPContextProvider) activeEditor).getExecutionContext();
            }
            return executionContext;
        }

        private void updateToolTipText() {
            if (txnState == null) {
                setToolTipText(ModelMessages.error_not_connected_to_database);
            } else if (txnState.isTransactionMode()) {
                final long txnUptime = txnState.getTransactionStartTime() > 0 ?
                    ((System.currentTimeMillis() - txnState.getTransactionStartTime()) / 1000) + 1 : 0;
                String toolTip = String.valueOf(txnState.getExecuteCount()) + " total statements\n" +
                    String.valueOf(txnState.getUpdateCount()) + " modifying statements";
                if (txnUptime > 0) {
                    toolTip += "\n" + String.valueOf(txnUptime) + " seconds uptime";
                }
                setToolTipText(toolTip);
            } else {
                setToolTipText(CoreMessages.action_menu_transactionMonitor_autocommitMode);
            }
        }
    }

    public static class ToolbarContribution extends WorkbenchWindowControlContribution {
        public ToolbarContribution() {
            super(IActionConstants.TOOLBAR_TXN);
        }

        @Override
        protected Control createControl(Composite parent) {
            TransactionMonitorToolbar toolbar = new TransactionMonitorToolbar(UIUtils.getActiveWorkbenchWindow());
            return toolbar.createControl(parent);
        }
    }

    private static class QMEventsHandler extends DefaultExecutionHandler {
        private final MonitorPanel monitorPanel;

        QMEventsHandler(MonitorPanel monitorPanel) {
            this.monitorPanel = monitorPanel;
        }

        @NotNull
        @Override
        public String getHandlerName() {
            return QMEventsHandler.class.getName();
        }

        private void refreshMonitor() {
            if (!monitorPanel.isDisposed()) {
                monitorPanel.refresh();
            }
        }

        @Override
        public synchronized void handleTransactionAutocommit(@NotNull DBCExecutionContext context, boolean autoCommit) {
            refreshMonitor();
        }

        @Override
        public synchronized void handleTransactionCommit(@NotNull DBCExecutionContext context) {
            refreshMonitor();
            DBExecUtils.recoverSmartCommit(context);
        }

        @Override
        public synchronized void handleTransactionRollback(@NotNull DBCExecutionContext context, DBCSavepoint savepoint) {
            refreshMonitor();
            DBExecUtils.recoverSmartCommit(context);
        }

/*
        @Override
        public synchronized void handleStatementExecuteBegin(@NotNull DBCStatement statement) {
            refreshMonitor();
        }
*/

        @Override
        public void handleStatementExecuteEnd(@NotNull DBCStatement statement, long rows, Throwable error) {
            refreshMonitor();
        }

        @Override
        public void handleContextOpen(@NotNull DBCExecutionContext context, boolean transactional) {
            refreshMonitor();
        }

        @Override
        public void handleContextClose(@NotNull DBCExecutionContext context) {
            refreshMonitor();
        }
    }

}
