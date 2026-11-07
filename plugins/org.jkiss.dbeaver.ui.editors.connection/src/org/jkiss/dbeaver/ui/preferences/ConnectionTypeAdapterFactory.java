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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;

/**
 * Adapts {@link DBPConnectionType} to {@link IWorkbenchAdapter},
 * so that the workbench can show its name (for example, in the property dialog title).
 */
public class ConnectionTypeAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] ADAPTER_LIST = {IWorkbenchAdapter.class};

    private static final IWorkbenchAdapter CONNECTION_TYPE_ADAPTER = new WorkbenchAdapter() {
        @Override
        public String getLabel(Object object) {
            return object instanceof DBPConnectionType connectionType ? connectionType.getName() : "";
        }
    };

    @Nullable
    @Override
    public <T> T getAdapter(@Nullable Object adaptableObject, @NotNull Class<T> adapterType) {
        if (adapterType == IWorkbenchAdapter.class && adaptableObject instanceof DBPConnectionType) {
            return adapterType.cast(CONNECTION_TYPE_ADAPTER);
        }
        return null;
    }

    @NotNull
    @Override
    public Class<?>[] getAdapterList() {
        return ADAPTER_LIST;
    }
}
