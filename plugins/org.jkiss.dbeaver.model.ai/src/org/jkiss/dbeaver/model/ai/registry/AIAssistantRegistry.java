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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.impl.AIAssistantImpl;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;

public class AIAssistantRegistry {

    private static final Log log = Log.getLog(AIAssistantRegistry.class);

    private static final String WORKSPACE_ATTR_ASSISTANT = "ai.assistant";

    private static AIAssistantRegistry instance = null;

    private AIAssistantDescriptor globalDescriptor;

    public static synchronized AIAssistantRegistry getInstance() {
        if (instance == null) {
            instance = new AIAssistantRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    public AIAssistantRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(AIAssistantDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("assistant".equals(ext.getName())) {
                AIAssistantDescriptor descriptor = new AIAssistantDescriptor(ext);
                if (globalDescriptor == null || descriptor.getPriority() > globalDescriptor.getPriority()) {
                    this.globalDescriptor = descriptor;
                }
            }
        }
    }

    public AIAssistantDescriptor getDescriptor() {
        return globalDescriptor;
    }

    @NotNull
    public <T extends AIAssistant> T getAssistant(@NotNull DBPWorkspace workspace) {
        synchronized (this) {
            SMSession workspaceSession = workspace.getWorkspaceSession();
            if (workspaceSession instanceof SMSessionPersistent sp) {
                T assistant = sp.getAttribute(WORKSPACE_ATTR_ASSISTANT);
                if (assistant == null) {
                    assistant = createAssistant(workspace);
                    sp.setAttribute(WORKSPACE_ATTR_ASSISTANT, assistant);
                }
                return assistant;
            }
        }
        // No persistent session - create new assistant on each call
        return createAssistant(workspace);
    }

    @SuppressWarnings("unchecked")
    public <T extends AIAssistant> T createAssistant(@NotNull DBPWorkspace workspace) {
        try {
            if (globalDescriptor != null) {
                return (T) globalDescriptor.createInstance(workspace);
            }
        } catch (DBException e) {
            log.error("Error creating AI assistant", e);
        }
        return (T)new AIAssistantImpl(workspace);
    }

}
