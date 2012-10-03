/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.erd;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.osgi.framework.BundleContext;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.erd";

	// The shared instance
	private static Activator plugin;
	
    private static ResourceBundle resourceBundle;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
		plugin = this;

        initDefaultPreferences();
        try {
            resourceBundle = ResourceBundle.getBundle(ERDMessages.BUNDLE_NAME);
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }

        // Switch off D3D because of Sun XOR painting bug
        // See http://www.jgraph.com/forum/viewtopic.php?t=4066
        System.setProperty("sun.java2d.d3d", Boolean.FALSE.toString()); //$NON-NLS-1$
	}

    /*
      * (non-Javadoc)
      * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
      */
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
	public static Activator getDefault() {
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

    private void initDefaultPreferences()
    {
        // Init default preferences
        IPreferenceStore store = getPreferenceStore();
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_PAGE_MODE, ERDConstants.PRINT_MODE_DEFAULT);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_TOP, ERDConstants.PRINT_MARGIN_DEFAULT);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_BOTTOM, ERDConstants.PRINT_MARGIN_DEFAULT);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_LEFT, ERDConstants.PRINT_MARGIN_DEFAULT);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_RIGHT, ERDConstants.PRINT_MARGIN_DEFAULT);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_ENABLED, true);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_SNAP_ENABLED, true);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_WIDTH, 20);
        RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_HEIGHT, 20);
    }


    /**
     * Returns the plugin's resource bundle,
     * @return core resource bundle
     */
    public static ResourceBundle getResourceBundle()
    {
        return resourceBundle;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     */
    public static String getResourceString(String key)
    {
        ResourceBundle bundle = getResourceBundle();
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
