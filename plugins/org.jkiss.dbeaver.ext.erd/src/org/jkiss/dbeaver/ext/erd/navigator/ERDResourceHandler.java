/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorInput;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorStandalone;
import org.jkiss.dbeaver.ext.erd.model.DiagramLoader;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.resources.AbstractResourceHandler;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * ERD resource handler
 */
public class ERDResourceHandler extends AbstractResourceHandler {

    private static final Log log = Log.getLog(ERDResourceHandler.class);

    private static final String ERD_EXT = "erd"; //$NON-NLS-1$

    public static IFolder getDiagramsFolder(IProject project, boolean forceCreate) throws CoreException
    {
        return DBeaverCore.getInstance().getProjectRegistry().getResourceDefaultRoot(project, ERDResourceHandler.class, forceCreate);
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

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        if (resource instanceof IFolder) {
            return "diagram folder";
        } else {
            return "diagram";
        }
    }

    @NotNull
    @Override
    public String getResourceNodeName(@NotNull IResource resource) {
        if (resource.getParent() instanceof IProject && resource.equals(getDefaultRoot(resource.getProject()))) {
            return "ER Diagrams";
        } else {
            return super.getResourceNodeName(resource);
        }
    }

    @NotNull
    @Override
    public DBNResource makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            return new DBNDiagram(parentNode, resource, this);
        } else {
            return new DBNDiagramFolder(parentNode, resource, this);
        }
    }

    @Override
    public void openResource(@NotNull final IResource resource) throws CoreException, DBException
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
        ResourceUtils.checkFolderExists(folder, monitor);

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

                        file.create(data, true, RuntimeUtils.getNestedMonitor(monitor));
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

    @Override
    public Collection<DBPDataSourceContainer> getAssociatedDataSources(IResource resource) {
        if (resource instanceof IFile) {
            try {
                return DiagramLoader.extractContainers((IFile)resource);
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return super.getAssociatedDataSources(resource);
    }
}
