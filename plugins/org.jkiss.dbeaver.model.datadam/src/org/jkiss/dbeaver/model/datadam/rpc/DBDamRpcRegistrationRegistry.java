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

public class DBDamRpcRegistrationRegistry {
    private static final Log log = Log.getLog(DBDamRpcRegistrationRegistry.class);
    private static final String EXTENSION_ID = "dbeaver.datadam.damService";
    private static DBDamRpcRegistrationRegistry instance;
    private final List<DBDamRpcRegistration> registrations = new ArrayList<>();

    private DBDamRpcRegistrationRegistry() {
        Map<String, DBDamRpcRegistrationDescriptor> descriptors = new LinkedHashMap<>();
        for (var extension : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"service".equals(extension.getName())) {
                continue;
            }
            DBDamRpcRegistrationDescriptor descriptor = new DBDamRpcRegistrationDescriptor(extension);
            if (descriptors.putIfAbsent(descriptor.getId(), descriptor) != null) {
                throw new IllegalStateException("Duplicate desktop RPC registration: " + descriptor.getId());
            }
        }
        List<DBDamRpcRegistrationDescriptor> creationOrder = orderBy(
            descriptors,
            DBDamRpcRegistrationDescriptor::getDependsOn
        );
        List<DBDamRpcRegistrationDescriptor> registrationOrder = orderBy(
            descriptors,
            DBDamRpcRegistrationDescriptor::getRegisterAfter
        );
        Map<String, DBDamRpcRegistration> created = new HashMap<>();
        for (DBDamRpcRegistrationDescriptor descriptor : creationOrder) {
            List<DBDamRpcRegistration> dependencies = descriptor.getDependsOn().stream().map(created::get).toList();
            try {
                created.put(descriptor.getId(), descriptor.create(dependencies));
            } catch (DBException e) {
                throw new IllegalStateException("Cannot create desktop RPC registration " + descriptor.getId(), e);
            }
        }
        for (DBDamRpcRegistrationDescriptor descriptor : registrationOrder) {
            registrations.add(created.get(descriptor.getId()));
        }
        log.debug("Found " + registrations.size() + " DataDam desktop RPC registrations");
    }

    @NotNull
    private List<DBDamRpcRegistrationDescriptor> orderBy(
        @NotNull Map<String, DBDamRpcRegistrationDescriptor> descriptors,
        @NotNull Function<DBDamRpcRegistrationDescriptor, List<String>> dependencies
    ) {
        List<DBDamRpcRegistrationDescriptor> order = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (DBDamRpcRegistrationDescriptor descriptor : descriptors.values()) {
            addToOrder(descriptor, descriptors, dependencies, visiting, visited, order);
        }
        return order;
    }

    private void addToOrder(
        @NotNull DBDamRpcRegistrationDescriptor descriptor,
        @NotNull Map<String, DBDamRpcRegistrationDescriptor> descriptors,
        @NotNull Function<DBDamRpcRegistrationDescriptor, List<String>> dependencies,
        @NotNull Set<String> visiting,
        @NotNull Set<String> visited,
        @NotNull List<DBDamRpcRegistrationDescriptor> order
    ) {
        if (visited.contains(descriptor.getId())) {
            return;
        }
        if (!visiting.add(descriptor.getId())) {
            throw new IllegalStateException("Cyclic desktop RPC registration dependency: " + descriptor.getId());
        }
        for (String id : dependencies.apply(descriptor)) {
            DBDamRpcRegistrationDescriptor dependency = descriptors.get(id);
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
    public static DBDamRpcRegistrationRegistry getInstance() {
        if (instance == null) {
            instance = new DBDamRpcRegistrationRegistry();
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
                    if (endpoint.isClosed()) {
                        return;
                    }
                    try {
                        registerAll(endpoint, updated);
                        DBDamSettingsStore.getInstance().updateSettings(updated);
                    } catch (DBException e) {
                        throw new DDRemoteDamException("Cannot apply Dam settings", e);
                    }
                }
            });
            registerAll(endpoint, settings);
        }
    }

    public void registerAll(@NotNull MessagingRpcEndpoint endpoint, @NotNull DDRemoteDamSettings settings) throws DBException {
        for (DBDamRpcRegistration registration : registrations) {
            registration.register(endpoint, settings);
        }
    }
}
