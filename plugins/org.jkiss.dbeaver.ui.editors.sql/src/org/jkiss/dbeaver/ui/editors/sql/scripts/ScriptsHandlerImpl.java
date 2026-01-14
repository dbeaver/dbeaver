/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.scripts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPResourceCreator;
import org.jkiss.dbeaver.model.fs.DBFFileStoreProvider;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.resources.AbstractResourceHandler;

import java.util.Collections;
import java.util.List;

/**
 * Scripts handler
 */
public class ScriptsHandlerImpl extends AbstractResourceHandler implements DBPResourceCreator {

    public static final String RESOURCE_TYPE_ID_SQL_SCRIPT = "sql-script";

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        } else {
             return super.getFeatures(resource) | FEATURE_CREATE_FILE;
        }
    }

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        if (resource instanceof IFolder) {
            return "script folder"; //$NON-NLS-1$
        } else {
            return "script"; //$NON-NLS-1$
        }
    }

    @Nullable
    @Override
    public String getResourceDescription(@NotNull IResource resource)
    {
        return SQLEditorUtils.getResourceDescription(resource);
    }

    @Override
    public DBPImage getResourceIcon(@NotNull IResource resource) {
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IProject) {
                return DBIcon.TREE_SCRIPT_FOLDER;
            } else {
                return DBIcon.TREE_FOLDER;
            }
        } else {
            return DBIcon.TREE_SCRIPT;
        }
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException
    {
        IEditorInput input = null;
        if (resource instanceof DBFFileStoreProvider) {
            input = new FileStoreEditorInput(((DBFFileStoreProvider) resource).getFileStore());
        } else if (resource instanceof IFile) {
            input = new FileEditorInput((IFile) resource);
        }
        if (input != null) {
            int matchFlags = IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_IGNORE_SIZE;
            UIUtils.getActiveWorkbenchWindow().getActivePage().openEditor(input, SQLEditor.class.getName(), true, matchFlags);
        } else {
            super.openResource(resource);
        }
    }

    @Nullable
    @Override
    public List<DBPDataSourceContainer> getAssociatedDataSources(DBNResource resource)
    {
        if (resource.getResource() instanceof IFile) {
            DBPDataSourceContainer dataSource = EditorUtils.getFileDataSource((IFile) resource.getResource());
            return dataSource == null ? null : Collections.singletonList(dataSource);
        }
        return null;
    }

    @NotNull
    @Override
    public IResource createResource(@NotNull IFolder folder) throws CoreException, DBException {
        return SQLEditorHandlerOpenEditor.openNewEditor(
            new SQLNavigatorContext(),
            new StructuredSelection(folder));
    }
}
