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
    private SaveChangesAction saveChangesAction = new SaveChangesAction();
    private RevertChangesAction revertChangesAction = new RevertChangesAction();
    private PreviewAction previewAction = new PreviewAction();

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

        // Execution
        manager.add(saveChangesAction);
        manager.add(revertChangesAction);
        manager.add(previewAction);
/*
        manager.add(loadAction);
        manager.add(saveAction);
        manager.add(new Separator());
        manager.add(infoAction);
        manager.add(new Separator());
        manager.add(applyAction);
        manager.add(closeAction);
        manager.add(new Separator());
*/
    }

    /////////////////////////////////////////////////////////
    // Actions
    /////////////////////////////////////////////////////////

    private class SaveChangesAction extends SimpleAction
    {
        public SaveChangesAction()
        {
            super(IWorkbenchCommandConstants.FILE_SAVE, "Save", "Apply changes", DBIcon.SAVE_TO_DATABASE);
            setActionDefinitionId(IWorkbenchCommandConstants.FILE_SAVE);
        }

        @Override
        public void run()
        {
            System.out.println("SAVE");
        }
    }

    private class PreviewAction extends SimpleAction
    {
        public PreviewAction()
        {
            super(IWorkbenchCommandConstants.FILE_PROPERTIES, "Preview", "View Script", DBIcon.SQL_PREVIEW);
            setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);
        }

        @Override
        public void run()
        {
            System.out.println("PREVIEW");
        }
    }

    private class RevertChangesAction extends SimpleAction
    {
        public RevertChangesAction()
        {
            super(IWorkbenchCommandConstants.FILE_REVERT, "Revert", "Revert changes", DBIcon.REVERT);
            setActionDefinitionId(IWorkbenchCommandConstants.FILE_REVERT);
        }

        @Override
        public void run()
        {
            System.out.println("REVERT");
        }
    }

}