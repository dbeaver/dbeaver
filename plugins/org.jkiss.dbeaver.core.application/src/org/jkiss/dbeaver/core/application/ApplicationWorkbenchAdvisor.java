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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.keys.IBindingService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.DBeaverVersionChecker;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Random;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{
    static final Log log = LogFactory.getLog(ApplicationWorkbenchAdvisor.class);

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
        DBeaverCore.setStandalone(true);

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
        if (DBeaverCore.getGlobalPreferenceStore().getBoolean(PrefConstants.UI_AUTO_UPDATE_CHECK)) {
            if (new Random().nextInt(4) != 0) {
                // check for update with 25% chance
                // to avoid too high load on server in release days
                return;
            }
            long lastVersionCheckTime = DBeaverCore.getGlobalPreferenceStore().getLong(PrefConstants.UI_UPDATE_CHECK_TIME);
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
            DBeaverCore.getGlobalPreferenceStore().setValue(PrefConstants.UI_UPDATE_CHECK_TIME, System.currentTimeMillis());
            DBeaverVersionChecker checker = new DBeaverVersionChecker(false);
            checker.schedule(3000);
        }
    }

    @Override
    public boolean preShutdown()
    {
        final DBeaverCore core = DBeaverCore.getInstance();
        if (core == null) {
            return true;
        }
        try {
            core.setClosing(true);
            if (!saveAndCleanup()) {
                return false;
            }
            // Disconnect all connections
            // Try to close all connections
            for (IProject project : core.getLiveProjects()) {
                if (!project.isOpen()) {
                    continue;
                }
                final DataSourceRegistry dataSourceRegistry = core.getProjectRegistry().getDataSourceRegistry(project);
                if (dataSourceRegistry != null && !dataSourceRegistry.closeConnections()) {
                    return false;
                }
            }
            // Wait for all datasource jobs to finish
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    Job.getJobManager().join(DBPDataSource.class, monitor.getNestedMonitor());
                }
            });
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        }
        catch (Throwable e) {
            // do nothing
            log.debug("Internal error during shutdown process", e); //$NON-NLS-1$
        }
        finally {
            core.setClosing(false);
        }
        return super.preShutdown();
    }

    @Override
    public void postShutdown()
    {
        super.postShutdown();
    }

    private boolean saveAndCleanup()
    {
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        //IProgressMonitor nullMonitor = new NullProgressMonitor();
        DBRProgressMonitor nullMonitor = VoidProgressMonitor.INSTANCE;
        //final List<File> openFiles = new ArrayList<File>();
        for (IWorkbenchPage workbenchPage : workbenchWindow.getPages()) {
            for (IEditorReference editorRef : workbenchPage.getEditorReferences()) {
                try {
                    IEditorInput editorInput = editorRef.getEditorInput();

                    if (editorInput instanceof IAutoSaveEditorInput && ((IAutoSaveEditorInput) editorInput).isAutoSaveEnabled()) {
                        IEditorPart editor = editorRef.getEditor(false);
                        if (editor != null) {
                            if (!RuntimeUtils.validateAndSave(nullMonitor, editor)) {
                                return false;
                            }
                        }
                    }

/*
                    if (editorInput instanceof ProjectFileEditorInput) {
                        ProjectFileEditorInput sei = (ProjectFileEditorInput)editorInput;
                        openFiles.add(sei.getPath().toFile());
                    }
*/
                } catch (CoreException ex) {
                    log.error("Can't obtain editor storage", ex); //$NON-NLS-1$
                }
            }
            // Save all other editors
            //if (!workbenchPage.saveAllEditors(true)) {
            //    return false;
            //}
        }

/*
        IFolder tempFolder = null;
        try {
            tempFolder = DBeaverCore.getInstance().getAutosaveFolder(VoidProgressMonitor.INSTANCE);
        }
        catch (IOException e) {
            log.error(e);
        }
        if (tempFolder != null) {
            try {
                IResource[] tempResources = tempFolder.members();
                for (IResource tempResource : tempResources) {
                    if (tempResource instanceof IFile) {
                        IFile tempFile = (IFile)tempResource;
                        if (!openFiles.contains(tempFile.getLocation().toFile())) {
                            tempFile.delete(true, false, nullMonitor);
                        }
                    }
                }
            } catch (CoreException ex) {
                log.warn("Error deleting autosave files", ex);
            }
        }
*/
        return true;
    }

}
