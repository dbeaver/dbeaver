/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.internal.core;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DebugCoreActivator implements BundleActivator {

    private static DebugCoreActivator activator;
    private static BundleContext bundleContext;

    private IEventBroker eventBroker;
    
    public static DebugCoreActivator getDefault() {
        return activator;
    }

    static BundleContext getContext() {
        return bundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        activator = this;
        IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(bundleContext);
        eventBroker = serviceContext.get(IEventBroker.class);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        activator = null;
        bundleContext = null;
    }

    public void postEvent(String topic, Object data) {
        eventBroker.post(topic, data);
    }

}
