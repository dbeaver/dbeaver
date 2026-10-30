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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionConfirmation;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoading;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoadingException;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionPermissionStore;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteDataSource;
import org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IErrorAction;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetErrorAction;

public class SQLiteExtensionsErrorAction implements IResultSetErrorAction, IAdapterFactory {
    @Override
    public boolean isVisible(@NotNull IResultSetContainer container, @NotNull IStatus status) {
        DBCExecutionContext context = container.getExecutionContext();
        if (context == null || !(context.getDataSource() instanceof SQLiteDataSource)) {
            return false;
        }
        return SQLiteExtensionLoading.isLoadingDenied(status.getException());
    }

    @Override
    public void perform(@NotNull IResultSetContainer container, @NotNull IStatus status) {
        DBCExecutionContext context = container.getExecutionContext();
        if (context != null) {
            request(context.getDataSource().getContainer());
        }
    }

    @Nullable
    @Override
    public <TYPE> TYPE getAdapter(@NotNull Object adaptableObject, @NotNull Class<TYPE> adapterType) {
        if (adapterType == IErrorAction.class && adaptableObject instanceof SQLiteExtensionLoadingException error
            && error.getExecutionContext() != null) {
            DBPDataSourceContainer container = error.getExecutionContext().getDataSource().getContainer();
            if (!container.isTemporary()) {
                return adapterType.cast(new ExtensionApprovalAction(container));
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Class<?>[] getAdapterList() {
        return new Class<?>[] {IErrorAction.class};
    }

    private record ExtensionApprovalAction(@NotNull DBPDataSourceContainer container) implements IErrorAction {
        @NotNull
        @Override
        public String getLabel() {
            return SQLiteUIMessages.extension_settings_open;
        }

        @NotNull
        @Override
        public String getDescription() {
            return SQLiteUIMessages.extension_settings_loading_disabled;
        }

        @Override
        public void perform() {
            request(container);
        }
    }

    private static void request(@NotNull DBPDataSourceContainer container) {
        UIUtils.asyncExec(() -> {
            var application = DBWorkbench.getPlatform().getApplication();
            if (application.isHeadlessMode() || application.isMultiuser()) {
                return;
            }
            SQLiteExtensionConfirmation confirmation = DBWorkbench.getService(SQLiteExtensionConfirmation.class);
            if (confirmation == null) {
                return;
            }
            try {
                if (SQLiteExtensionLoading.requestEnable(container, SQLiteExtensionPermissionStore.getInstance(),
                    settings -> confirmation.confirmExtensionLoading(settings, false))) {
                    if (container.isConnected()) {
                        DataSourceHandler.reconnectDataSource(null, container);
                    } else {
                        DataSourceHandler.connectToDataSource(null, container, null);
                    }
                }
            } catch (DBException exception) {
                DBWorkbench.getPlatformUI().showError(SQLiteUIMessages.extension_confirmation_title,
                    SQLiteUIMessages.extension_settings_error, exception);
            }
        });
    }
}
