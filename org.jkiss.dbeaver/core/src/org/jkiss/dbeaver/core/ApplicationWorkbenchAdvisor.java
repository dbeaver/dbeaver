/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.*;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.jkiss.dbeaver.ext.IAutoSaveEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
        try {
            saveAndCleanup();
            // Disconnect all connections
            if (DBeaverCore.getInstance() != null) {
                DBeaverCore.getInstance().getDataSourceRegistry().closeConnections();
                // Wait for all datasource jobs to finish
                DBeaverCore.getInstance().runAndWait2(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        Job.getJobManager().join(DBPDataSource.class, monitor.getNestedMonitor());
                    }
                });
            }
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        }
        catch (Throwable e) {
            // do nothing
            log.debug("Internal error during shutdown process", e);
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

    private void saveAndCleanup()
    {
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IProgressMonitor nullMonitor = new NullProgressMonitor();
        final List<File> openFiles = new ArrayList<File>();
        for (IWorkbenchPage workbenchPage : workbenchWindow.getPages()) {
            for (IEditorReference editorRef : workbenchPage.getEditorReferences()) {
                try {
                    IEditorInput editorInput = editorRef.getEditorInput();

                    if (editorInput instanceof IAutoSaveEditorInput) {
                        IEditorPart editor = editorRef.getEditor(false);
                        if (editor != null && editor.isDirty()) {
                            editor.doSave(nullMonitor);
                        }
                    }

                    if (editorInput instanceof ProjectFileEditorInput) {
                        ProjectFileEditorInput sei = (ProjectFileEditorInput)editorInput;
                        openFiles.add(sei.getPath().toFile());
                    }
                } catch (CoreException ex) {
                    log.error("Can't obtain editor storage", ex);
                }
            }
        }

        IFolder tempFolder;
        try {
            tempFolder = DBeaverCore.getInstance().getAutosaveFolder(VoidProgressMonitor.INSTANCE);
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
                    if (!openFiles.contains(tempFile.getLocation().toFile())) {
                        tempFile.delete(true, false, nullMonitor);
                    }
                }
            }
        } catch (CoreException ex) {
            log.warn("Error deleting autosave files", ex);
        }
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
