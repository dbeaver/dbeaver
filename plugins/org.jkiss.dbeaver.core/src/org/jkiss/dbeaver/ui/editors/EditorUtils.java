/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.lang.reflect.Method;

/**
 * EditorUtils
 */
public class EditorUtils {

    public static final QualifiedName PROP_DATA_SOURCE_ID = new QualifiedName("org.jkiss.dbeaver", "sql-editor-data-source-id");

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
            //((IURIEditorInput) editorInput).getURI()
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
        } else if (editorInput instanceof IFileEditorInput) {
            return getFileDataSource(((IFileEditorInput) editorInput).getFile());
        } else {
            return null;
        }
    }

    @Nullable
    public static DBPDataSourceContainer getFileDataSource(IFile file)
    {
        try {
            if (!file.exists()) {
                return null;
            }
            String dataSourceId = file.getPersistentProperty(PROP_DATA_SOURCE_ID);
            if (dataSourceId != null) {
                DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(file.getProject());
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
        IFile file = getFileFromInput(editorInput);
        if (file != null) {
            setFileDataSource(file, dataSourceContainer, notify);
        } else {
            log.error("Can't set datasource for input " + editorInput);
        }
    }

    public static void setFileDataSource(@NotNull IFile file, @Nullable DBPDataSourceContainer dataSourceContainer)
    {
        setFileDataSource(file, dataSourceContainer, false);
    }

    public static void setFileDataSource(@NotNull IFile file, @Nullable DBPDataSourceContainer dataSourceContainer, boolean notify)
    {
        try {
            file.setPersistentProperty(PROP_DATA_SOURCE_ID, dataSourceContainer == null ? null : dataSourceContainer.getId());
            if (notify) {
                final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(file.getProject());
                if (projectNode != null) {
                    final DBNResource fileNode = projectNode.findResource(file);
                    if (fileNode != null) {
                        fileNode.refreshResourceState(dataSourceContainer);
                    }
                }
            }
        } catch (CoreException e) {
            log.error("Internal error while writing file property", e);
        }
    }
}
