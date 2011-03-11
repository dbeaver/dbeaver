/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorInput;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorStandalone;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.impl.project.AbstractResourceHandler;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Bookmarks handler
 */
public class ERDResourceHandler extends AbstractResourceHandler {

    private static final String ERD_DIR = "Diagrams";
    private static final String ERD_EXT = "erd"; //$NON-NLS-1$

    public static final String RES_TYPE_DIAGRAMS = "erd"; //$NON-NLS-1$

    public static IFolder getDiagramsFolder(IProject project)
    {
        return project.getFolder(ERD_DIR);
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IFolder) {
                return FEATURE_DELETE | FEATURE_RENAME | FEATURE_CREATE_FOLDER | FEATURE_MOVE_INTO;
            }
            return FEATURE_CREATE_FOLDER | FEATURE_MOVE_INTO;
        } else {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
    }

    public String getTypeName(IResource resource)
    {
        if (resource instanceof IFolder) {
            return "diagram folder";
        } else {
            return "diagram";
        }
    }

    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFolder diagramsFolder = getDiagramsFolder(project);
        if (!diagramsFolder.exists()) {
            diagramsFolder.create(true, true, monitor);
        }
        diagramsFolder.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_DIAGRAMS);
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            return new DBNDiagram(parentNode, resource, this);
        } else {
            return new DBNDiagramFolder(parentNode, resource, this);
        }
    }

    @Override
    public void openResource(final IResource resource, final IWorkbenchWindow window) throws CoreException, DBException
    {
        if (!(resource instanceof IFile)) {
            return;
        }

        ERDEditorInput erdInput = new ERDEditorInput((IFile)resource);
        window.getActivePage().openEditor(
            erdInput,
            ERDEditorStandalone.class.getName());
    }

    public static IFile createDiagram(
        final EntityDiagram copyFrom,
        final String title,
        IFolder folder,
        DBRProgressMonitor monitor)
        throws DBException
    {
        if (folder == null) {
            folder = getDiagramsFolder(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
        }
        if (folder == null) {
            throw new DBException("Can't detect folder for diagram");
        }

        final IFile file = ContentUtils.getUniqueFile(folder, CommonUtils.escapeFileName(title), ERD_EXT);

        try {
            DBRRunnableWithProgress runnable = new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        EntityDiagram newDiagram = copyFrom == null ? new EntityDiagram(null, "<Diagram>") : copyFrom.copy();
                        newDiagram.setName(title);

                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        newDiagram.save(buffer);
                        InputStream data = new ByteArrayInputStream(buffer.toByteArray());

                        file.create(data, true, monitor.getNestedMonitor());
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            if (monitor == null) {
                DBeaverCore.getInstance().runInProgressService(runnable);
            } else {
                runnable.run(monitor);
            }
        } catch (InvocationTargetException e) {
            throw new DBException(e.getTargetException());
        } catch (InterruptedException e) {
            // interrupted
        }

        return file;
    }


}
