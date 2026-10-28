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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.ConfirmedConnectionInitScriptsStore;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnectionInit;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

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
                    && new ConfirmScriptDialog(
                        shell,
                        CoreMessages.connection_init_confirmation_title,
                        CoreMessages.connection_init_confirmation_message,
                        Map.of(CoreMessages.connection_init_confirmation_connection, container.getName()),
                        script,
                        CoreMessages.connection_init_confirmation_script,
                        CoreMessages.connection_init_confirmation_allow
                    ).open() == Window.OK;
            }
        }));
        if (approved) {
            approvals.add(container, url, script);
        }
        return approved;
    }
}
