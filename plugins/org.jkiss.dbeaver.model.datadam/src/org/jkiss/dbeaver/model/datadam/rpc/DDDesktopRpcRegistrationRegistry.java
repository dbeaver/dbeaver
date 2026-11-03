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

import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

public class DDDesktopRpcRegistrationRegistry {
    private static final Log log = Log.getLog(DDDesktopRpcRegistrationRegistry.class);
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.model.datadam.desktopRpcService";
    private static DDDesktopRpcRegistrationRegistry instance;
    private final List<DDDesktopRpcRegistration> registrations = new ArrayList<>();

    private DDDesktopRpcRegistrationRegistry() {
        for (var extension : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"service".equals(extension.getName())) {
                continue;
            }
            try {
                registrations.add((DDDesktopRpcRegistration) extension.createExecutableExtension("class"));
            } catch (CoreException e) {
                throw new IllegalStateException("Cannot create desktop RPC service from "
                    + extension.getContributor().getName(), e);
            }
        }
        log.debug("Found " + registrations.size() + " DataDam desktop RPC registrations");
    }

    @NotNull
    public static DDDesktopRpcRegistrationRegistry getInstance() {
        if (instance == null) {
            instance = new DDDesktopRpcRegistrationRegistry();
        }
        return instance;
    }

    public void registerAll(@NotNull MessagingRpcEndpoint endpoint) {
        for (DDDesktopRpcRegistration registration : registrations) {
            try {
                registration.register(endpoint);
            } catch (RuntimeException e) {
                throw new IllegalStateException("Cannot register desktop RPC service "
                    + registration.getClass().getName(), e);
            }
        }
    }
}
