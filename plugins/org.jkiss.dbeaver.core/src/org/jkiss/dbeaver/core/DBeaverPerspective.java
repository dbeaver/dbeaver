/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectNavigatorView;
import org.jkiss.dbeaver.ui.views.qm.QueryManagerView;

public class DBeaverPerspective implements IPerspectiveFactory
{

    public static final String FOLDER_NAVIGATION = "navigation";
    public static final String BOTTOM_BOTTOM_LEFT = "bottomLeft";
    public static final String FOLDER_BOTTOM_RIGHT = "bottomRight";

    public void createInitialLayout(IPageLayout layout)
    {
        String editorArea = layout.getEditorArea();
        //layout.setEditorAreaVisible(false);

        // Navigator
        IFolderLayout treeFolder = layout.createFolder(
            FOLDER_NAVIGATION,
            IPageLayout.LEFT,
            0.60f,
            editorArea);
        //treeFolder.addPlaceholder(DatabaseNavigatorView.ID + ":*");
        treeFolder.addView(DatabaseNavigatorView.VIEW_ID);
        treeFolder.addView(ProjectNavigatorView.VIEW_ID);

        // Bottom left.
        //IPlaceholderFolderLayout bottomLeft = layout.createPlaceholderFolder(
        IFolderLayout bottomLeft = layout.createFolder(
            BOTTOM_BOTTOM_LEFT,
            IPageLayout.BOTTOM,
            0.8f,
            FOLDER_NAVIGATION);
        bottomLeft.addPlaceholder(IPageLayout.ID_OUTLINE);
        bottomLeft.addView(ProjectExplorerView.VIEW_ID);

        // Bottom right.
        IFolderLayout bottomRight = layout.createFolder(
            FOLDER_BOTTOM_RIGHT,
            IPageLayout.BOTTOM,
            0.2f,
            editorArea);
        bottomRight.addPlaceholder("org.eclipse.pde.runtime.LogView");
        bottomRight.addPlaceholder(QueryManagerView.VIEW_ID);
        bottomRight.addPlaceholder(IPageLayout.ID_PROP_SHEET);

        //layout.getViewLayout(DatabaseNavigatorView.VIEW_ID).setCloseable(false);
    }
}
