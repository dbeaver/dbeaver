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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.dam.remote.api.exception.DDRemoteDamException;
import com.dbeaver.datadam.dam.remote.api.model.DDRemoteDamSettings;
import com.dbeaver.datadam.dam.remote.api.service.desktop.DDRemoteDamSettingsService;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.util.*;
import java.util.function.Function;

public class DDDesktopRpcRegistrationRegistry {
    private static final Log log = Log.getLog(DDDesktopRpcRegistrationRegistry.class);
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.model.datadam.desktopRpcService";
    private static DDDesktopRpcRegistrationRegistry instance;
    private final List<DDDesktopRpcRegistration> registrations = new ArrayList<>();

    private DDDesktopRpcRegistrationRegistry() {
        Map<String, DDDesktopRpcRegistrationDescriptor> descriptors = new LinkedHashMap<>();
        for (var extension : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"service".equals(extension.getName())) {
                continue;
            }
            DDDesktopRpcRegistrationDescriptor descriptor = new DDDesktopRpcRegistrationDescriptor(extension);
            if (descriptors.putIfAbsent(descriptor.getId(), descriptor) != null) {
                throw new IllegalStateException("Duplicate desktop RPC registration: " + descriptor.getId());
            }
        }
        List<DDDesktopRpcRegistrationDescriptor> creationOrder = orderBy(
            descriptors,
            DDDesktopRpcRegistrationDescriptor::getDependsOn
        );
        List<DDDesktopRpcRegistrationDescriptor> registrationOrder = orderBy(
            descriptors,
            DDDesktopRpcRegistrationDescriptor::getRegisterAfter
        );
        Map<String, DDDesktopRpcRegistration> created = new HashMap<>();
        for (DDDesktopRpcRegistrationDescriptor descriptor : creationOrder) {
            List<DDDesktopRpcRegistration> dependencies = descriptor.getDependsOn().stream().map(created::get).toList();
            try {
                created.put(descriptor.getId(), descriptor.create(dependencies));
            } catch (DBException e) {
                throw new IllegalStateException("Cannot create desktop RPC registration " + descriptor.getId(), e);
            }
        }
        for (DDDesktopRpcRegistrationDescriptor descriptor : registrationOrder) {
            registrations.add(created.get(descriptor.getId()));
        }
        log.debug("Found " + registrations.size() + " DataDam desktop RPC registrations");
    }

    @NotNull
    private List<DDDesktopRpcRegistrationDescriptor> orderBy(
        @NotNull Map<String, DDDesktopRpcRegistrationDescriptor> descriptors,
        @NotNull Function<DDDesktopRpcRegistrationDescriptor, List<String>> dependencies
    ) {
        List<DDDesktopRpcRegistrationDescriptor> order = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (DDDesktopRpcRegistrationDescriptor descriptor : descriptors.values()) {
            addToOrder(descriptor, descriptors, dependencies, visiting, visited, order);
        }
        return order;
    }

    private void addToOrder(
        @NotNull DDDesktopRpcRegistrationDescriptor descriptor,
        @NotNull Map<String, DDDesktopRpcRegistrationDescriptor> descriptors,
        @NotNull Function<DDDesktopRpcRegistrationDescriptor, List<String>> dependencies,
        @NotNull Set<String> visiting,
        @NotNull Set<String> visited,
        @NotNull List<DDDesktopRpcRegistrationDescriptor> order
    ) {
        if (visited.contains(descriptor.getId())) {
            return;
        }
        if (!visiting.add(descriptor.getId())) {
            throw new IllegalStateException("Cyclic desktop RPC registration dependency: " + descriptor.getId());
        }
        for (String id : dependencies.apply(descriptor)) {
            DDDesktopRpcRegistrationDescriptor dependency = descriptors.get(id);
            if (dependency == null) {
                throw new IllegalStateException("Missing desktop RPC registration dependency: " + id);
            }
            addToOrder(dependency, descriptors, dependencies, visiting, visited, order);
        }
        visiting.remove(descriptor.getId());
        visited.add(descriptor.getId());
        order.add(descriptor);
    }

    @NotNull
    public static DDDesktopRpcRegistrationRegistry getInstance() {
        if (instance == null) {
            instance = new DDDesktopRpcRegistrationRegistry();
        }
        return instance;
    }

    public void registerEndpoint(@NotNull MessagingRpcEndpoint endpoint, @NotNull DDRemoteDamSettings settings) throws DBException {
        if (endpoint.isClosed()) {
            return;
        }
        Object settingsLock = new Object();
        synchronized (settingsLock) {
            // a callback arriving during initialization must be applied after the handshake snapshot
            endpoint.register(DDRemoteDamSettingsService.SERVICE_ID, DDRemoteDamSettingsService.class, updated -> {
                synchronized (settingsLock) {
                    try {
                        registerAll(endpoint, updated);
                    } catch (DBException e) {
                        throw new DDRemoteDamException("Cannot apply DAM settings", e);
                    }
                }
            });
            registerAll(endpoint, settings);
        }
    }

    public void registerAll(@NotNull MessagingRpcEndpoint endpoint, @NotNull DDRemoteDamSettings settings) throws DBException {
        for (DDDesktopRpcRegistration registration : registrations) {
            registration.register(endpoint, settings);
        }
    }
}
