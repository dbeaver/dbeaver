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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.lang.reflect.Method;

/**
 * EditorUtils
 */
public class EditorUtils {

    public static final String PROP_SQL_DATA_SOURCE = "sql-editor-data-source-id";
    public static final String PROP_SQL_PROJECT = "sql-editor-project-id";
    public static final QualifiedName QN_PROJECT_ID = new QualifiedName("org.jkiss.dbeaver", PROP_SQL_PROJECT);
    public static final QualifiedName QN_DATA_SOURCE_ID = new QualifiedName("org.jkiss.dbeaver", PROP_SQL_DATA_SOURCE);

    private static final Log log = Log.getLog(EditorUtils.class);

    @Nullable
    public static IFile getFileFromInput(IEditorInput editorInput)
    {
        if (editorInput == null) {
            return null;
        } else if (editorInput instanceof IFileEditorInput) {
            return ((IFileEditorInput) editorInput).getFile();
        } else if (editorInput instanceof IPathEditorInput) {
            final IPath path = ((IPathEditorInput) editorInput).getPath();
            return ContentUtils.convertPathToWorkspaceFile(path);
        } else if (editorInput instanceof IURIEditorInput) {
            // Most likely it is an external file
            return null;
        }
        // Try to get path input adapter (works for external files)
        final IPathEditorInput pathInput = editorInput.getAdapter(IPathEditorInput.class);
        if (pathInput != null) {
            final IPath path = pathInput.getPath();
            return ContentUtils.convertPathToWorkspaceFile(path);
        }

        try {
            Method getFileMethod = editorInput.getClass().getMethod("getFile");
            if (IFile.class.isAssignableFrom(getFileMethod.getReturnType())) {
                return IFile.class.cast(getFileMethod.invoke(editorInput));
            }
        } catch (Exception e) {
            //log.debug("Error getting file from editor input with reflection: " + e.getMessage());
            // Just ignore
        }
        return null;
    }

    public static IStorage getStorageFromInput(Object element)
    {
        if (element instanceof IAdaptable) {
            IStorage storage = ((IAdaptable) element).getAdapter(IStorage.class);
            if (storage != null) {
                return storage;
            }
        }
        if (element instanceof IEditorInput) {
            IFile file = getFileFromInput((IEditorInput) element);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public static File getLocalFileFromInput(Object element)
    {
        if (element instanceof IEditorInput) {
            IFile file = getFileFromInput((IEditorInput) element);
            if (file != null) {
                return file.getLocation().toFile();
            }
            if (element instanceof IURIEditorInput) {
                final File localFile = new File(((IURIEditorInput) element).getURI());
                if (localFile.exists()) {
                    return localFile;
                }
            }
        }
        return null;
    }

    //////////////////////////////////////////////////////////
    // Datasource <-> resource manipulations

    public static DBPDataSourceContainer getInputDataSource(IEditorInput editorInput)
    {
        if (editorInput instanceof IDatabaseEditorInput) {
            final DBSObject object = ((IDatabaseEditorInput) editorInput).getDatabaseObject();
            if (object != null) {
                return object.getDataSource().getContainer();
            }
            return null;
        } else if (editorInput instanceof INonPersistentEditorInput) {
            return (DBPDataSourceContainer) ((INonPersistentEditorInput) editorInput).getProperty(PROP_SQL_DATA_SOURCE);
        } else {
            IFile file = getFileFromInput(editorInput);
            if (file != null) {
                return getFileDataSource(file);
            } else {
                File localFile = getLocalFileFromInput(editorInput);
                if (localFile != null) {
                    final DBPExternalFileManager efManager = DBeaverCore.getInstance().getExternalFileManager();
                    String dataSourceId = (String) efManager.getFileProperty(localFile, PROP_SQL_DATA_SOURCE);
                    String projectName = (String) efManager.getFileProperty(localFile, PROP_SQL_PROJECT);
                    if (CommonUtils.isEmpty(dataSourceId) || CommonUtils.isEmpty(projectName)) {
                        return null;
                    }
                    final IProject project = DBeaverCore.getInstance().getWorkspace().getRoot().getProject(projectName);
                    if (project == null || !project.exists()) {
                        log.error("Can't locate project '" + projectName + "' in workspace");
                        return null;
                    }
                    DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
                    return dataSourceRegistry == null ? null : dataSourceRegistry.getDataSource(dataSourceId);

                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    public static DBPDataSourceContainer getFileDataSource(IFile file)
    {
        try {
            if (!file.exists()) {
                return null;
            }
            String projectId = file.getPersistentProperty(QN_PROJECT_ID);
            String dataSourceId = file.getPersistentProperty(QN_DATA_SOURCE_ID);
            if (dataSourceId != null) {
                IProject project = file.getProject();
                if (projectId != null) {
                    final IProject fileProject = DBeaverCore.getInstance().getWorkspace().getRoot().getProject(projectId);
                    if (fileProject != null && fileProject.exists()) {
                        project = fileProject;
                    }
                }
                DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
                return dataSourceRegistry == null ? null : dataSourceRegistry.getDataSource(dataSourceId);
            } else {
                return null;
            }
        } catch (CoreException e) {
            log.error("Internal error while reading file property", e);
            return null;
        }
    }

    public static void setInputDataSource(@NotNull IEditorInput editorInput, @Nullable DBPDataSourceContainer dataSourceContainer, boolean notify)
    {
        if (editorInput instanceof INonPersistentEditorInput) {
            ((INonPersistentEditorInput) editorInput).setProperty(PROP_SQL_DATA_SOURCE, dataSourceContainer);
            return;
        }
        IFile file = getFileFromInput(editorInput);
        if (file != null) {
            setFileDataSource(file, dataSourceContainer, notify);
        } else {
            File localFile = getLocalFileFromInput(editorInput);
            if (localFile != null) {
                final DBPExternalFileManager efManager = DBeaverCore.getInstance().getExternalFileManager();
                efManager.setFileProperty(
                    localFile,
                    PROP_SQL_PROJECT,
                    dataSourceContainer == null ? null : dataSourceContainer.getRegistry().getProject().getName());
                efManager.setFileProperty(
                    localFile,
                    PROP_SQL_DATA_SOURCE,
                    dataSourceContainer == null ? null : dataSourceContainer.getId());
            } else {
                log.error("Can't set datasource for input " + editorInput);
            }
        }
    }

    public static void setFileDataSource(@NotNull IFile file, @Nullable DBPDataSourceContainer dataSourceContainer)
    {
        setFileDataSource(file, dataSourceContainer, false);
    }

    public static void setFileDataSource(@NotNull IFile file, @Nullable DBPDataSourceContainer dataSourceContainer, boolean notify)
    {
        try {
            file.setPersistentProperty(QN_DATA_SOURCE_ID, dataSourceContainer == null ? null : dataSourceContainer.getId());
            file.setPersistentProperty(QN_PROJECT_ID, dataSourceContainer == null ? null : dataSourceContainer.getRegistry().getProject().getName());
            if (notify) {
                NavigatorUtils.refreshNavigatorResource(file, dataSourceContainer);
            }
        } catch (CoreException e) {
            log.error("Internal error while writing file property", e);
        }
    }

    public static void openExternalFileEditor(File file, IWorkbenchWindow window) {
        try {
            IEditorDescriptor desc = window.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
            IFileStore fileStore = EFS.getStore(file.toURI());
            IEditorInput input = new FileStoreEditorInput(fileStore);
            IDE.openEditor(window.getActivePage(), input, desc.getId());
        } catch (CoreException e) {
            log.error("Can't open editor from file '" + file.getAbsolutePath(), e);
        }
    }

}
