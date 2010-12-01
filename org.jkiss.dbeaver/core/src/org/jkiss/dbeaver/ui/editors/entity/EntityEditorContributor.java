/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.SimpleAction;

/**
 * Entity Editor contributor
 */
public class EntityEditorContributor extends MultiPageEditorActionBarContributor
{
    private EntityEditor curEditor;
    private IEditorPart curPage;

    @Override
    public void setActiveEditor(IEditorPart part)
    {
        super.setActiveEditor(part);
        curEditor = (EntityEditor) part;
    }

    @Override
    public void setActivePage(IEditorPart activeEditor) {
        curPage = activeEditor;
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);

/*
        // Execution
        manager.add(saveChangesAction);
        manager.add(revertChangesAction);
        manager.add(previewAction);
*/
    }

}