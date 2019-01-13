/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.osgi.framework.BundleContext;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The activator class controls the plug-in life cycle
 */
public class ERDActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.erd";

	// The shared instance
	private static ERDActivator plugin;
	
    private static ResourceBundle resourceBundle;

    private DBPPreferenceStore preferences;

    /**
     * The constructor
     */
	public ERDActivator() {
	}

	@Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
		plugin = this;
        preferences = new BundlePreferenceStore(getBundle());

        try {
            resourceBundle = ResourceBundle.getBundle(ERDMessages.BUNDLE_NAME);
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }

        // Switch off D3D because of Sun XOR painting bug
        // See http://www.jgraph.com/forum/viewtopic.php?t=4066
        System.setProperty("sun.java2d.d3d", Boolean.FALSE.toString()); //$NON-NLS-1$
	}

	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ERDActivator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

    /**
     * Returns the plugin's resource bundle,
     * @return core resource bundle
     */
    public static ResourceBundle getResourceBundle()
    {
        return resourceBundle;
    }

    public DBPPreferenceStore getPreferences() {
        return preferences;
    }
}
