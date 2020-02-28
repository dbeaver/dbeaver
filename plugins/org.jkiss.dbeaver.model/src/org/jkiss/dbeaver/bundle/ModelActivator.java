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
package org.jkiss.dbeaver.bundle;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;
import org.osgi.framework.BundleContext;

import java.io.*;

/**
 * The activator class controls the plug-in life cycle
 */
public class ModelActivator extends Plugin
{

    // The shared instance
    private static ModelActivator instance;

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
        instance = null;

        super.stop(context);
    }

/*
    public synchronized PrintStream getDebugWriter()
    {
        if (debugWriter == null) {
            File logPath = GeneralUtils.getMetadataFolder();
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
*/

}
