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
package org.jkiss.dbeaver.ui.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NotificationRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.notifications";

    private static NotificationRegistry instance;

    private final Map<String, NotificationDescriptor> notifications = new HashMap<>();

    private NotificationRegistry(@NotNull IExtensionRegistry registry) {
        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if (NotificationDescriptor.ELEMENT_ID.equals(element.getName())) {
                final NotificationDescriptor descriptor = new NotificationDescriptor(element);
                notifications.put(descriptor.getId(), descriptor);
            }
        }
    }

    @NotNull
    public static synchronized NotificationRegistry getInstance() {
        if (instance == null) {
            instance = new NotificationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public Collection<NotificationDescriptor> getNotifications() {
        return notifications.values();
    }

    @Nullable
    public NotificationDescriptor getNotification(@NotNull String id) {
        return notifications.get(id);
    }
}
