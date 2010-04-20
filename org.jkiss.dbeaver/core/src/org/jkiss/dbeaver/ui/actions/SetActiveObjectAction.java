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
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;
import org.jkiss.dbeaver.model.struct.DBSUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditor;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditorInput;
import org.jkiss.dbeaver.DBException;

public class SetActiveObjectAction extends NavigatorAction
{
    static Log log = LogFactory.getLog(SetActiveObjectAction.class);

    public void run(IAction action)
    {
        DBMNode selectedNode = getSelectedNode();
        if (selectedNode != null) {
            DBSStructureContainerActive activeContainer = DBSUtils.queryParentInterface(
                DBSStructureContainerActive.class, selectedNode.getObject());
            try {
                activeContainer.setActiveChild(selectedNode.getObject());
            }
            catch (DBException e) {
                log.error("Could not set active object", e);
            }
        }
    }

}