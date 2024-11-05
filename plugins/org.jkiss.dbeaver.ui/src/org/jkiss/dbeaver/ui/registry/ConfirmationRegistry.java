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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConfirmationRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.confirmations";

    private static ConfirmationRegistry instance;

    private final Map<String, ConfirmationDescriptor> confirmations = new HashMap<>();

    private ConfirmationRegistry(@NotNull IExtensionRegistry registry) {
        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if (ConfirmationDescriptor.ELEMENT_ID.equals(element.getName())) {
                final ConfirmationDescriptor descriptor = new ConfirmationDescriptor(element);
                confirmations.put(descriptor.getId(), descriptor);
            }
        }
    }

    @NotNull
    public static synchronized ConfirmationRegistry getInstance() {
        if (instance == null) {
            instance = new ConfirmationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public Collection<ConfirmationDescriptor> getConfirmations() {
        return confirmations.values();
    }

    @NotNull
    public ConfirmationDescriptor getConfirmation(@NotNull String id) {
        ConfirmationDescriptor descriptor = confirmations.get(id);
        if (descriptor == null) {
            throw new IllegalArgumentException("Can't find confirmation '" + id + "'");
        }
        return descriptor;
    }

    public int confirmAction(@Nullable Shell shell, @NotNull String id, int type, int imageType, @NotNull Object... args) {
        ConfirmationDescriptor descriptor = getConfirmation(id);
        return ConfirmationDialog.open(
            type,
            imageType == -1 ? type : imageType,
            shell,
            NLS.bind(descriptor.getTitle(), args),
            NLS.bind(descriptor.getMessage(), args),
            descriptor.getToggleMessage() != null ? NLS.bind(descriptor.getToggleMessage(), args) : null,
            false,
            ConfirmationDialog.PREF_KEY_PREFIX + id
        );
    }
}
