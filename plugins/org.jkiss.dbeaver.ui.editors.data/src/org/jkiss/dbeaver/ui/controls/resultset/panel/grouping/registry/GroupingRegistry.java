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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.registry;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupingRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.grouping"; //$NON-NLS-1$

    private static GroupingRegistry instance;

    private final List<GroupingActionDescriptor> groupingDescriptors;


    private GroupingRegistry(@NotNull IExtensionRegistry registry) {
        groupingDescriptors = Arrays.stream(registry.getConfigurationElementsFor(EXTENSION_ID))
            .filter(element -> GroupingActionDescriptor.TAG_ACTION.equals(element.getName()))
            .map(GroupingActionDescriptor::new)
            .toList();
    }

    @NotNull
    public static synchronized GroupingRegistry getInstance() {
        if (instance == null) {
            instance = new GroupingRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<GroupingActionDescriptor> getGroupingDescriptors() {
        return new ArrayList<>(groupingDescriptors);
    }
}
