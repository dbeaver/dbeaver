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
package org.jkiss.dbeaver.ui.services;

import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.ui.UIServiceAuth;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.CodeAuthDialog;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class UIServiceAuthImpl implements UIServiceAuth {
    @Override
    public void showCodePopup(
        @NotNull URI browserUrl,
        @NotNull String userCode,
        @NotNull CompletableFuture<Void> future
    ) {
        UIUtils.asyncExec(() -> {
            Shell shell = UIUtils.getActiveWorkbenchShell();
            if (shell == null) {
                // No shell - can't show the dialog
                return;
            }
            CodeAuthDialog dialog = new CodeAuthDialog(shell, browserUrl, userCode, future);
            future.handle((result, exception) -> {
                UIUtils.syncExec(dialog::close);
                return null;
            });
            dialog.open();
        });
    }
}
