/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{
    static final Log log = LogFactory.getLog(ApplicationWorkbenchAdvisor.class);

    private static final String PERSPECTIVE_ID = "org.jkiss.dbeaver.core.perspective";

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    public String getInitialWindowPerspectiveId()
    {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer)
    {
        super.initialize(configurer);
        configurer.setSaveAndRestore(true);

        // Initialize DBeaver Core
        DBeaverCore.createInstance(DBeaverActivator.getInstance());
    }

    @Override
    public void preStartup()
    {
        super.preStartup();
        cleanupLobFiles(VoidProgressMonitor.INSTANCE);
    }

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
            for (IProject project : core.getWorkspace().getRoot().getProjects()) {
                final DataSourceRegistry dataSourceRegistry = core.getProjectRegistry().getDataSourceRegistry(project);
                if (dataSourceRegistry != null && !dataSourceRegistry.closeConnections()) {
                    return false;
                }
            }
            // Wait for all datasource jobs to finish
            core.runInProgressService(new DBRRunnableWithProgress() {
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
            log.debug("Internal error during shutdown process", e);
        }
        finally {
            core.setClosing(false);
        }
        return super.preShutdown();
    }

    public void postShutdown()
    {
        super.postShutdown();

        try {
// Dispose core
            if (DBeaverCore.getInstance() != null) {
                DBeaverCore.getInstance().dispose();
            }
        } catch (Throwable e) {
            log.debug("Internal error after shutdown process", e);
        }
    }

    private boolean saveAndCleanup()
    {
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IProgressMonitor nullMonitor = new NullProgressMonitor();
        //final List<File> openFiles = new ArrayList<File>();
        for (IWorkbenchPage workbenchPage : workbenchWindow.getPages()) {
            for (IEditorReference editorRef : workbenchPage.getEditorReferences()) {
                try {
                    IEditorInput editorInput = editorRef.getEditorInput();

                    if (editorInput instanceof IAutoSaveEditorInput && ((IAutoSaveEditorInput) editorInput).isAutoSaveEnabled()) {
                        IEditorPart editor = editorRef.getEditor(false);
                        if (editor != null && editor.isDirty()) {
                            editor.doSave(nullMonitor);
                        }
                    }

/*
                    if (editorInput instanceof ProjectFileEditorInput) {
                        ProjectFileEditorInput sei = (ProjectFileEditorInput)editorInput;
                        openFiles.add(sei.getPath().toFile());
                    }
*/
                } catch (CoreException ex) {
                    log.error("Can't obtain editor storage", ex);
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

    private void cleanupLobFiles(DBRProgressMonitor monitor)
    {
        IFolder tempFolder;
        try {
            tempFolder = DBeaverCore.getInstance().getLobFolder(monitor);
        }
        catch (IOException e) {
            log.error(e);
            return;
        }
        try {
            IResource[] tempResources = tempFolder.members();
            for (IResource tempResource : tempResources) {
                if (tempResource instanceof IFile) {
                    IFile tempFile = (IFile)tempResource;
                    tempFile.delete(true, false, monitor.getNestedMonitor());
                }
            }
        } catch (CoreException ex) {
            log.warn("Error deleting temp lob file", ex);
        }
    }

}
