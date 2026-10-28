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
package org.jkiss.dbeaver.ui.services;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.ConfirmedConnectionInitScriptsStore;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnectionInit;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class UIServiceConnectionInitImpl implements UIServiceConnectionInit {
    @Override
    public boolean confirmInitScript(@NotNull DBPDataSourceContainer container, @Nullable String url, @NotNull String script) {
        if (DBWorkbench.getPlatform().getApplication().isHeadlessMode()) {
            return false;
        }
        ConfirmedConnectionInitScriptsStore approvals = new ConfirmedConnectionInitScriptsStore(
            DBWorkbench.getPlatform().getGlobalConfigurationFile(ConfirmedConnectionInitScriptsStore.FILE_NAME)
        );
        if (approvals.contains(container, url, script)) {
            return true;
        }
        boolean approved = Boolean.TRUE.equals(UIUtils.syncExec(new RunnableWithResult<Boolean>() {
            @NotNull
            @Override
            public Boolean runWithResult() {
                Shell shell = UIUtils.getActiveWorkbenchShell();
                return shell != null && !shell.isDisposed()
                    && new InitScriptDialog(shell, container.getName(), script).open() == Window.OK;
            }
        }));
        if (approved) {
            approvals.add(container, url, script);
        }
        return approved;
    }

    private static class InitScriptDialog extends BaseDialog {
        private final String connectionName;
        private final String script;

        private InitScriptDialog(@NotNull Shell shell, @NotNull String connectionName, @NotNull String script) {
            super(shell, CoreMessages.connection_init_confirmation_title, DBIcon.STATUS_WARNING);
            this.connectionName = connectionName;
            this.script = script;
        }

        @NotNull
        @Override
        protected Composite createDialogArea(@NotNull Composite parent) {
            Composite area = super.createDialogArea(parent);
            Composite content = UIUtils.createComposite(area, 2);
            content.setLayoutData(new GridData(GridData.FILL_BOTH));
            Label warning = new Label(content, SWT.WRAP);
            warning.setText(CoreMessages.connection_init_confirmation_message);
            warning.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .hint(convertWidthInCharsToPixels(80), SWT.DEFAULT).create());
            UIUtils.createLabelText(content, CoreMessages.connection_init_confirmation_connection, connectionName, SWT.READ_ONLY);
            UIUtils.createLabelText(
                content,
                CoreMessages.connection_init_confirmation_script,
                script,
                SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL,
                GridDataFactory.fillDefaults().grab(true, true)
                    .hint(convertWidthInCharsToPixels(80), convertHeightInCharsToPixels(12)).create()
            );
            return area;
        }

        @Override
        protected void createButtonsForButtonBar(@NotNull Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, CoreMessages.connection_init_confirmation_allow, false);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
        }
    }
}
