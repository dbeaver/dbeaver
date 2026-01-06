/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class ResultSetErrorActionRegistry {
    private static final String TAG_ACTION = "action"; //$NON-NLS-1$

    private static ResultSetErrorActionRegistry instance;

    private final List<ResultSetErrorActionDescriptor> actions;

    private ResultSetErrorActionRegistry(@NotNull IExtensionRegistry registry) {
        actions = Arrays.stream(registry.getConfigurationElementsFor(ResultSetErrorActionDescriptor.EXTENSION_ID))
            .filter(element -> TAG_ACTION.equals(element.getName()))
            .map(ResultSetErrorActionDescriptor::new)
            .sorted(Comparator.comparing(ResultSetErrorActionDescriptor::getOrder))
            .toList();
    }

    @NotNull
    public static synchronized ResultSetErrorActionRegistry getInstance() {
        if (instance == null) {
            instance = new ResultSetErrorActionRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<ResultSetErrorActionDescriptor> getActions() {
        return actions;
    }
}
