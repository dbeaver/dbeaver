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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupingRegistry {

    private static Log log = Log.getLog(GroupingRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.grouping"; //$NON-NLS-1$

    private static GroupingRegistry instance;

    private final List<GroupingActionDescriptor> actions = new ArrayList<>();

    private final List<TransformerGroupingFunctionColumnDescriptor> transformedColumns = new ArrayList<>();

    private GroupingRegistry(@NotNull IExtensionRegistry registry) {
        Arrays.stream(registry.getConfigurationElementsFor(EXTENSION_ID)).forEach(this::processElement);
    }

    private void processElement(@NotNull IConfigurationElement element) {
        switch (element.getName()) {
            case GroupingActionDescriptor.TAG_ACTION -> actions.add(new GroupingActionDescriptor(element));
            case TransformerGroupingFunctionColumnDescriptor.TAG_COLUMN ->
                transformedColumns.add(new TransformerGroupingFunctionColumnDescriptor(element));
            default -> log.debug("No corresponding descriptor found for element" + element.getName());
        }
        ;
    }

    @NotNull
    public static synchronized GroupingRegistry getInstance() {
        if (instance == null) {
            instance = new GroupingRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<GroupingActionDescriptor> getActions() {
        return actions;
    }

    @NotNull
    public List<TransformerGroupingFunctionColumnDescriptor> getTransformedColumns() {
        return transformedColumns;
    }
}
