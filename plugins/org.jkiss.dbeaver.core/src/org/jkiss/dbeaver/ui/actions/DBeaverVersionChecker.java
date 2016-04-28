/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.dialogs.VersionUpdateDialog;

import java.io.IOException;

/**
 * Version checker job
 */
public class DBeaverVersionChecker extends AbstractJob {

    private static final Log log = Log.getLog(DBeaverVersionChecker.class);

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
