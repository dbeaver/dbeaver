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
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorInput;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorStandalone;
import org.jkiss.dbeaver.ext.erd.model.DiagramLoader;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ui.resources.AbstractResourceHandler;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

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

    public static IFolder getDiagramsFolder(IProject project, boolean forceCreate) throws CoreException
    {
    	if (project == null) {
			return null;
		}
        final IFolder diagramsFolder = project.getFolder(ERD_DIR);
        if (!diagramsFolder.exists() && forceCreate) {
            diagramsFolder.create(true, true, new NullProgressMonitor());
        }
        return diagramsFolder;
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

    @Override
    public String getTypeName(IResource resource)
    {
        if (resource instanceof IFolder) {
            return "diagram folder";
        } else {
            return "diagram";
        }
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
    public void openResource(final IResource resource) throws CoreException, DBException
    {
        if (!(resource instanceof IFile)) {
            return;
        }

        ERDEditorInput erdInput = new ERDEditorInput((IFile)resource);
        DBeaverUI.getActiveWorkbenchWindow().getActivePage().openEditor(
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
            try {
                folder = getDiagramsFolder(DBeaverCore.getInstance().getProjectRegistry().getActiveProject(), true);
            } catch (CoreException e) {
                throw new DBException("Can't obtain folder for diagram", e);
            }
        }
        if (folder == null) {
            throw new DBException("Can't detect folder for diagram");
        }

        final IFile file = ContentUtils.getUniqueFile(folder, CommonUtils.escapeFileName(title), ERD_EXT);

        try {
            DBRRunnableWithProgress runnable = new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        EntityDiagram newDiagram = copyFrom == null ? new EntityDiagram(null, "<Diagram>") : copyFrom.copy();
                        newDiagram.setName(title);
                        newDiagram.setLayoutManualAllowed(true);
                        newDiagram.setLayoutManualDesired(true);

                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        DiagramLoader.save(monitor, null, newDiagram, false, buffer);
                        InputStream data = new ByteArrayInputStream(buffer.toByteArray());

                        file.create(data, true, monitor.getNestedMonitor());
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            if (monitor == null) {
                DBeaverUI.runInProgressService(runnable);
            } else {
                runnable.run(monitor);
            }
        } catch (InvocationTargetException e) {
            throw new DBException("Error creating diagram", e.getTargetException());
        } catch (InterruptedException e) {
            // interrupted
        }

        return file;
    }


}
