/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDPersistedState;
import org.jkiss.dbeaver.erd.model.navigator.DBNDiagram;
import org.jkiss.dbeaver.erd.model.navigator.DBNDiagramFolder;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorStandalone;
import org.jkiss.dbeaver.erd.ui.model.DiagramLoader;
import org.jkiss.dbeaver.erd.ui.model.ERDContentProviderDecorated;
import org.jkiss.dbeaver.erd.ui.model.ERDDecoratorDefault;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.resources.AbstractResourceHandler;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * ERD resource handler
 */
public class ERDResourceHandler extends AbstractResourceHandler {

    private static final Log log = Log.getLog(ERDResourceHandler.class);

    private static final String ERD_EXT = "erd"; //$NON-NLS-1$

    public static IFolder getDiagramsFolder(DBPProject project, boolean forceCreate) throws CoreException
    {
        return DBPPlatformDesktop.getInstance().getWorkspace().getResourceDefaultRoot(project, ERDResourceHandler.class, forceCreate);
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
            super.openResource(resource);
            return;
        }

        FileEditorInput erdInput = new FileEditorInput((IFile)resource);
        UIUtils.getActiveWorkbenchWindow().getActivePage().openEditor(
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
                folder = getDiagramsFolder(DBWorkbench.getPlatform().getWorkspace().getActiveProject(), true);
            } catch (CoreException e) {
                throw new DBException("Can't obtain folder for diagram", e);
            }
        }
        if (folder == null) {
            throw new DBException("Can't detect folder for diagram");
        }
        ResourceUtils.checkFolderExists(folder, monitor);

        final IFile file = ResourceUtils.getUniqueFile(folder, CommonUtils.escapeFileName(title), ERD_EXT);

        try {
            DBRRunnableWithProgress runnable = monitor1 -> {
                try {
                    EntityDiagram newDiagram = copyFrom == null ?
                            new EntityDiagram(null, "<Diagram>", new ERDContentProviderDecorated(), new ERDDecoratorDefault()) :
                            copyFrom.copy();
                    newDiagram.setName(title);
                    newDiagram.setLayoutManualAllowed(true);
                    newDiagram.setLayoutManualDesired(true);

                    String diagramState = DiagramLoader.serializeDiagram(monitor1, null, newDiagram, false, false);
                    InputStream data = new ByteArrayInputStream(diagramState.getBytes(StandardCharsets.UTF_8));

                    file.create(data, true, RuntimeUtils.getNestedMonitor(monitor1));
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            };
            if (monitor == null) {
                UIUtils.runInProgressService(runnable);
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
    public List<DBPDataSourceContainer> getAssociatedDataSources(DBNResource resource) {
        if (resource.getResource() instanceof IFile) {
            try {
                IResource iResource = resource.getResource();
                DBPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(
                    iResource.getProject());
                if (projectMeta == null) {
                    return Collections.emptyList();
                }
                if (iResource instanceof IFile) {
                    try (InputStream is = ((IFile) iResource).getContents()) {
                        return ERDPersistedState.extractContainers(projectMeta, is);
                    }
                }
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return super.getAssociatedDataSources(resource);
    }
}
