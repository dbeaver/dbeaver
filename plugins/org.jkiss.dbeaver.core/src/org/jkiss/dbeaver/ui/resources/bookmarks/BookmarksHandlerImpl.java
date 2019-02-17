/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.resources.bookmarks;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.resources.AbstractResourceHandler;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Bookmarks handler
 */
public class BookmarksHandlerImpl extends AbstractResourceHandler {

    private static final String BOOKMARK_EXT = "bm"; //$NON-NLS-1$

    public static IFolder getBookmarksFolder(IProject project, boolean forceCreate)
    {
        return DBWorkbench.getPlatform().getProjectManager().getResourceDefaultRoot(project, BookmarksHandlerImpl.class, forceCreate);
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        if (resource instanceof IFolder) {
            return "bookmark folder"; //$NON-NLS-1$
        } else {
            return "bookmark"; //$NON-NLS-1$
        }
    }

    @NotNull
    @Override
    public String getResourceNodeName(@NotNull IResource resource) {
        if (resource.getParent() instanceof IProject) {
            return "Bookmarks";
        } else {
            return super.getResourceNodeName(resource);
        }
    }

    @NotNull
    @Override
    public DBNResource makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            return new DBNBookmark(parentNode, resource, this);
        } else {
            return new DBNBookmarkFolder(parentNode, resource, this);
        }
    }

    @Override
    public void openResource(@NotNull final IResource resource) throws CoreException, DBException
    {
        if (!(resource instanceof IFile)) {
            return;
        }
        final DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProject(resource.getProject());
        if (projectNode == null) {
            throw new DBException("Can't find project node for '" + resource.getProject().getName() + "'"); //$NON-NLS-2$
        }
        final BookmarkStorage storage = new BookmarkStorage((IFile) resource, false);
        try {
            final DBPDataSourceContainer dataSourceContainer = projectNode.getDatabases().getDataSourceRegistry().getDataSource(storage.getDataSourceId());
            if (dataSourceContainer == null) {
                throw new DBException("Can't find datasource '" + storage.getDataSourceId() + "'"); //$NON-NLS-2$
            }
            final DBNDataSource dsNode = (DBNDataSource) NavigatorUtils.getNodeByObject(dataSourceContainer);
            if (dsNode == null) {
                throw new DBException("Can't find datasource node for '" + dataSourceContainer.getName() + "'"); //$NON-NLS-2$
            }
            dsNode.initializeNode(null, status -> {
                if (status.isOK()) {
                    UIUtils.syncExec(() -> openNodeByPath(dsNode, (IFile) resource, storage));
                } else {
                    DBWorkbench.getPlatformUI().showError(
                        "Open bookmark",
                        "Can't open bookmark",
                        status);
                }
            });
        }
        finally {
            storage.dispose();
        }
    }

    @Override
    public List<DBPDataSourceContainer> getAssociatedDataSources(DBNResource resource) {
        if (resource instanceof DBNBookmark) {
            DBPDataSourceRegistry dataSourceRegistry = DBWorkbench.getPlatform().getProjectManager().getDataSourceRegistry(resource.getResource().getProject());
            if (dataSourceRegistry != null) {
                DBPDataSourceContainer dataSource = dataSourceRegistry.getDataSource(((DBNBookmark) resource).getStorage().getDataSourceId());
                if (dataSource != null) {
                    return Collections.singletonList(dataSource);
                }
            }
        }
        return super.getAssociatedDataSources(resource);
    }

    private static void openNodeByPath(final DBNDataSource dsNode, final IFile file, final BookmarkStorage storage)
    {
        try {
            BookmarkNodeLoader nodeLoader = new BookmarkNodeLoader(dsNode, storage, file);
            UIUtils.runInProgressService(nodeLoader);
            if (nodeLoader.databaseNode != null) {
                UIUtils.syncExec(() -> NavigatorHandlerObjectOpen.openEntityEditor(
                    nodeLoader.databaseNode, null, UIUtils.getActiveWorkbenchWindow()));
            }
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                CoreMessages.model_project_open_bookmark, CoreMessages.model_project_cant_open_bookmark, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void navigateNodeByPath(NavigatorViewBase view, final DBNDataSource dsNode, final BookmarkStorage storage)
    {
        try {
            BookmarkNodeLoader nodeLoader = new BookmarkNodeLoader(dsNode, storage, null);
            UIUtils.runInProgressService(nodeLoader);
            if (nodeLoader.databaseNode != null) {
                UIUtils.syncExec(() -> view.showNode(nodeLoader.databaseNode));
            }
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                CoreMessages.model_project_open_bookmark, CoreMessages.model_project_cant_open_bookmark, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    static DBNDatabaseNode getTargetBookmarkNode(DBRProgressMonitor monitor, DBNBookmark bookmark)
    {
        IFile resource = (IFile) bookmark.getResource();
        final DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProject(resource.getProject());
        if (projectNode != null) {
            BookmarkStorage storage = bookmark.getStorage();
            final DBPDataSourceContainer dataSourceContainer = projectNode.getDatabases().getDataSourceRegistry().getDataSource(storage.getDataSourceId());
            if (dataSourceContainer != null) {
                final DBNDataSource dsNode = (DBNDataSource) NavigatorUtils.getNodeByObject(dataSourceContainer);
                if (dsNode != null) {
                    DBNDatabaseNode[] result = new DBNDatabaseNode[1];
                    dsNode.initializeNode(monitor, status -> {
                        if (status.isOK()) {
                            try {
                                BookmarkNodeLoader nodeLoader = new BookmarkNodeLoader(dsNode, storage, resource);
                                nodeLoader.run(monitor);
                                result[0] = nodeLoader.databaseNode;
                            } catch (Exception e) {
                                // Doesn't matter
                            }
                        }
                    });
                    return result[0];
                }
            }
        }
        return null;
    }

    public static void createBookmark(final DBNDatabaseNode node, String title, IFolder folder) throws DBException
    {
        if (folder == null) {
            final IProject project = node.getOwnerProject();
            if (project != null) {
                folder = getBookmarksFolder(project, true);
            }
        }
        if (folder == null) {
            throw new DBException("Can't detect folder for bookmark");
        }
        ContentUtils.checkFolderExists(folder);

        IFile file = ContentUtils.getUniqueFile(
            folder,
            CommonUtils.escapeFileName(title),
            BOOKMARK_EXT);

        updateBookmark(node, title, file);
    }

    private static void updateBookmark(DBNDatabaseNode node, String title, IFile file)
        throws DBException
    {
        if (CommonUtils.isEmpty(title)) {
            title = node.getNodeName();
        }

        List<String> nodePath = new ArrayList<>();
        for (DBNNode parent = node; !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            nodePath.add(0, parent.getNodeName());
        }
        String dsId = null;
        if (node.getObject() != null && node.getObject().getDataSource() != null) {
            dsId = node.getObject().getDataSource().getContainer().getId();
        }
        BookmarkStorage storage = new BookmarkStorage(
            title,
            node.getNodeType() + " " + node.getNodeName(), //$NON-NLS-1$
            node.getNodeIconDefault(),
            dsId,
            nodePath);

        try {
            InputStream data = storage.serialize();
            file.create(data, true, new NullProgressMonitor());
        } catch (Exception e) {
            throw new DBException("Error saving bookmark", e);
        }
    }

    private static class BookmarkNodeLoader implements DBRRunnableWithProgress {
        private final DBNDataSource dsNode;
        private final BookmarkStorage storage;
        private final IFile file;
        private DBNDatabaseNode databaseNode;

        BookmarkNodeLoader(DBNDataSource dsNode, BookmarkStorage storage, IFile file) {
            this.dsNode = dsNode;
            this.storage = storage;
            this.file = file;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                DBNNode currentNode = dsNode;
                final Collection<String> dataSourcePath = storage.getDataSourcePath();
                for (String path : dataSourcePath) {
                    DBNNode nextChild = null;
                    final DBNNode[] children = currentNode.getChildren(monitor);
                    if (!ArrayUtils.isEmpty(children)) {
                        for (DBNNode node : children) {
                            if (path.equals(node.getNodeName())) {
                                nextChild = node;
                                break;
                            }
                        }
                    }
                    if (nextChild == null) {
                        throw new DBException("Can't find node '" + path + "' in '" + currentNode.getNodeFullName() + "'"); //$NON-NLS-2$ //$NON-NLS-3$
                    }
                    currentNode = nextChild;
                }
                if (currentNode instanceof DBNDatabaseNode) {
                    if (file != null) {
                        // Update bookmark image
                        storage.setImage(currentNode.getNodeIconDefault());
                        file.setContents(storage.serialize(), true, false, RuntimeUtils.getNestedMonitor(monitor));
                    }

                    // Open entity editor
                    databaseNode = (DBNDatabaseNode) currentNode;
                } else if (currentNode != null) {
                    throw new DBException("Node '" + currentNode.getNodeFullName() + "' is not a database object");
                } else {
                    throw new DBException("Can't find database node by path");
                }
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
