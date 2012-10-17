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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

import java.io.File;
import java.net.URL;

/**
 * This class controls all aspects of the application's execution
 */
public class DBeaverApplication implements IApplication
{

    public static final String DBEAVER_DEFAULT_DIR = ".dbeaver"; //$NON-NLS-1$

    /* (non-Javadoc)
    * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
    */
    @Override
    public Object start(IApplicationContext context)
    {
        Display display = PlatformUI.createDisplay();

        Location instanceLoc = Platform.getInstanceLocation();
        String defaultHomePath = getDefaultWorkspaceLocation().getAbsolutePath();
        try {
            URL defaultHomeURL = new URL(
                "file",  //$NON-NLS-1$
                null,
                defaultHomePath);
            instanceLoc.set(defaultHomeURL, true);
        } catch (Throwable e) {
            // Just skip it
            // Error may occur if -data parameter was specified at startup
            System.err.println("Can't switch workspace to '" + defaultHomePath + "' - " + e.getMessage());  //$NON-NLS-1$ //$NON-NLS-2$
        }
/*
        try {
            if (instanceLoc.isLocked()) {
                System.out.println("LOCKED!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        System.out.println("Install path: '" + Platform.getInstallLocation().getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("Workspace path: '" + instanceLoc.getURL() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        PlatformUI.getPreferenceStore().setDefault(
            IWorkbenchPreferenceConstants.KEY_CONFIGURATION_ID,
            ApplicationWorkbenchAdvisor.DBEAVER_SCHEME_NAME);
        try {
            int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
        } finally {
/*
            try {
                Job.getJobManager().join(null, new NullProgressMonitor());
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
*/
            display.dispose();
        }
    }

    private static File getDefaultWorkspaceLocation() {
        return new File(
            System.getProperty("user.home"),
            DBEAVER_DEFAULT_DIR);
    }

    /* (non-Javadoc)
      * @see org.eclipse.equinox.app.IApplication#stop()
      */
    @Override
    public void stop()
    {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null)
            return;
        final Display display = workbench.getDisplay();
        display.syncExec(new Runnable()
        {
            @Override
            public void run()
            {
                if (!display.isDisposed())
                    workbench.close();
            }
        });
    }


}
