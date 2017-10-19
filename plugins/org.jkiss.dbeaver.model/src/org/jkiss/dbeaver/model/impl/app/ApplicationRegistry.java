/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.app;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

public class ApplicationRegistry
{
    private static final Log log = Log.getLog(ApplicationRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.application"; //$NON-NLS-1$

    private static ApplicationRegistry instance = null;

    public synchronized static ApplicationRegistry getInstance()
    {
        if (instance == null) {
            instance = new ApplicationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ApplicationDescriptor> applications = new ArrayList<>();
    private ApplicationDescriptor defaultApplication;

    private ApplicationRegistry(IExtensionRegistry registry)
    {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            ApplicationDescriptor app = new ApplicationDescriptor(ext);
            applications.add(app);
        }

        for (ApplicationDescriptor app : applications) {
            if (app.getParentId() != null) {
                ApplicationDescriptor parentApp = getApplication(app.getParentId());
                if (parentApp == null) {
                    log.error("Parent application '" + app.getParentId() + "' not found");
                } else {
                    app.setParent(parentApp);
                }
            }
        }

        List<ApplicationDescriptor> finalApps = new ArrayList<>();
        for (ApplicationDescriptor app : applications) {
            if (app.isFinalApplication()) {
                finalApps.add(app);
            }
        }
        if (finalApps.isEmpty()) {
            log.error("No applications defined.");
        } else {
            defaultApplication = finalApps.get(0);
            if (finalApps.size() > 1) {
                log.error("Multiple application defined. Use first one (" + defaultApplication.getId() + ")");
            }
        }
    }

    private ApplicationDescriptor getApplication(String id) {
        for (ApplicationDescriptor app : applications) {
            if (app.getId().equals(id)) {
                return app;
            }
        }
        return null;
    }

    public ApplicationDescriptor getApplication() {
        if (defaultApplication == null) {
            throw new IllegalStateException("No DBeaver application was defined");
        }
        return defaultApplication;
    }

}
