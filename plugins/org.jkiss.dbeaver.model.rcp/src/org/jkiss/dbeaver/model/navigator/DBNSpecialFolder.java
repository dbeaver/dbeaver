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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.registry.ResourceTypeDescriptor;
import org.jkiss.dbeaver.registry.ResourceTypeRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/** Resource folder whose predefined root can have a localized display name. */
public class DBNSpecialFolder extends DBNResource {

    public DBNSpecialFolder(@NotNull DBNNode parentNode, @NotNull IResource resource, @NotNull DBPResourceHandler handler) {
        super(parentNode, resource, handler);
    }

    @NotNull
    @Override
    public String getNodeId() {
        // Persisted navigator URIs must not depend on the display language or preference.
        return getResource().getName();
    }

    @NotNull
    @Override
    public String getNodeDisplayName() {
        IResource resource = getResource();
        if (resource != null
            && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.NAVIGATOR_LOCALIZE_FOLDER_NAMES)
            && NavigatorResources.isRootResource(getOwnerProject(), resource)
        ) {
            // Resolve extension metadata without instantiating the resource handler.
            String path = resource.getProjectRelativePath().toString();
            ResourceTypeDescriptor type = ResourceTypeRegistry.getInstance().getResourceTypeByRootPath(getOwnerProject(), path);
            String label = type == null ? null : type.getRootFolderLabel(path);
            if (label != null) {
                return label;
            }
        }
        return super.getNodeDisplayName();
    }
}
