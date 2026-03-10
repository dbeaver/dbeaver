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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.descriptor;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GroupingActionRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.grouping"; //$NON-NLS-1$

    private static GroupingActionRegistry instance;

    private final List<GroupingActionDescriptor> actions;

    private GroupingActionRegistry(@NotNull IExtensionRegistry registry) {
        actions = Arrays.stream(registry.getConfigurationElementsFor(EXTENSION_ID))
            .filter(element -> GroupingActionDescriptor.TAG_ACTION.equals(element.getName()))
            .map(GroupingActionDescriptor::new)
            .sorted(Comparator.comparing(GroupingActionDescriptor::getOrder))
            .toList();
    }

    @NotNull
    public static synchronized GroupingActionRegistry getInstance() {
        if (instance == null) {
            instance = new GroupingActionRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<GroupingActionDescriptor> getActions() {
        return actions;
    }

}
