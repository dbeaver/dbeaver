/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.core.DBeaverCore;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public class EntityEditorsRegistry {

    private DBeaverCore core;
    private List<EntityEditorDescriptor> entityEditors = new ArrayList<EntityEditorDescriptor>();

    public EntityEditorsRegistry(DBeaverCore core, IExtensionRegistry registry)
    {
        this.core = core;

        // Load datasource providers from external plugins

        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EntityEditorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            EntityEditorDescriptor provider = new EntityEditorDescriptor(this, ext);
            entityEditors.add(provider);
        }

    }

    public DBeaverCore getCore()
    {
        return core;
    }

    public List<EntityEditorDescriptor> getEntityEditors()
    {
        return entityEditors;
    }

    public EntityEditorDescriptor getMainEntityEditor(Class objectType)
    {
        for (EntityEditorDescriptor descriptor : entityEditors) {
            if (descriptor.appliesToType(objectType) && descriptor.isMain()) {
                return descriptor;
            }
        }
        return null;
    }

    public List<EntityEditorDescriptor> getEntityEditors(Class objectType, String position)
    {
        List<EntityEditorDescriptor> editors = new ArrayList<EntityEditorDescriptor>();
        for (EntityEditorDescriptor descriptor : entityEditors) {
            if (descriptor.appliesToType(objectType) && (position == null || position.equalsIgnoreCase(descriptor.getPosition()))) {
                editors.add(descriptor);
            }
        }
        return editors;
    }

}
