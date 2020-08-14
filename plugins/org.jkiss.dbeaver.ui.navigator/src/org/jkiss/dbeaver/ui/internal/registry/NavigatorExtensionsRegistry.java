/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.internal.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeActionHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NavigatorExtensionsRegistry
 */
public class NavigatorExtensionsRegistry {

    private static final Log log = Log.getLog(NavigatorExtensionsRegistry.class);

    private static NavigatorExtensionsRegistry instance = null;

    public synchronized static NavigatorExtensionsRegistry getInstance() {
        if (instance == null) {
            instance = new NavigatorExtensionsRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<NavigatorNodeActionDescriptor> nodeActions = new ArrayList<>();

    public NavigatorExtensionsRegistry(IExtensionRegistry registry) {
        // Load node action handlers
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(NavigatorNodeActionDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("nodeAction".equals(ext.getName())) {
                try {
                    NavigatorNodeActionDescriptor descriptor = new NavigatorNodeActionDescriptor(ext);
                    nodeActions.add(descriptor);
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
        nodeActions.sort(Comparator.comparingInt(NavigatorNodeActionDescriptor::getOrder));
    }

    public void dispose() {
        nodeActions.clear();
    }

    public List<INavigatorNodeActionHandler> getNodeActions(INavigatorModelView view, DBNNode node) {
        return nodeActions.stream()
            .filter(nad -> (nad.appliesTo(node) || (node instanceof DBNDatabaseNode && nad.appliesTo(((DBNDatabaseNode) node).getObject())))
                && nad.getHandler().isEnabledFor(view, node))
            .map(NavigatorNodeActionDescriptor::getHandler)
            .collect(Collectors.toList());
    }
}
