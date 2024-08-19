/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.configurator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.IObjectPropertyConfiguratorProvider;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.HashMap;
import java.util.Map;

public class UIPropertyConfiguratorRegistry {

    private static final Log log = Log.getLog(UIPropertyConfiguratorRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.propertyConfigurator"; //$NON-NLS-1$

    private static UIPropertyConfiguratorRegistry instance = null;

    public synchronized static UIPropertyConfiguratorRegistry getInstance() {
        if (instance == null) {
            instance = new UIPropertyConfiguratorRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, UIPropertyConfiguratorDescriptor> descriptors = new HashMap<>();
    private final Map<String, Pair<UIPropertyConfiguratorProviderDescriptor, Map<String, UIPropertyConfiguratorProviderDescriptor>>> providers = new HashMap<>();

    private UIPropertyConfiguratorRegistry(IExtensionRegistry registry) {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                switch (ext.getName()) {
                    case UIPropertyConfiguratorDescriptor.ELEMENT_NAME -> {
                        UIPropertyConfiguratorDescriptor descriptor = new UIPropertyConfiguratorDescriptor(ext);
                        if (descriptors.get(descriptor.getObjectType()) == null) {
                            descriptors.put(descriptor.getObjectType(), descriptor);
                        } else {
                            log.warn("Ambiguous configurators for " + descriptor.getObjectType() + " detected");
                        }
                    }
                    case UIPropertyConfiguratorProviderDescriptor.ELEMENT_NAME -> {
                        UIPropertyConfiguratorProviderDescriptor descriptor = new UIPropertyConfiguratorProviderDescriptor(ext);

                        var objectConfiguratorProviders = providers.computeIfAbsent(descriptor.getObjectTypeName(), k -> Pair.of(null, new HashMap<>()));
                        if (CommonUtils.isEmpty(descriptor.getParameterTypeNameOrNull())) {
                            if (objectConfiguratorProviders.getFirst() == null) {
                                objectConfiguratorProviders.setFirst(descriptor);
                            } else {
                                log.warn("Ambiguous parameter-less configurator providers for " + descriptor.getObjectTypeName() + " detected");
                            }
                        } else {
                            Map<String, UIPropertyConfiguratorProviderDescriptor> parametrizedDescriptors = objectConfiguratorProviders.getSecond();
                            if (parametrizedDescriptors.get(descriptor.getParameterTypeNameOrNull()) == null) {
                                parametrizedDescriptors.put(descriptor.getParameterTypeNameOrNull(), descriptor);
                            } else {
                                log.warn("Ambiguous configurator providers for " + descriptor.getObjectTypeName() +
                                         " with " + descriptor.getParameterTypeNameOrNull() + " parameter detected");
                            }
                        }
                    }
                }
            }
        }
    }

    public UIPropertyConfiguratorDescriptor getDescriptor(Object object) {
        return findByKeyTypeName(this.descriptors, object.getClass());
    }

    public UIPropertyConfiguratorDescriptor getDescriptor(String className) {
        return descriptors.get(className);
    }

    private static <T> T findByKeyTypeName(Map<String, T> map, Class<?> type) {
        for (Class<?> theClass = type; theClass != Object.class; theClass = theClass.getSuperclass()) {
            T value = map.get(theClass.getName());
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    public UIPropertyConfiguratorProviderDescriptor findProviderDescriptorByObjectType(@NotNull Object object, @Nullable Object parameter) {
        var objectConfiguratorProviders = findByKeyTypeName(this.providers, object.getClass());
        if (objectConfiguratorProviders != null) {
            var configuratorProvider = findByKeyTypeName(objectConfiguratorProviders.getSecond(), parameter.getClass());
            if (configuratorProvider != null) {
                return configuratorProvider;
            } else {
                return objectConfiguratorProviders.getFirst();
            }
        } else {
            return null;
        }
    }

    @Nullable
    public UIPropertyConfiguratorProviderDescriptor findProviderDescriptorByExactObjectTypeName(@NotNull String className, @Nullable Object parameter) {
        var objectConfiguratorProviders = this.providers.get(className);
        if (objectConfiguratorProviders != null) {
            var configuratorProvider = findByKeyTypeName(objectConfiguratorProviders.getSecond(), parameter.getClass());
            if (configuratorProvider != null) {
                return configuratorProvider;
            } else {
                return objectConfiguratorProviders.getFirst();
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <OBJECT, SETTINGS, PARAMETER, T extends IObjectPropertyConfigurator<OBJECT, SETTINGS>> T tryCreateConfigurator(@NotNull OBJECT object, PARAMETER parameter) throws DBException {
        UIPropertyConfiguratorProviderDescriptor providerDescriptor = this.findProviderDescriptorByObjectType(object, parameter);
        if (providerDescriptor != null) {
            IObjectPropertyConfiguratorProvider<OBJECT, SETTINGS, PARAMETER, T> provider =  providerDescriptor.createConfiguratorProvider();
            return provider.createConfigurator(object, parameter);
        } else {
            UIPropertyConfiguratorDescriptor configuratorDescriptor = this.getDescriptor(object);
            if (configuratorDescriptor != null) {
                return configuratorDescriptor.createConfigurator();
            } else {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <OBJECT, SETTINGS, PARAMETER, T extends IObjectPropertyConfigurator<OBJECT, SETTINGS>> IObjectPropertyConfiguratorProvider<OBJECT, SETTINGS, PARAMETER, T> findConfiguratorProviderByObjectType(@NotNull Object object, PARAMETER parameter) throws DBException {
        UIPropertyConfiguratorProviderDescriptor providerDescriptor = this.findProviderDescriptorByObjectType(object, parameter);
        if (providerDescriptor != null) {
            return providerDescriptor.createConfiguratorProvider();
        } else {
            UIPropertyConfiguratorDescriptor configuratorDescriptor = this.getDescriptor(object);
            if (configuratorDescriptor != null) {
                return new IObjectPropertyConfiguratorProvider<OBJECT, SETTINGS, PARAMETER, T>() {
                    @NotNull
                    @Override
                    public T createConfigurator(OBJECT object, PARAMETER parameter) throws DBException {
                        return configuratorDescriptor.createConfigurator();
                    }
                };
            } else {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <OBJECT, SETTINGS, PARAMETER, T extends IObjectPropertyConfigurator<OBJECT, SETTINGS>> IObjectPropertyConfiguratorProvider<OBJECT, SETTINGS, PARAMETER, T> findConfiguratorProviderByExactObjectTypeName(@NotNull String className, PARAMETER parameter) throws DBException {
        UIPropertyConfiguratorProviderDescriptor providerDescriptor = this.findProviderDescriptorByExactObjectTypeName(className, parameter);
        if (providerDescriptor != null) {
            return providerDescriptor.createConfiguratorProvider();
        } else {
            UIPropertyConfiguratorDescriptor configuratorDescriptor = this.getDescriptor(className);
            if (configuratorDescriptor != null) {
                return new IObjectPropertyConfiguratorProvider<OBJECT, SETTINGS, PARAMETER, T>() {
                    @NotNull
                    @Override
                    public T createConfigurator(OBJECT object, PARAMETER parameter) throws DBException {
                        return configuratorDescriptor.createConfigurator();
                    }
                };
            } else {
                return null;
            }
        }
    }
}
