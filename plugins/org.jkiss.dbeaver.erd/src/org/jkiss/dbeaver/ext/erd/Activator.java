/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.erd";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
        super.start(context);
		plugin = this;

        initDefaultPreferences();

        // Switch off D3D because of Sun XOR painting bug
        // See http://www.jgraph.com/forum/viewtopic.php?t=4066
        System.setProperty("sun.java2d.d3d", Boolean.FALSE.toString()); //$NON-NLS-1$
	}

    /*
      * (non-Javadoc)
      * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
      */
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

}
