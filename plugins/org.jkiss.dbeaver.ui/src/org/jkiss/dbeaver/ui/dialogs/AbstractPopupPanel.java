/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * SelectObjectDialog
 *
 * @author Serge Rider
 */
public abstract class AbstractPopupPanel extends Dialog {

    private final String title;
    private boolean modeless;
    private static boolean popupOpen;

    public static boolean isPopupOpen() {
        return popupOpen;
    }

    protected AbstractPopupPanel(Shell parentShell, String title)
    {
        super(parentShell);
        this.title = title;
    }

    @Override
    protected Point getInitialSize() {
        Point initialSize = super.getInitialSize();
        Rectangle maxBounds = Display.getCurrent().getBounds();
        initialSize.x = Math.min(initialSize.x, maxBounds.width - maxBounds.width / 50);
        return initialSize;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    protected boolean isModeless() {
        return modeless;
    }

    protected boolean isShowTitle() {
        return true;
    }

    public void setModeless(boolean modeless) {
        this.modeless = modeless;
        if (modeless) {
            setShellStyle(SWT.RESIZE | (isShowTitle() ? (SWT.CLOSE | SWT.TITLE | SWT.MIN | SWT.MAX) : SWT.NONE));
        } else {
            setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX | SWT.RESIZE);
        }
    }

    @Override
    public int open() {
        popupOpen = true;
        try {
            return super.open();
        } finally {
            popupOpen = false;
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        if (this.modeless) {
            return UIUtils.createPlaceholder(parent, 1);
        }
        return super.createButtonBar(parent);
    }

    protected void closeOnFocusLost(Control ... controls) {
        if (modeless) {
            FocusAdapter focusListener = new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    UIUtils.asyncExec(() -> {
                        handleFocusLost(e);
                    });
                }
            };
            for (Control ctrl : controls) {
                if (ctrl != null) {
                    ctrl.addFocusListener(focusListener);
                }
            }
        }

    }

    private void handleFocusLost(FocusEvent e) {
        Shell shell = getShell();
        if (shell != null && !shell.isDisposed()) {
            Control focusControl = shell.getDisplay().getFocusControl();
            if (focusControl != null && !UIUtils.isParent(shell, focusControl)) {
                Object dialog = focusControl.getShell().getData();
                if (dialog instanceof BlockingPopupDialog || dialog instanceof ErrorDialog) {
                    // It is an error popup
                    return;
                }
                cancelPressed();
            }
        } else {
            cancelPressed();
        }
    }

}
