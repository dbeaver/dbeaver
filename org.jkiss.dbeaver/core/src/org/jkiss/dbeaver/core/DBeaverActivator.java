/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jkiss.dbeaver.ui.DBeaverConstants;
import org.osgi.framework.BundleContext;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The activator class controls the plug-in life cycle
 */
public class DBeaverActivator extends AbstractUIPlugin
{

    // The shared instance
    private static DBeaverActivator instance;
    private ResourceBundle resourceBundle;

    /**
     * The constructor
     */
    public DBeaverActivator()
    {
    }

    public static DBeaverActivator getInstance()
    {
        return instance;
    }

    /*
      * (non-Javadoc)
      * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
      */
    @Override
    public void start(BundleContext context)
        throws Exception
    {
        super.start(context);
        instance = this;
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", DBeaverLogger.class.getName());
        try {
            resourceBundle = ResourceBundle.getBundle("org.jkiss.dbeaver.core.DBeaverResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    /*
      * (non-Javadoc)
      * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
      */
    public void stop(BundleContext context)
        throws Exception
    {
        instance = null;

        super.stop(context);
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path)
    {
        return imageDescriptorFromPlugin(DBeaverConstants.PLUGIN_ID, path);
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle()
    {
        return resourceBundle;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     */
    public static String getResourceString(String key)
    {
        ResourceBundle bundle = DBeaverCore.getInstance().getPlugin().getResourceBundle();
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
    /**
     * Returns the workspace instance.
     */
    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

}
