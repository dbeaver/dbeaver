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
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.keys.IBindingService;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.DBeaverVersionChecker;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Random;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{
    static final Log log = Log.getLog(ApplicationWorkbenchAdvisor.class);

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

        // Disable all schemas except our own
        final IBindingService bindingService = (IBindingService)configurer.getWorkbench().getService(IBindingService.class);
//        for (Binding binding : bindingService.getBindings()) {
//            System.out.println("binding:" + binding);
//        }
//        for (Scheme scheme : bindingService.getDefinedSchemes()) {
//            if (!scheme.getId().equals(DBEAVER_SCHEME_NAME)) {
//                scheme.undefine();
//            }
//        }

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
        if (!closeActiveTransactions()) {
            return false;
        }
        return true;
    }

    private boolean closeActiveTransactions()
    {
        TransactionCloser closer = new TransactionCloser();
        try {
            DBeaverUI.runInProgressService(closer);
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
        return closer.getResult();
    }

    private static class TransactionCloser extends DBRRunnableWithResult<Boolean> {
        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            result = true;
            DBeaverCore core = DBeaverCore.getInstance();
            for (IProject project : core.getLiveProjects()) {
                if (project.isOpen()) {
                    DataSourceRegistry registry = core.getProjectRegistry().getDataSourceRegistry(project);
                    if (registry != null) {
                        for (DataSourceDescriptor dataSourceDescriptor : registry.getDataSources()) {
                            if (!dataSourceDescriptor.closeActiveTransaction(monitor)) {
                                result = false;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
