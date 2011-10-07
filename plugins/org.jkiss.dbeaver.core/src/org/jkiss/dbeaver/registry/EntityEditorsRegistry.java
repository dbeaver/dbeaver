/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EntityEditorsRegistry
 */
public class EntityEditorsRegistry {

    private static final String TAG_EDITOR = "editor"; //NON-NLS-1
    private static final String TAG_MANAGER = "manager"; //NON-NLS-1

    private EntityEditorDescriptor defaultEditor;
    private List<EntityEditorDescriptor> entityEditors = new ArrayList<EntityEditorDescriptor>();
    private Map<String, List<EntityEditorDescriptor>> positionsMap = new HashMap<String, List<EntityEditorDescriptor>>();
    private List<EntityManagerDescriptor> entityManagers = new ArrayList<EntityManagerDescriptor>();

    public EntityEditorsRegistry(IExtensionRegistry registry)
    {
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
                    list = new ArrayList<EntityEditorDescriptor>();
                    positionsMap.put(descriptor.getPosition(), list);
                }
                list.add(descriptor);
            } else if (TAG_MANAGER.equals(ext.getName())) {
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
        final List<EntityEditorDescriptor> positionList = positionsMap.get(position);
        if (positionList != null) {
            for (EntityEditorDescriptor descriptor : positionList) {
                if (descriptor.appliesToType(objectType)) {
                    editors.add(descriptor);
                }
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

    public <T> T getObjectManager(Class<?> objectClass, Class<T> managerType)
    {
        final DBEObjectManager<?> objectManager = getObjectManager(objectClass);
        if (objectManager != null && managerType.isAssignableFrom(objectManager.getClass())) {
            return managerType.cast(objectManager);
        } else {
            return null;
        }
    }

}
