/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.jkiss.dbeaver.ui.views.navigator.NavigatorTreeView;

public class DBeaverPerspective implements IPerspectiveFactory
{

    public void createInitialLayout(IPageLayout layout)
    {
        String editorArea = layout.getEditorArea();
        //layout.setEditorAreaVisible(false);

        IFolderLayout treeFolder = layout.createFolder("navigation", IPageLayout.LEFT, 0.3f, editorArea);
        //treeFolder.addPlaceholder(NavigatorTreeView.ID + ":*");
        treeFolder.addView(NavigatorTreeView.VIEW_ID);

        layout.getViewLayout(NavigatorTreeView.VIEW_ID).setCloseable(false);

        // Bottom right.
        IFolderLayout bottomRight = layout.createFolder("bottomRight", IPageLayout.BOTTOM, (float) 0.66, editorArea);

        //bottomRight.addView(IPageLayout.ID_TASK_LIST);
    }
}
