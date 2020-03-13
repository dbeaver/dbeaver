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
package org.jkiss.dbeaver.ext.postgresql;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class PostgreUIActivator extends AbstractUIPlugin {
    
    public static final String IMG_PG_SQL = "IMG_PG_SQL"; //$NON-NLS-1$

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.postgresql"; //$NON-NLS-1$

	// The shared instance
	private static PostgreUIActivator plugin;
    private static BundleContext bundleContext;

    public PostgreUIActivator() {
	}

	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		plugin = this;
	}

	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		bundleContext = context;
		super.stop(context);
	}

	public static PostgreUIActivator getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	@Override
	protected void initializeImageRegistry(ImageRegistry reg)
	{
	    super.initializeImageRegistry(reg);
	    reg.put(IMG_PG_SQL, getImageDescriptor("$nl$/icons/postgresql_icon.png")); //$NON-NLS-1$
	}
	
	public IEventBroker getEventBroker() {
        IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(bundleContext);
        return serviceContext.get(IEventBroker.class);
    }
}
