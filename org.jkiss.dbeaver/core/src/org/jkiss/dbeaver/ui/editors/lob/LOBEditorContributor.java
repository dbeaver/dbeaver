/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;

/**
 * LOB Editor contributor
 */
public class LOBEditorContributor extends MultiPageEditorActionBarContributor
{
    //private IEditorActionBarContributor curActionBarContributor;

    public LOBEditorContributor()
    {
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    public void setActivePage(IEditorPart activeEditor)
    {
/*
        IEditorSite site = activeEditor.getEditorSite();
        IActionBars actionBars = getActionBars();
        actionBars.clearGlobalActionHandlers();
        if (curActionBarContributor != null) {
            curActionBarContributor.dispose();
            curActionBarContributor = null;
        }
        IEditorActionBarContributor actionBarContributor = site.getActionBarContributor();

        actionBarContributor.init(actionBars, site.getWorkbenchWindow().getActivePage());
        actionBarContributor.setActiveEditor(activeEditor);
        actionBars.updateActionBars();
        curActionBarContributor = actionBarContributor;
*/
    }

    @Override
    public void contributeToMenu(IMenuManager menuManager)
    {
        super.contributeToMenu(menuManager);
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
    }

    @Override
    public void contributeToToolBar(IToolBarManager toolBarManager)
    {
        super.contributeToToolBar(toolBarManager);
    }
}