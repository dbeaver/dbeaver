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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.*;

/**
 * SelectObjectDialog
 *
 * @author Serge Rider
 */
public abstract class AbstractPopupPanel extends Dialog {


    private final String title;
    private boolean modeless;

    protected AbstractPopupPanel(Shell parentShell, String title)
    {
        super(parentShell);
        this.title = title;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    public boolean isModeless() {
        return modeless;
    }

    public void setModeless(boolean modeless) {
        this.modeless = modeless;
        if (modeless) {
            setShellStyle(SWT.SHELL_TRIM);
        } else {
            setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX | SWT.RESIZE);
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
                        if (getShell() != null && !UIUtils.isParent(getShell(), getShell().getDisplay().getFocusControl())) {
                            cancelPressed();
                        }
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

}
