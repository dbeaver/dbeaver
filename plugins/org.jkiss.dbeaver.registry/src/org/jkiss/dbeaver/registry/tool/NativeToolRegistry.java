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
package org.jkiss.dbeaver.registry.tool;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

public class NativeToolRegistry {
    private static NativeToolRegistry instance = null;

    private NativeToolRegistry(@NotNull IExtensionRegistry registry) {
    }

    public synchronized static NativeToolRegistry getInstance() {
        if (instance == null) {
            instance = new NativeToolRegistry(Platform.getExtensionRegistry());
        }
        for (IConfigurationElement nativeClientsElement : config.getChildren("nativeClients")) {
            for (IConfigurationElement clientElement : nativeClientsElement.getChildren("client")) {
                this.nativeClients.add(new NativeClientDescriptor(clientElement));
            }
        }
    }
}
