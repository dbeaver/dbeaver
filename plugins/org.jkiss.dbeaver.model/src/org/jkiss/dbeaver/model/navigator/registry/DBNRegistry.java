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
package org.jkiss.dbeaver.model.navigator.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNNodeExtendable;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBNRegistry {
    private static final Log log = Log.getLog(DBNRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.navigator"; //$NON-NLS-1$

    private static DBNRegistry instance = null;

    public synchronized static DBNRegistry getInstance() {
        if (instance == null) {
            instance = new DBNRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DBNModelExtenderDescriptor> modelExtenders = new ArrayList<>();
    private DBNModelExtenderDescriptor defaultApplication;

    private DBNRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (ext.getName().equals("extender")) {
                DBNModelExtenderDescriptor app = new DBNModelExtenderDescriptor(ext);
                modelExtenders.add(app);
            }
        }
    }

    public void extendNode(DBNNodeExtendable parentNode, boolean reflect) {
        if (modelExtenders.isEmpty()) {
            return;
        }
        List<DBNNode> extraNodes = null;
        for (DBNModelExtenderDescriptor med : modelExtenders) {
            try {
                DBNNode[] enList = med.getInstance().getExtraNodes((DBNNode) parentNode);
                if (enList != null) {
                    if (extraNodes == null) {
                        extraNodes = new ArrayList<>();
                    }
                    Collections.addAll(extraNodes, enList);
                }
            } catch (DBException e) {
                log.debug("Error getting model extenders", e);
            }
        }
        if (!CommonUtils.isEmpty(extraNodes)) {
            for (DBNNode eNode : extraNodes) {
                parentNode.addExtraNode(eNode, reflect);
            }
        }
    }

}
