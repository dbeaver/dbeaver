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
package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectBase;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.resources.bookmarks.BookmarksHandlerImpl;

public class AddBookmarkHandler extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell activeShell = HandlerUtil.getActiveShell(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            final DBNNode node = NavigatorUtils.getSelectedNode(selection);
            DBPProject project = node.getOwnerProject();
            if (project == null || !project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
                return null;
            }
            if (node instanceof DBNDataSource) {
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.actions_navigator_bookmark_error_title,
                    "Connection itself cannot be bookmarked. Choose some element under a connection element.");
                return null;
            }
            if (node instanceof DBNDatabaseNode dbNode) {
                try {
                    createBookmarkDialog(dbNode, activeShell);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError(
                            CoreMessages.actions_navigator_bookmark_error_title,
                            CoreMessages.actions_navigator_bookmark_error_message, e);
                }
            }
        }
        return null;
    }

    public static void createBookmarkDialog(DBNDatabaseNode node, Shell activeShell) throws DBException {
        AddBookmarkDialog dialog = new AddBookmarkDialog(activeShell, node);
        final String title = dialog.chooseName();
        if (title != null) {
            BookmarksHandlerImpl.createBookmark(node, title, dialog.getTargetFolder());
        }
    }

    private static class AddBookmarkDialog extends EnterNameDialog {
        private IFolder targetFolder;
        private DBNDatabaseNode node;

        public AddBookmarkDialog(Shell parentShell, DBNDatabaseNode node) {
            super(parentShell, CoreMessages.actions_navigator_bookmark_title, node.getNodeDisplayName());
            this.node = node;
        }

        protected IDialogSettings getDialogBoundsSettings() {
            return UIUtils.getDialogSettings("DBeaver.AddBookmarkDialog"); //$NON-NLS-1$
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite area = super.createDialogArea(parent);

            final DBPProject project = node.getOwnerProject();
            if (project != null) {
                IFolder bookmarksFolder = BookmarksHandlerImpl.getBookmarksFolder(project, false);
                if (bookmarksFolder != null) {
                    DBNResource bookmarksFolderNode = NavigatorResources.getNodeByResource(node.getModel(), bookmarksFolder);
                    if (bookmarksFolderNode != null) {
                        UIUtils.createControlLabel(area, "Bookmark folder");
                        DatabaseNavigatorTree foldersNavigator = new DatabaseNavigatorTree((Composite) area, bookmarksFolderNode, SWT.BORDER | SWT.SINGLE, true);
                        final GridData gd = new GridData(GridData.FILL_BOTH);
                        gd.widthHint = 200;
                        gd.heightHint = 200;
                        foldersNavigator.setLayoutData(gd);
                        final TreeViewer treeViewer = foldersNavigator.getViewer();

                        if (targetFolder != null && targetFolder.exists()) {
                            DBNResource targetNode = NavigatorResources.getNodeByResource(node.getModel(), targetFolder);
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
                        treeViewer.addSelectionChangedListener(event -> {
                            IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                            Object object = structSel.isEmpty() ? null : structSel.getFirstElement();
                            if (object instanceof DBNResource && ((DBNResource) object).getResource() instanceof IFolder) {
                                targetFolder = (IFolder) ((DBNResource) object).getResource();
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