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

        // Search views
        bottomRight.addPlaceholder("org.eclipse.search.ui.views.SearchView");
        bottomRight.addPlaceholder("org.jkiss.dbeaver.ui.search.DatabaseSearchView");
        bottomRight.addPlaceholder("org.jkiss.dbeaver.core.shellProcess");
    }

}
