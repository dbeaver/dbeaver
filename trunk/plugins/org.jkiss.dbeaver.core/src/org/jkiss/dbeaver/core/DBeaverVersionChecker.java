/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.core;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.dialogs.VersionUpdateDialog;

import java.io.IOException;

/**
 * Version checker job
 */
public class DBeaverVersionChecker extends AbstractJob {

    static final Log log = Log.getLog(DBeaverVersionChecker.class);

    private final boolean showAlways;

    public DBeaverVersionChecker(boolean showAlways)
    {
        super("DBeaver new version release checker");
        this.showAlways = showAlways;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            VersionDescriptor versionDescriptor = new VersionDescriptor(VersionDescriptor.DEFAULT_VERSION_URL);
            if (versionDescriptor.getProgramVersion().compareTo(DBeaverCore.getVersion()) > 0) {
                showUpdaterDialog(versionDescriptor);
            } else if (showAlways) {
                showUpdaterDialog(null);
            }
        } catch (IOException e) {
            log.debug(e);
        }
        return Status.OK_STATUS;
    }

    private void showUpdaterDialog(final VersionDescriptor versionDescriptor)
    {
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                VersionUpdateDialog dialog = new VersionUpdateDialog(
                    shell,
                    versionDescriptor);
                dialog.open();
            }
        });
    }
}
