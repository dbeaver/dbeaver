/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditorsRegistry
 */
public class EntityEditorsRegistry {

    private static final String TAG_EDITOR = "editor"; //NON-NLS-1
    private static final String TAG_CONFIGURATOR = "configurator"; //NON-NLS-1
    private static final String TAG_MANAGER = "manager"; //NON-NLS-1

    private static EntityEditorsRegistry instance = null;

    public synchronized static EntityEditorsRegistry getInstance() {
        if (instance == null) {
            instance = new EntityEditorsRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private EntityEditorDescriptor defaultEditor;
    private List<EntityEditorDescriptor> entityEditors = new ArrayList<EntityEditorDescriptor>();
    private List<EntityConfiguratorDescriptor> entityConfigurators = new ArrayList<>();
    private Map<String, List<EntityEditorDescriptor>> positionsMap = new HashMap<String, List<EntityEditorDescriptor>>();

    public EntityEditorsRegistry(IExtensionRegistry registry) {
        // Create default editor
        defaultEditor = new EntityEditorDescriptor();
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EntityEditorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (TAG_EDITOR.equals(ext.getName())) {
                EntityEditorDescriptor descriptor = new EntityEditorDescriptor(ext);
                entityEditors.add(descriptor);
                List<EntityEditorDescriptor> list = positionsMap.get(descriptor.getPosition());
                if (list == null) {
                    list = new ArrayList<>();
                    positionsMap.put(descriptor.getPosition(), list);
                }
                list.add(descriptor);
            } else if (TAG_CONFIGURATOR.equals(ext.getName())) {
                EntityConfiguratorDescriptor descriptor = new EntityConfiguratorDescriptor(ext);
                entityConfigurators.add(descriptor);
            }
        }
    }

    public void dispose() {
        entityConfigurators.clear();
        entityEditors.clear();
    }

    public EntityEditorDescriptor getMainEntityEditor(DBPObject object, IEntityEditorContext context) {
        for (EntityEditorDescriptor descriptor : entityEditors) {
            if (descriptor.appliesTo(object, context) && descriptor.isMain() && descriptor.getType() == EntityEditorDescriptor.Type.editor) {
                return descriptor;
            }
        }
        return defaultEditor;
    }

    public List<EntityEditorDescriptor> getEntityEditors() {
        return entityEditors;
    }

    public List<EntityEditorDescriptor> getEntityEditors(DBPObject object, IEntityEditorContext context, String position) {
        List<EntityEditorDescriptor> editors = new ArrayList<>();
        final List<EntityEditorDescriptor> positionList =
            CommonUtils.isEmpty(position) ? entityEditors : positionsMap.get(position);
        if (positionList != null) {
            for (EntityEditorDescriptor descriptor : positionList) {
                if (descriptor.appliesTo(object, context)) {
                    editors.add(descriptor);
                }
            }
        }
        return editors;
    }

    public EntityConfiguratorDescriptor getEntityConfigurator(DBPObject object) {
        for (EntityConfiguratorDescriptor descriptor : entityConfigurators) {
            if (descriptor.appliesTo(object)) {
                return descriptor;
            }
        }
        return null;
    }

    public EntityEditorDescriptor getEntityEditor(IEditorPart editor) {
        for (EntityEditorDescriptor ed : entityEditors) {
            if (ed.getEditorType().getImplName().equals(editor.getClass().getName())) {
                return ed;
            }
        }
        return null;
    }
}
