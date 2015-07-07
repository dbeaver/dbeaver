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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.actions.DBeaverVersionChecker;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;

import java.util.Calendar;
import java.util.Random;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{
    private static final String PERSPECTIVE_ID = "org.jkiss.dbeaver.core.perspective"; //$NON-NLS-1$
    public static final String DBEAVER_SCHEME_NAME = "org.jkiss.dbeaver.defaultKeyScheme"; //$NON-NLS-1$


    @Override
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    @Override
    public String getInitialWindowPerspectiveId()
    {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer)
    {
        super.initialize(configurer);
        configurer.setSaveAndRestore(true);

        TrayDialog.setDialogHelpAvailable(true);
    }

    @Override
    public void preStartup()
    {
        super.preStartup();
    }

    @Override
    public void postStartup()
    {
        super.postStartup();

        startVersionChecker();
    }

    private void startVersionChecker()
    {
        if (DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK)) {
            if (new Random().nextInt(4) != 0) {
                // check for update with 25% chance
                // to avoid too high load on server in release days
                return;
            }
            long lastVersionCheckTime = DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.UI_UPDATE_CHECK_TIME);
            if (lastVersionCheckTime > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(lastVersionCheckTime);
                int checkDay = cal.get(Calendar.DAY_OF_MONTH);
                cal.setTimeInMillis(System.currentTimeMillis());
                int curDay = cal.get(Calendar.DAY_OF_MONTH);
                if (curDay == checkDay) {
                    return;
                }
            }
            DBeaverCore.getGlobalPreferenceStore().setValue(DBeaverPreferences.UI_UPDATE_CHECK_TIME, System.currentTimeMillis());
            DBeaverVersionChecker checker = new DBeaverVersionChecker(false);
            checker.schedule(3000);
        }
    }

    @Override
    public boolean preShutdown()
    {
        return saveAndCleanup() && super.preShutdown();
    }

    @Override
    public void postShutdown()
    {
        super.postShutdown();
    }

    private boolean saveAndCleanup()
    {
        return closeActiveTransactions();
    }

    private boolean closeActiveTransactions()
    {
        DBeaverCore core = DBeaverCore.getInstance();
        for (IProject project : core.getLiveProjects()) {
            if (project.isOpen()) {
                DataSourceRegistry registry = core.getProjectRegistry().getDataSourceRegistry(project);
                if (registry != null) {
                    for (DataSourceDescriptor dataSourceDescriptor : registry.getDataSources()) {
                        if (!DataSourceHandler.checkAndCloseActiveTransaction(dataSourceDescriptor)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

}
