/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The activator class controls the plug-in life cycle
 */
public class DBeaverActivator extends AbstractUIPlugin
{
    static final Log log = Log.getLog(DBeaverActivator.class);

    // The shared instance
    private static DBeaverActivator instance;
    private ResourceBundle pluginResourceBundle, coreResourceBundle;
    private PrintStream debugWriter;
    private DBPPreferenceStore preferences;

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

    @Override
    public void start(BundleContext context)
        throws Exception
    {
        context.addBundleListener(new BundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                if (DBeaverCore.isStandalone()) {
                    if (event.getType() == BundleEvent.STARTED) {
                        log.debug("> Start bundle " + event.getBundle().getSymbolicName() + " [" + event.getBundle().getVersion() + "]");
                    } else if (event.getType() == BundleEvent.STOPPED) {
                        log.debug("< Stop bundle " + event.getBundle().getSymbolicName() + " [" + event.getBundle().getVersion() + "]");
                    }
                }
            }
        });

        super.start(context);

        instance=this;
        Bundle bundle = getBundle();
        ModelPreferences.setMainBundle(bundle);
        preferences=new

        BundlePreferenceStore(bundle);

        DBeaverUI.getInstance();

        try

        {
            coreResourceBundle = ResourceBundle.getBundle(CoreMessages.BUNDLE_NAME);
            pluginResourceBundle = Platform.getResourceBundle(bundle);
        }

        catch(
        MissingResourceException x
        )

        {
            coreResourceBundle = null;
        }
    }

    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        this.shutdownUI();
        this.shutdownCore();

        if (debugWriter != null) {
            debugWriter.close();
            debugWriter = null;
        }
        instance = null;

        super.stop(context);
    }

    private void shutdownUI() {
        DBeaverUI.disposeUI();
    }

    public synchronized PrintStream getDebugWriter()
    {
        if (debugWriter == null) {
            File logPath = Platform.getLogFileLocation().toFile().getParentFile();
            File debugLogFile = new File(logPath, "dbeaver-debug.log"); //$NON-NLS-1$
            if (debugLogFile.exists()) {
                if (!debugLogFile.delete()) {
                    System.err.println("Can't delete debug log file"); //$NON-NLS-1$
                }
            }
            try {
                debugWriter = new PrintStream(debugLogFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace(System.err);
            }
        }
        return debugWriter;
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
        return imageDescriptorFromPlugin(DBeaverCore.PLUGIN_ID, path);
    }

    /**
     * Returns the plugin's resource bundle,
     * @return core resource bundle
     */
    public static ResourceBundle getCoreResourceBundle()
    {
        return getInstance().coreResourceBundle;
    }

    public static ResourceBundle getPluginResourceBundle() {
        return getInstance().pluginResourceBundle;
    }

    public DBPPreferenceStore getPreferences() {
        return preferences;
    }

    /**
     * Returns the workspace instance.
     */
    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    private void shutdownCore()
    {
        try {
            // Dispose core
            if (DBeaverCore.instance != null) {
                DBeaverCore.instance.dispose();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            logMessage("Internal error after shutdown process:" + e.getMessage()); //$NON-NLS-1$
        }
    }

    private void logMessage(String message)
    {
        getDebugWriter().print(message);
    }

}
