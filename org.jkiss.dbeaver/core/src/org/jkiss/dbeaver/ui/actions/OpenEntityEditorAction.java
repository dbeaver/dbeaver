/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;

public class OpenEntityEditorAction extends NavigatorAction
{
    static Log log = LogFactory.getLog(OpenSQLEditorAction.class);

    public OpenEntityEditorAction()
    {
        setId(ICommandIds.CMD_OPEN_ENTITYEDITOR);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/tree/edit_item.png"));
        setText("Open Entity Editor");
    }

    public void run()
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode != null) {
            openEntityEditor(selectedNode, getActiveWindow());
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
            EntityEditorInput editorInput = new EntityEditorInput(selectedNode);
            workbenchWindow.getActivePage().openEditor(
                editorInput,
                EntityEditor.class.getName());
        } catch (Exception ex) {
            log.error("Can't open editor", ex);
        }
    }

}