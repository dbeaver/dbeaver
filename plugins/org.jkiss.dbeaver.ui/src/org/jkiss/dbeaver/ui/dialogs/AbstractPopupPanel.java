/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * SelectObjectDialog
 *
 * @author Serge Rider
 */
public abstract class AbstractPopupPanel extends BaseDialog {

    private boolean modeless;
    private boolean closeOnOutsideClickInstalled;
    private static boolean popupOpen;

    public static boolean isPopupOpen() {
        return popupOpen;
    }

    protected AbstractPopupPanel(Shell parentShell, String title)
    {
        super(parentShell, title, null);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        Dialog.applyDialogFont(contents);
        return contents;
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
            setShellStyle(SWT.RESIZE | (isShowTitle() ? (SWT.CLOSE | SWT.TITLE | SWT.MAX) : SWT.NONE));
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
        newShell.setText(getTitle());
    }

    @Override
    protected Control createButtonBar(@NotNull Composite parent) {
        if (needsButtonBar()) {
            return super.createButtonBar(parent);
        } else {
            return UIUtils.createPlaceholder(parent, 1);
        }
    }

    protected boolean needsButtonBar() {
        return !modeless;
    }

    /**
     * Closes a modeless popup when the user clicks outside of it.
     * <p>
     * Keyboard focus is deliberately not used here: with focus-follows-mouse window
     * managers (e.g. GNOME with {@code focus-mode='sloppy'}) the popup loses focus as
     * soon as the pointer moves over the parent window, which would close the popup
     * before the user could reach it.
     */
    protected void closeOnFocusLost(Control ... controls) {
        if (modeless && !closeOnOutsideClickInstalled) {
            closeOnOutsideClickInstalled = installCloseOnOutsideClick();
        }
    }

    /**
     * Installs a listener that will close the dialog if it loses focus.
     *
     * @param shell the shell to monitor
     */
    protected void closeOnFocusLost(@NotNull Shell shell) {
        shell.addShellListener(ShellListener.shellDeactivatedAdapter(e -> UIUtils.asyncExec(this::handleFocusLost)));
    }

    private boolean installCloseOnOutsideClick() {
        Shell popupShell = getShell();
        if (popupShell == null || popupShell.isDisposed()) {
            return false;
        }
        Display display = popupShell.getDisplay();
        Listener mouseFilter = event -> {
            if (event.type != SWT.MouseDown) {
                return;
            }
            Shell shell = getShell();
            if (shell == null || shell.isDisposed()) {
                return;
            }
            if (event.widget instanceof Control clicked) {
                Shell clickedShell = clicked.getShell();
                // Ignore clicks inside the popup and its child shells (e.g. a drop-down opened from it)
                if (isPopupShell(shell, clickedShell)) {
                    return;
                }
                // Ignore clicks in transient shells (context menus, combo drop-downs, tooltips)
                // which may not be parented to the popup shell
                if ((clickedShell.getStyle() & SWT.ON_TOP) != 0) {
                    return;
                }
            }
            UIUtils.asyncExec(() -> {
                Shell currentShell = getShell();
                if (currentShell != null && !currentShell.isDisposed()) {
                    cancelPressed();
                }
            });
        };
        display.addFilter(SWT.MouseDown, mouseFilter);
        popupShell.addDisposeListener(e -> display.removeFilter(SWT.MouseDown, mouseFilter));
        return true;
    }

    private static boolean isPopupShell(Shell popupShell, Shell shell) {
        for (Composite parent = shell; parent != null; parent = parent.getParent()) {
            if (parent == popupShell) {
                return true;
            }
        }
        return false;
    }

    private void handleFocusLost() {
        Shell shell = getShell();
        if (shell != null && !shell.isDisposed()) {
            Control focusControl = shell.getDisplay().getFocusControl();
            if (focusControl != null && !UIUtils.isParent(shell, focusControl)) {
                Object dialogData = focusControl.getShell().getData();
                if (dialogData instanceof MessageBox ||
                    dialogData instanceof MessageBoxModern ||
                    dialogData instanceof BlockingPopupDialog ||
                    dialogData instanceof ErrorDialog
                ) {
                    return;
                }
                cancelPressed();
            }
        } else {
            cancelPressed();
        }
    }

}
