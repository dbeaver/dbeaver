/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeFolder;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditor;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditorInput;

public class OpenObjectEditorAction extends NavigatorAction
{
    static Log log = LogFactory.getLog(OpenSQLEditorAction.class);

    public void run(IAction action)
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode != null) {
            openEntityEditor(selectedNode, getWindow());
        }
    }

    public static void openEntityEditor(DBMNode selectedNode, IWorkbenchWindow workbenchWindow)
    {
        try {
            for (IEditorReference ref : workbenchWindow.getActivePage().getEditorReferences()) {
                if (ref.getEditorInput() instanceof EntityEditorInput && ((EntityEditorInput)ref.getEditorInput()).getNode() == selectedNode) {
                    workbenchWindow.getActivePage().activate(ref.getEditor(false));
                    return;
                }
            }
            if (selectedNode instanceof DBMTreeFolder) {
                FolderEditorInput folderInput = new FolderEditorInput((DBMTreeFolder)selectedNode);
                workbenchWindow.getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            } else {
                EntityEditorInput editorInput = new EntityEditorInput(selectedNode);
                workbenchWindow.getActivePage().openEditor(
                    editorInput,
                    EntityEditor.class.getName());
            }
        } catch (Exception ex) {
            log.error("Can't open editor", ex);
        }
    }

}