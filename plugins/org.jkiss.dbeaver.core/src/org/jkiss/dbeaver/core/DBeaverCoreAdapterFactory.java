/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.resource.WorkspaceResourceResolver;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

//FIXME: AF: we can rework this after "org.jkiss.dbeaver.core" bundle will be split for parts
public class DBeaverCoreAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { WorkspaceResourceResolver.class, DBPPlatform.class, DBPPlatformUI.class };
    
    private final WorkspaceResourceResolver workspaceResourceResolver = databaseObject -> {
        if (databaseObject != null) {
            DBPDataSource dataSource = databaseObject.getDataSource();
            if (dataSource != null) {
                DBPDataSourceContainer container = dataSource.getContainer();
                if (container != null) {
                    return container.getRegistry().getProject();
                }
            }
        }
        // FIXME:AF: for now it looks like reasonable default
        return DBeaverCore.getInstance().getProjectManager().getActiveProject();
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof DBWorkbench) {
            if (adapterType == DBPPlatform.class) {
                return adapterType.cast(DBeaverCore.getInstance());
            } else if (adapterType == DBPPlatformUI.class) {
                return adapterType.cast(DBeaverUI.getInstance());
            }
        } else {
            if (adapterType == WorkspaceResourceResolver.class) {
                return adapterType.cast(workspaceResourceResolver);
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
