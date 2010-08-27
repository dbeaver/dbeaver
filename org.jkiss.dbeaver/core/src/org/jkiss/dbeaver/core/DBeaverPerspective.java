/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.views.navigator.project.ProjectNavigatorView;

public class DBeaverPerspective implements IPerspectiveFactory
{

    public void createInitialLayout(IPageLayout layout)
    {
        String editorArea = layout.getEditorArea();
        //layout.setEditorAreaVisible(false);

        // Navigator
        IFolderLayout treeFolder = layout.createFolder(
            "navigation",
            IPageLayout.LEFT,
            0.60f,
            editorArea);
        //treeFolder.addPlaceholder(DatabaseNavigatorView.ID + ":*");
        treeFolder.addView(DatabaseNavigatorView.VIEW_ID);
        treeFolder.addView(ProjectNavigatorView.VIEW_ID);

        layout.getViewLayout(DatabaseNavigatorView.VIEW_ID).setCloseable(false);

        // Bottom right.
        IFolderLayout bottomRight = layout.createFolder(
            "bottomRight",
            IPageLayout.BOTTOM,
            IPageLayout.RATIO_MIN,
            editorArea);
        bottomRight.addView(IPageLayout.ID_PROP_SHEET);

    }
}
