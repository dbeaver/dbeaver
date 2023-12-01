/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class DebugIconsHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell shell = HandlerUtil.getActiveShell(event);
        final IconDialog dialog = new IconDialog(shell);
        dialog.open();

        return null;
    }

    private static class IconDialog extends BaseDialog {
        public IconDialog(@NotNull Shell shell) {
            super(shell, "Icons", null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = new Composite(super.createDialogArea(parent), SWT.NONE);
            composite.setLayout(new GridLayout());

            new IconContainer(composite, new DBIcon("logo", "platform:/plugin/org.jkiss.dbeaver.core/icons/logo.svg"));
            new IconContainer(composite, new DBIcon("logo_small", "platform:/plugin/org.jkiss.dbeaver.core/icons/logo_small.svg"));

            return composite;
        }
    }

    private static class IconContainer extends Composite {
        public IconContainer(@NotNull Composite parent, @NotNull DBPImage image) {
            super(parent, SWT.BORDER);

            setLayout(new GridLayout());
            setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            final Label icon = new Label(this, SWT.NONE);
            icon.setImage(DBeaverIcons.getImage(image));
            icon.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, true, false));

            final Label location = new Label(this, SWT.NONE);
            location.setText(image.getLocation());
            location.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, true, false));
        }
    }
}
