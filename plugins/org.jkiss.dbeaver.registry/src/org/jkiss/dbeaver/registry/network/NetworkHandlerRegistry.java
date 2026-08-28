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
package org.jkiss.dbeaver.registry.network;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerRegistry;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.runtime.DBSecurityException;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;
import java.util.function.Predicate;

public class NetworkHandlerRegistry implements DBWHandlerRegistry {
    private static NetworkHandlerRegistry instance = null;

    public synchronized static NetworkHandlerRegistry getInstance() {
        if (instance == null) {
            instance = new NetworkHandlerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<NetworkHandlerDescriptor> descriptors = new ArrayList<>();

    private NetworkHandlerRegistry(IExtensionRegistry registry) {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(NetworkHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                NetworkHandlerDescriptor formatterDescriptor = new NetworkHandlerDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }

            // Remove replaced handlers
            for (NetworkHandlerDescriptor hd1 : descriptors) {
                for (NetworkHandlerDescriptor hd2 : descriptors) {
                    if (hd2.replaces(hd1)) {
                        hd1.setReplacedBy(hd2);
                        break;
                    }
                }
            }

            descriptors.sort(Comparator.comparingInt(NetworkHandlerDescriptor::getOrder));
        }
    }

    @NotNull
    public List<NetworkHandlerDescriptor> getDescriptors() {
        List<NetworkHandlerDescriptor> descList = new ArrayList<>(descriptors);
        descList.removeIf(nhd -> !isAvailable(nhd));
        return descList;
    }

    @Nullable
    public NetworkHandlerDescriptor getDescriptor(@NotNull String id) {
        return getDescriptor(id, DBWorkbench.getPlatform().getWorkspace());
    }

    @Nullable
    public NetworkHandlerDescriptor getDescriptor(
        @NotNull String id,
        @NotNull DBAPermissionRealm permissionRealm
    ) {
        NetworkHandlerDescriptor descriptor = getRawDescriptor(id);
        return descriptor != null && isAvailable(descriptor, permissionRealm) ? descriptor : null;
    }

    @Nullable
    public NetworkHandlerDescriptor getRawDescriptor(@NotNull String id) {
        for (NetworkHandlerDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                if (descriptor.getReplacedBy() != null) {
                    descriptor = descriptor.getReplacedBy();
                }
                return descriptor;
            }
        }
        return null;
    }

    @NotNull
    public List<NetworkHandlerDescriptor> getDescriptors(@NotNull DBPDataSourceContainer dataSource) {
        return getDescriptors(dataSource.getDriver(), dataSource.getProject());
    }

    @NotNull
    public List<NetworkHandlerDescriptor> getDescriptors(@NotNull DBPDriver driver) {
        return getDescriptors(driver, DBWorkbench.getPlatform().getWorkspace());
    }

    @NotNull
    private List<NetworkHandlerDescriptor> getDescriptors(
        @NotNull DBPDriver driver,
        @NotNull DBAPermissionRealm permissionRealm
    ) {
        List<NetworkHandlerDescriptor> result = new ArrayList<>();
        for (NetworkHandlerDescriptor d : descriptors) {
            if (d.getReplacedBy() != null ||
                !permissionRealm.supportsRealmFeature(DBAPermissionRealm.FEATURE_SSH_TUNNELING)
                && d.getType() == DBWHandlerType.TUNNEL) {
                continue;
            }
            if ((!d.hasObjectTypes() || d.matches(driver)) && hasRequiredPermissions(d, permissionRealm)) {
                result.add(d);
            }
        }
        return result;
    }

    private boolean isAvailable(@NotNull NetworkHandlerDescriptor descriptor) {
        return isAvailable(descriptor, DBWorkbench.getPlatform().getWorkspace());
    }

    private boolean isAvailable(
        @NotNull NetworkHandlerDescriptor descriptor,
        @NotNull DBAPermissionRealm permissionRealm
    ) {
        return descriptor.getReplacedBy() == null && hasRequiredPermissions(descriptor, permissionRealm);
    }

    private boolean hasRequiredPermissions(
        @NotNull NetworkHandlerDescriptor descriptor,
        @NotNull DBAPermissionRealm permissionRealm
    ) {
        return descriptor.getRequiredPermissions().stream()
            .allMatch(permissionRealm::hasRealmPermission);
    }

    public void validateHandlerConfigurationUpdate(
        @Nullable DBPConnectionConfiguration storedConfiguration,
        @NotNull DBPConnectionConfiguration updatedConfiguration,
        @NotNull Predicate<String> hasPermission
    ) throws DBSecurityException {
        validateHandlerConfigurationUpdate(
            storedConfiguration == null ? List.of() : storedConfiguration.getHandlers(),
            updatedConfiguration.getHandlers(),
            hasPermission
        );
    }

    public void validateHandlerConfigurationUpdate(
        @NotNull Collection<DBWHandlerConfiguration> storedConfigurations,
        @NotNull Collection<DBWHandlerConfiguration> updatedConfigurations,
        @NotNull Predicate<String> hasPermission
    ) throws DBSecurityException {
        Set<String> handlerIds = new LinkedHashSet<>();
        storedConfigurations.forEach(configuration -> handlerIds.add(configuration.getId()));
        updatedConfigurations.forEach(configuration -> handlerIds.add(configuration.getId()));

        for (String handlerId : handlerIds) {
            DBWHandlerConfiguration stored = findConfiguration(storedConfigurations, handlerId);
            DBWHandlerConfiguration updated = findConfiguration(updatedConfigurations, handlerId);
            NetworkHandlerDescriptor descriptor = getRawDescriptor(handlerId);
            if (descriptor == null || descriptor.getRequiredPermissions().stream().allMatch(hasPermission)) {
                continue;
            }
            if (stored == null || updated == null || !stored.equalSettings(updated)) {
                throw new DBSecurityException("No permissions to configure network handler '" + handlerId + "'");
            }
            // Restricted credentials may be omitted from a shared configuration sent by the client.
            // Keep the authoritative server-side values instead of treating omitted secrets as an update.
            updated.setUserName(stored.getUserName());
            updated.setPassword(stored.getPassword());
            updated.setSavePassword(stored.isSavePassword());
            updated.setSecureProperties(stored.getSecureProperties());
        }
    }

    @Nullable
    private static DBWHandlerConfiguration findConfiguration(
        @NotNull Collection<DBWHandlerConfiguration> configurations,
        @NotNull String handlerId
    ) {
        return configurations.stream()
            .filter(configuration -> configuration.getId().equals(handlerId))
            .findFirst()
            .orElse(null);
    }

}
