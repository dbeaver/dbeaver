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
package org.jkiss.dbeaver.ext.sqlite.ui;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionConfirmation;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionSettings;
import org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ExecutionConfirmationDialog;

import java.util.LinkedHashMap;
import java.util.Map;

public class SQLiteExtensionConfirmationImpl implements SQLiteExtensionConfirmation {
    @Override
    public boolean confirmExtensionLoading(@NotNull SQLiteExtensionSettings settings, boolean connecting) {
        return Boolean.TRUE.equals(UIUtils.syncExec(new RunnableWithResult<Boolean>() {
            @NotNull
            @Override
            public Boolean runWithResult() {
                Shell shell = UIUtils.getActiveWorkbenchShell();
                if (shell == null || shell.isDisposed()) {
                    return false;
                }
                Map<String, String> execution = new LinkedHashMap<>();
                if (!settings.libraries().isEmpty()) {
                    execution.put(SQLiteUIMessages.extension_confirmation_libraries, String.join("\n", settings.libraries()));
                }
                if (!settings.initQueries().isEmpty()) {
                    execution.put(SQLiteUIMessages.extension_confirmation_queries, String.join(";\n", settings.initQueries()));
                }
                ExecutionConfirmationDialog dialog = new ExecutionConfirmationDialog(
                    shell,
                    SQLiteUIMessages.extension_confirmation_title,
                    SQLiteUIMessages.extension_confirmation_message,
                    connecting ? SQLiteUIMessages.extension_confirmation_connect : SQLiteUIMessages.extension_confirmation_allow,
                    Map.of(SQLiteUIMessages.extension_confirmation_connection, settings.connectionName()),
                    execution
                );
                dialog.setAdditionalDetails(Map.of(SQLiteUIMessages.extension_confirmation_url, settings.url()));
                return dialog.open() == Window.OK;
            }
        }));
    }
}
