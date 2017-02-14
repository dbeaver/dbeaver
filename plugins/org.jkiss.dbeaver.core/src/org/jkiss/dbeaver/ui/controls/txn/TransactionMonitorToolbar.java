/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DataSource Toolbar
 */
public class TransactionMonitorToolbar {
    private static final Log log = Log.getLog(TransactionMonitorToolbar.class);

    private IWorkbenchWindow workbenchWindow;

    public TransactionMonitorToolbar(IWorkbenchWindow workbenchWindow) {
        this.workbenchWindow = workbenchWindow;
    }

/*
    private void dispose() {
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage != null) {
            pageClosed(activePage);
        }

        this.workbenchWindow.removePageListener(this);
    }
*/

    private Control createControl(Composite parent) {
        return new MonitorPanel(parent);
    }

    private class MonitorPanel extends Composite {

        private final Text txnText;

        public MonitorPanel(Composite parent) {
            super(parent, SWT.NONE);
            //setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            setToolTipText("Transactions monitor");
            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            GridLayout layout = new GridLayout(1, false);
            layout.marginHeight = 0;
            //layout.marginWidth = 0;
            setLayout(layout);

            txnText = new Text(this, SWT.BORDER | SWT.SINGLE);
            txnText.setEnabled(false);
            txnText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            txnText.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            txnText.setText("");
            txnText.setToolTipText("Transactions monitor");
            GridData gd = new GridData();
            gd.verticalAlignment = GridData.CENTER;
            gd.grabExcessVerticalSpace = true;
            gd.widthHint = UIUtils.getFontHeight(txnText) * 6;
            txnText.setLayoutData(gd);
        }

        private void paint(PaintEvent e) {
            //e.gc.drawRectangle(e.x, e.y, e.width, e.height);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            return super.computeSize(wHint, hHint, changed);
        }
    }

    public static class ToolbarContribution extends WorkbenchWindowControlContribution {
        public ToolbarContribution() {
            super(IActionConstants.TOOLBAR_TXN);
        }

        @Override
        protected Control createControl(Composite parent) {
            TransactionMonitorToolbar toolbar = new TransactionMonitorToolbar(DBeaverUI.getActiveWorkbenchWindow());
            return toolbar.createControl(parent);
        }
    }

}
