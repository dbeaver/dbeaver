/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    static final Log log = LogFactory.getLog(DBeaverVersionChecker.class);

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
            if (versionDescriptor.getProgramVersion().compareTo(DBeaverCore.getInstance().getVersion()) > 0) {
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
        final Shell shell = DBeaverCore.getActiveWorkbenchShell();
        shell.getDisplay().asyncExec(new Runnable() {
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
