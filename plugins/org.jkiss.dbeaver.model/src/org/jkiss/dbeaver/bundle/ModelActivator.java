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
package org.jkiss.dbeaver.bundle;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * The activator class controls the plug-in life cycle
 */
public class ModelActivator extends Plugin
{

    // The shared instance
    private static ModelActivator instance;
    private PrintStream debugWriter;

    /**
     * The constructor
     */
    public ModelActivator()
    {
    }

    public static ModelActivator getInstance()
    {
        return instance;
    }

    @Override
    public void start(BundleContext context)
        throws Exception
    {
        super.start(context);

        instance = this;
    }

    @Override
    public void stop(BundleContext context)
        throws Exception
    {
        if (debugWriter != null) {
            debugWriter.close();
            debugWriter = null;
        }
        instance = null;

        super.stop(context);
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

}
