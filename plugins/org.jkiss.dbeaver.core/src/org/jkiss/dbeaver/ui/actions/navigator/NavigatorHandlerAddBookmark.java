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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.resources.bookmarks.BookmarksHandlerImpl;

public class NavigatorHandlerAddBookmark extends NavigatorHandlerObjectBase {

    private IFolder targetFolder;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell activeShell = HandlerUtil.getActiveShell(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            final DBNNode node = NavigatorUtils.getSelectedNode(selection);
            if (node instanceof DBNDatabaseNode) {
                try {
                    AddBookmarkDialog dialog = new AddBookmarkDialog(activeShell, (DBNDatabaseNode) node);
                    final String title = dialog.chooseName();
                    if (title != null) {
                        BookmarksHandlerImpl.createBookmark((DBNDatabaseNode) node, title, dialog.getTargetFolder());
                    }
                } catch (DBException e) {
                    UIUtils.showErrorDialog(
                            activeShell,
                            CoreMessages.actions_navigator_bookmark_error_title,
                            CoreMessages.actions_navigator_bookmark_error_message, e);
                }
            }
        }
        return null;
    }

    private class AddBookmarkDialog extends EnterNameDialog {
        private DBNDatabaseNode node;

        public AddBookmarkDialog(Shell parentShell, DBNDatabaseNode node) {
            super(parentShell, CoreMessages.actions_navigator_bookmark_title, node.getNodeName());
            this.node = node;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            final Control area = super.createDialogArea(parent);

            final IProject project = node.getOwnerProject();
            if (project != null) {
                IFolder bookmarksFolder = BookmarksHandlerImpl.getBookmarksFolder(project, false);
                if (bookmarksFolder != null) {
                    DBNResource bookmarksFolderNode = node.getModel().getNodeByResource(bookmarksFolder);
                    if (bookmarksFolderNode != null) {
                        UIUtils.createControlLabel((Composite) area, "Bookmark folder");
                        DatabaseNavigatorTree foldersNavigator = new DatabaseNavigatorTree((Composite) area, bookmarksFolderNode, SWT.BORDER | SWT.SINGLE, true);
                        final GridData gd = new GridData(GridData.FILL_BOTH);
                        gd.widthHint = 200;
                        gd.heightHint = 200;
                        foldersNavigator.setLayoutData(gd);
                        final TreeViewer treeViewer = foldersNavigator.getViewer();

                        if (targetFolder != null && targetFolder.exists()) {
                            DBNResource targetNode = node.getModel().getNodeByResource(targetFolder);
                            if (targetNode != null) {
                                treeViewer.setSelection(new StructuredSelection(targetNode));
                            }
                        }

                        treeViewer.addFilter(new ViewerFilter() {
                            @Override
                            public boolean select(Viewer viewer, Object parentElement, Object element) {
                                return element instanceof DBNResource && ((DBNResource) element).getResource() instanceof IFolder;
                            }
                        });
                        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                            @Override
                            public void selectionChanged(SelectionChangedEvent event) {
                                IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                                Object object = structSel.isEmpty() ? null : structSel.getFirstElement();
                                if (object instanceof DBNResource && ((DBNResource) object).getResource() instanceof IFolder) {
                                    targetFolder = (IFolder) ((DBNResource) object).getResource();
                                }
                            }
                        });
                        treeViewer.expandAll();
                    }
                }
            }

            return area;
        }

        public IFolder getTargetFolder() {
            return targetFolder;
        }
    }
}