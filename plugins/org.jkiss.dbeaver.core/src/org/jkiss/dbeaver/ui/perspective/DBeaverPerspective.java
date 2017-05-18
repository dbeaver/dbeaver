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
package org.jkiss.dbeaver.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.texteditor.templates.TemplatesView;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.ui.views.qm.QueryManagerView;

public class DBeaverPerspective implements IPerspectiveFactory
{

    public static final String FOLDER_NAVIGATION = "navigation"; //$NON-NLS-1$
    public static final String FOLDER_HELP = "help"; //$NON-NLS-1$
    public static final String BOTTOM_BOTTOM_LEFT = "bottomLeft"; //$NON-NLS-1$
    public static final String FOLDER_BOTTOM_RIGHT = "bottomRight"; //$NON-NLS-1$

    @Override
    public void createInitialLayout(IPageLayout layout)
    {
        String editorArea = layout.getEditorArea();
        //layout.setEditorAreaVisible(false);

        // Navigator
        IFolderLayout treeFolder = layout.createFolder(
            FOLDER_NAVIGATION,
            IPageLayout.LEFT,
            0.30f,
            editorArea);
        treeFolder.addView(DatabaseNavigatorView.VIEW_ID);
        treeFolder.addView(ProjectNavigatorView.VIEW_ID);
        treeFolder.addPlaceholder(TemplatesView.ID);

        // Right
        IPlaceholderFolderLayout right = layout.createPlaceholderFolder(
            FOLDER_HELP,
            IPageLayout.RIGHT,
            0.8f,
            editorArea);
        right.addPlaceholder(IActionConstants.HELP_VIEW_ID);

        // Bottom left.
        //IPlaceholderFolderLayout bottomLeft = layout.createPlaceholderFolder(
        IFolderLayout bottomLeft = layout.createFolder(
            BOTTOM_BOTTOM_LEFT,
            IPageLayout.BOTTOM,
            0.7f,
            FOLDER_NAVIGATION);
        bottomLeft.addView(ProjectExplorerView.VIEW_ID);

        // Bottom right.
        IPlaceholderFolderLayout bottomRight = layout.createPlaceholderFolder(
            FOLDER_BOTTOM_RIGHT,
            IPageLayout.BOTTOM,
            0.7f,
            editorArea);
        bottomRight.addPlaceholder(IActionConstants.LOG_VIEW_ID);
        bottomRight.addPlaceholder(QueryManagerView.VIEW_ID);
        bottomRight.addPlaceholder(ShellProcessView.VIEW_ID);
        bottomRight.addPlaceholder(IPageLayout.ID_PROP_SHEET);
        bottomRight.addPlaceholder(IPageLayout.ID_PROGRESS_VIEW);
        bottomRight.addPlaceholder(IPageLayout.ID_OUTLINE);
        bottomRight.addPlaceholder(IPageLayout.ID_TASK_LIST);
        bottomRight.addPlaceholder(IPageLayout.ID_BOOKMARKS);

        // Search views
        bottomRight.addPlaceholder("org.eclipse.search.ui.views.SearchView");
        bottomRight.addPlaceholder("org.jkiss.dbeaver.ui.search.DatabaseSearchView");
        bottomRight.addPlaceholder("org.jkiss.dbeaver.core.shellProcess");

        // Add view shortcuts
        layout.addShowViewShortcut(DatabaseNavigatorView.VIEW_ID);
        layout.addShowViewShortcut(ProjectNavigatorView.VIEW_ID);
        layout.addShowViewShortcut(ProjectExplorerView.VIEW_ID);
        layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
        layout.addShowViewShortcut(QueryManagerView.VIEW_ID);
        layout.addShowViewShortcut(IActionConstants.LOG_VIEW_ID);
    }

}
