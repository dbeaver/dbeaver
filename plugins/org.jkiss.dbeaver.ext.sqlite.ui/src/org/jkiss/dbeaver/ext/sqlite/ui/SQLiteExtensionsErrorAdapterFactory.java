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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.sqlite.SQLiteExtensionLoadingException;
import org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.IErrorAction;

public class SQLiteExtensionsErrorAdapterFactory implements IAdapterFactory {
    @Nullable
    @Override
    public <TYPE> TYPE getAdapter(@NotNull Object adaptableObject, @NotNull Class<TYPE> adapterType) {
        if (adapterType == IErrorAction.class && adaptableObject instanceof SQLiteExtensionLoadingException error
            && error.getExecutionContext() != null) {
            DBPDataSourceContainer container = error.getExecutionContext().getDataSource().getContainer();
            if (!container.isTemporary()) {
                return adapterType.cast(new ExtensionSettingsAction(container));
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Class<?>[] getAdapterList() {
        return new Class<?>[] {IErrorAction.class};
    }

    private record ExtensionSettingsAction(@NotNull DBPDataSourceContainer container) implements IErrorAction {
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
            UIServiceConnections service = DBWorkbench.getService(UIServiceConnections.class);
            if (service != null) {
                service.openConnectionEditor(container, null);
            }
        }
    }
}
