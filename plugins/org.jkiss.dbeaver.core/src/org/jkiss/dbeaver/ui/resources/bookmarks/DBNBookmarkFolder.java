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
package org.jkiss.dbeaver.ui.resources.bookmarks;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.navigator.NavigatorResources;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIIcon;

import java.util.Collection;
import java.util.Collections;

/**
 * DBNBookmarkFolder
 */
public class DBNBookmarkFolder extends DBNResource {
    public DBNBookmarkFolder(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException {
        super(parentNode, resource, handler);
    }

    @NotNull
    @Override
    public DBPImage getResourceNodeIcon() {
        IResource resource = getResource();
        if (NavigatorResources.isRootResource(getOwnerProject(), resource)) {
            return UIIcon.BOOKMARK_FOLDER;
        }
        return super.getResourceNodeIcon();
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode) {
        if (otherNode instanceof DBNDatabaseNode || otherNode instanceof DBNBookmark) {
            return true;
        } else {
            return super.supportsDrop(otherNode);
        }
    }

    @Override
    public void dropNodes(DBRProgressMonitor monitor, Collection<DBNNode> nodes) throws DBException {
        for (DBNNode node : nodes) {
            if (node instanceof DBNDatabaseNode) {
                BookmarksHandlerImpl.createBookmark((DBNDatabaseNode) node,
                    node.getNodeDisplayName(),
                    (IFolder) getResource());
            } else if (node instanceof DBNBookmark) {
                super.dropNodes(monitor, Collections.singleton(node));
            }
        }
    }

    @Override
    public boolean supportsPaste(@NotNull DBNNode other) {
        return other instanceof DBNDatabaseNode;
    }

    @Override
    public void pasteNodes(@NotNull Collection<DBNNode> nodes) throws DBException {
        for (DBNNode node : nodes) {
            if (node instanceof DBNDatabaseNode) {
                BookmarksHandlerImpl.createBookmark((DBNDatabaseNode) node,
                    node.getNodeDisplayName(),
                    (IFolder) getResource());
            }
        }
    }
}
