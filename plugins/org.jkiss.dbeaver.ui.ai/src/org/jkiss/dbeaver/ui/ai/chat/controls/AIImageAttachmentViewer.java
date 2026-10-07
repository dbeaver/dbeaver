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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;

final class AIImageAttachmentViewer {
    private AIImageAttachmentViewer() {
    }

    static void open(@NotNull Shell shell, @NotNull AIImageAttachment image) {
        new Dialog(shell) {
            {
                setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
            }

            @Override
            protected void configureShell(@NotNull Shell newShell) {
                super.configureShell(newShell);
                newShell.setText(image.name());
            }

            @NotNull
            @Override
            protected Control createDialogArea(@NotNull Composite parent) {
                Composite area = (Composite) super.createDialogArea(parent);
                Browser browser = new Browser(area, SWT.NONE);
                browser.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
                browser.setText("<html><body style='margin:0;background:#222;display:flex;align-items:center;"
                    + "justify-content:center;height:100vh'><img style='max-width:100%;max-height:100%' src='"
                    + image.toDataUrl() + "'></body></html>");
                return area;
            }

            @Override
            protected void createButtonsForButtonBar(@NotNull Composite parent) {
                createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
            }

            @NotNull
            @Override
            protected Point getInitialSize() {
                return new Point(900, 650);
            }
        }.open();
    }
}
