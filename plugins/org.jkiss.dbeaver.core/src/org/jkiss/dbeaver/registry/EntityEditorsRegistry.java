/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public class EntityEditorsRegistry {

    private static final String CFG_EDITOR = "editor";
    private static final String CFG_MANAGER = "manager";

    private EntityEditorDescriptor defaultEditor;
    private List<EntityEditorDescriptor> entityEditors = new ArrayList<EntityEditorDescriptor>();
    private List<EntityManagerDescriptor> entityManagers = new ArrayList<EntityManagerDescriptor>();

    public EntityEditorsRegistry(IExtensionRegistry registry)
    {
        // Create default editor
        defaultEditor = new EntityEditorDescriptor();
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EntityEditorDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (CFG_EDITOR.equals(ext.getName())) {
                EntityEditorDescriptor descriptor = new EntityEditorDescriptor(ext);
                entityEditors.add(descriptor);
            } else if (CFG_MANAGER.equals(ext.getName())) {
                EntityManagerDescriptor descriptor = new EntityManagerDescriptor(ext);
                entityManagers.add(descriptor);
            }
        }

    }

    public void dispose()
    {
        entityEditors.clear();

        for (EntityManagerDescriptor descriptor : entityManagers) {
            descriptor.dispose();
        }
        entityManagers.clear();
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
        return defaultEditor;
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

    private EntityManagerDescriptor getEntityManager(Class objectType)
    {
        for (EntityManagerDescriptor descriptor : entityManagers) {
            if (descriptor.appliesToType(objectType)) {
                return descriptor;
            }
        }
        return null;
    }

    public DBEObjectManager<?> getObjectManager(Class<?> aClass)
    {
        EntityManagerDescriptor entityManager = getEntityManager(aClass);
        return entityManager == null ? null : entityManager.getManager();
    }

    public <T> T getObjectManager(Class<?> aClass, Class<T> managerType)
    {
        final DBEObjectManager<?> objectManager = getObjectManager(aClass);
        if (objectManager != null && managerType.isAssignableFrom(objectManager.getClass())) {
            return managerType.cast(objectManager);
        } else {
            return null;
        }
    }

}
