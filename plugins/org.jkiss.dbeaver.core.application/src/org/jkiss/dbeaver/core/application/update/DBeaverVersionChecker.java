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
package org.jkiss.dbeaver.core.application.update;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.updater.VersionDescriptor;

import java.io.IOException;
import java.util.Calendar;

/**
 * Version checker job
 */
public class DBeaverVersionChecker extends AbstractJob {

    private static final Log log = Log.getLog(DBeaverVersionChecker.class);

    private final boolean showAlways;

    public DBeaverVersionChecker(boolean force)
    {
        super("DBeaver new version release checker");
        this.showAlways = force;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        if (monitor.isCanceled() || DBeaverCore.isClosing()) {
            return Status.CANCEL_STATUS;
        }
        boolean showUpdateDialog = showAlways;
        if (!showUpdateDialog) {
            // Check for auto-update settings
            showUpdateDialog = DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK);
            if (showUpdateDialog) {

                long lastVersionCheckTime = DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.UI_UPDATE_CHECK_TIME);
                if (lastVersionCheckTime > 0) {
                    // Do not check more often than daily
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(lastVersionCheckTime);
                    int checkMonth = cal.get(Calendar.MONTH);
                    int checkDay = cal.get(Calendar.DAY_OF_MONTH);
                    cal.setTimeInMillis(System.currentTimeMillis());
                    int curMonth = cal.get(Calendar.MONTH);
                    int curDay = cal.get(Calendar.DAY_OF_MONTH);
                    if (curMonth == checkMonth && curDay == checkDay) {
                        // Already checked today
                        return Status.OK_STATUS;
                    }
                }
            }
        }
        try {
            DBeaverCore.getGlobalPreferenceStore().setValue(DBeaverPreferences.UI_UPDATE_CHECK_TIME, System.currentTimeMillis());
            VersionDescriptor versionDescriptor = new VersionDescriptor(VersionDescriptor.DEFAULT_VERSION_URL);

            if (versionDescriptor.getProgramVersion().compareTo(DBeaverCore.getVersion()) > 0) {
                if (showAlways || showUpdateDialog) {
                    showUpdaterDialog(versionDescriptor);
                }
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
        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                VersionUpdateDialog dialog = new VersionUpdateDialog(
                    DBeaverUI.getActiveWorkbenchShell(),
                    versionDescriptor);
                dialog.open();
            }
        });
    }
}
