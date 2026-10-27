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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.ArrayList;
import java.util.List;

public class AIConnectionSettingsRegistry {
    private static final Log log = Log.getLog(AIConnectionSettingsRegistry.class);
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.ui.ai.connectionSettingsSection";
    private static final AIConnectionSettingsRegistry INSTANCE = new AIConnectionSettingsRegistry();

    @NotNull
    public static AIConnectionSettingsRegistry getInstance() {
        return INSTANCE;
    }

    @NotNull
    public List<AIConnectionSettingsSection> getSections(@NotNull DBPDataSourceContainer connection) {
        List<AIConnectionSettingsSection> sections = new ArrayList<>();
        for (var element : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"section".equals(element.getName())) {
                continue;
            }
            try {
                AIConnectionSettingsSection section = (AIConnectionSettingsSection) element.createExecutableExtension("class");
                if (section.appliesTo(connection)) {
                    sections.add(section);
                }
            } catch (CoreException e) {
                log.error("Cannot load AI connection settings section", e);
            }
        }
        return sections;
    }
}
