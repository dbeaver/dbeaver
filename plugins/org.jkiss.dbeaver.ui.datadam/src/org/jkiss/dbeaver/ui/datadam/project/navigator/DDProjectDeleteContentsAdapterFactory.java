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
package org.jkiss.dbeaver.ui.datadam.project.navigator;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.BindingStatus;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.navigator.actions.ProjectDeleteContentsPolicy;

public class DDProjectDeleteContentsAdapterFactory implements IAdapterFactory, ProjectDeleteContentsPolicy {
    @Nullable
    @Override
    public <T> T getAdapter(@NotNull Object adaptableObject, @NotNull Class<T> adapterType) {
        return adapterType == ProjectDeleteContentsPolicy.class && adaptableObject instanceof DBNProject
            ? adapterType.cast(this) : null;
    }

    @NotNull
    @Override
    public Class<?>[] getAdapterList() {
        return new Class<?>[]{ProjectDeleteContentsPolicy.class};
    }

    @Override
    public boolean requiresDeleteContents(@NotNull DBPProject project) throws DBException {
        if (!DDProductConfig.isIntegrationEnabled()) {
            return false;
        }
        return DDProjectUIManager.getInstance()
            .getProjectManager()
            .getBindingStatus(project) == BindingStatus.CURRENT_ACCOUNT;
    }
}
