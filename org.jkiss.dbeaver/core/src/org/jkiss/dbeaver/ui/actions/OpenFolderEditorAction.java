/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IEditorReference;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditor;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditorInput;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeFolder;

public class OpenFolderEditorAction extends NavigatorAction
{
    static Log log = LogFactory.getLog(OpenSQLEditorAction.class);

    public OpenFolderEditorAction()
    {
        setId(ICommandIds.CMD_OPEN_FOLDEREDITOR);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/tree/edit_folder.png"));
        setText("Open Folder Editor");
    }

    public void run()
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode instanceof DBMTreeFolder) {
            try {
                for (IEditorReference ref : getActiveWindow().getActivePage().getEditorReferences()) {
                    if (ref.getEditorInput() instanceof FolderEditorInput && ((FolderEditorInput)ref.getEditorInput()).getFolder() == selectedNode) {
                        getActiveWindow().getActivePage().activate(ref.getEditor(false));
                        return;
                    }
                }
                FolderEditorInput folderInput = new FolderEditorInput((DBMTreeFolder)selectedNode);
                getActiveWindow().getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            } catch (Exception ex) {
                log.error("Can't open folder editor", ex);
            }
        }
    }

}