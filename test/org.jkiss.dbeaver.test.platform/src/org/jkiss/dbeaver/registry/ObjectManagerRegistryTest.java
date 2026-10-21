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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class ObjectManagerRegistryTest {

    @Test
    public void testSameNamedClassesFromDifferentBundlesUseOwnManagers() throws Exception {
        String className = SampleObject.class.getName();
        Class<?> isolatedClass = new ClassLoader(SampleObject.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!className.equals(name)) {
                    return super.loadClass(name, resolve);
                }
                try (var stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (stream == null) {
                        throw new ClassNotFoundException(name);
                    }
                    byte[] bytes = stream.readAllBytes();
                    Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }.loadClass(className);

        DBEObjectManager<?> currentManager = Mockito.mock(DBEObjectManager.class);
        DBEObjectManager<?> isolatedManager = Mockito.mock(DBEObjectManager.class);
        IExtensionRegistry extensionRegistry = Mockito.mock(IExtensionRegistry.class);
        Mockito.when(extensionRegistry.getConfigurationElementsFor(ObjectManagerDescriptor.EXTENSION_ID))
            .thenReturn(new IConfigurationElement[0]);
        ObjectManagerRegistry registry = new ObjectManagerRegistry(extensionRegistry);
        Field managersField = ObjectManagerRegistry.class.getDeclaredField("entityManagers");
        managersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ObjectManagerDescriptor> managers = (List<ObjectManagerDescriptor>) managersField.get(registry);
        managers.add(descriptor(SampleObject.class, currentManager));
        managers.add(descriptor(isolatedClass, isolatedManager));

        Assertions.assertSame(isolatedManager, registry.getObjectManager(isolatedClass));
        Assertions.assertSame(currentManager, registry.getObjectManager(SampleObject.class));
        Assertions.assertSame(isolatedManager, registry.getObjectManager(isolatedClass));
    }

    private static ObjectManagerDescriptor descriptor(Class<?> objectClass, DBEObjectManager<?> manager) {
        ObjectManagerDescriptor descriptor = Mockito.mock(ObjectManagerDescriptor.class);
        AbstractDescriptor.ObjectType type = Mockito.mock(AbstractDescriptor.ObjectType.class);
        Mockito.when(type.getImplName()).thenReturn(objectClass.getName());
        Mockito.doReturn(objectClass).when(type).getObjectClass();
        Mockito.when(descriptor.getObjectType()).thenReturn(type);
        Mockito.when(descriptor.getManager()).thenReturn(manager);
        return descriptor;
    }

    public static class SampleObject {
    }
}
