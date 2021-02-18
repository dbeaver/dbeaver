/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLCommandsRegistry
{
    static final String TAG_COMMAND = "command"; //$NON-NLS-1$

    private static SQLCommandsRegistry instance = null;

    public synchronized static SQLCommandsRegistry getInstance()
    {
        if (instance == null) {
            instance = new SQLCommandsRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, SQLCommandHandlerDescriptor> commandHandlers = new HashMap<>();

    private SQLCommandsRegistry()
    {
    }

    private void loadExtensions(IExtensionRegistry registry)
    {
        IConfigurationElement[] extConfigs = registry.getConfigurationElementsFor(SQLCommandHandlerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extConfigs) {
            // Load functions
            if (TAG_COMMAND.equals(ext.getName())) {
                SQLCommandHandlerDescriptor commandDescriptor = new SQLCommandHandlerDescriptor(ext);
                this.commandHandlers.put(commandDescriptor.getId(), commandDescriptor);
            }
        }
    }

    public void dispose()
    {
        commandHandlers.clear();
    }

    public List<SQLCommandHandlerDescriptor> getCommandHandlers() {
        return new ArrayList<>(commandHandlers.values());
    }

    public SQLCommandHandlerDescriptor getCommandHandler(String id) {
        return commandHandlers.get(id);
    }

}
