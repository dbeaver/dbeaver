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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileUsageProvider;

public final class NetworkProfileUsageAdapterFactory implements IAdapterFactory {
    private static final Class<?>[] ADAPTERS = {DBWNetworkProfileUsageProvider.class};

    @Override
    @Nullable
    public <T> T getAdapter(@NotNull Object adaptableObject, @NotNull Class<T> adapterType) {
        if (!(adaptableObject instanceof DBPWorkspace workspace) || adapterType != DBWNetworkProfileUsageProvider.class ||
            workspace.getPlatform().getApplication().isDistributed()) {
            return null;
        }
        return adapterType.cast(new LocalNetworkProfileUsageProvider(workspace));
    }

    @Override
    @NotNull
    public Class<?>[] getAdapterList() {
        return ADAPTERS;
    }
}
