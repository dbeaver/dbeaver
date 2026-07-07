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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AIChatMessageListRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.ai.chatView";

    private static AIChatMessageListRegistry instance;

    @Nullable
    private final AIChatMessageListDescriptor messageList;

    @NotNull
    public static synchronized AIChatMessageListRegistry getInstance() {
        if (instance == null) {
            instance = new AIChatMessageListRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private AIChatMessageListRegistry(@NotNull IExtensionRegistry registry) {
        Map<String, AIChatMessageListDescriptor> byId = new LinkedHashMap<>();
        List<AIChatMessageListDescriptor> replacements = new ArrayList<>();

        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"messageList".equals(element.getName())) {
                continue;
            }
            AIChatMessageListDescriptor descriptor = new AIChatMessageListDescriptor(element);
            byId.put(descriptor.getId(), descriptor);
            if (!CommonUtils.isEmpty(descriptor.getReplaces())) {
                replacements.add(descriptor);
            }
        }

        for (AIChatMessageListDescriptor descriptor : replacements) {
            byId.remove(descriptor.getId());
            byId.put(descriptor.getReplaces(), descriptor);
        }

        this.messageList = byId.isEmpty() ? null : byId.values().iterator().next();
    }

    @Nullable
    public AIChatMessageListDescriptor getMessageListDescriptor() {
        return messageList;
    }
}
