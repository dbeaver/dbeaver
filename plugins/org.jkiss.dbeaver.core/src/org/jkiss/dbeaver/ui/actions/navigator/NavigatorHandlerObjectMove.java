/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavigatorHandlerObjectMove extends NavigatorHandlerObjectBase {

    private static final Log log = Log.getLog(NavigatorHandlerObjectMove.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node == null || !(node.getParentNode() instanceof DBNContainer)) {
            return null;
        }
        DBNContainer containerNode = (DBNContainer) node.getParentNode();
        DBSObject object = ((DBNDatabaseNode) node).getObject();
        if (!(object instanceof DBPOrderedObject)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        DBEObjectReorderer<DBSObject> objectReorderer = EntityEditorsRegistry.getInstance().getObjectManager(object.getClass(), DBEObjectReorderer.class);
        if (objectReorderer == null) {
            return null;
        }
        DBPOrderedObject orderedObject = (DBPOrderedObject) object;
        try {
            // Sibling objects - they are involved in reordering process
            List<DBSObject> siblingObjects = new ArrayList<>();
            for (DBNNode siblingNode : node.getParentNode().getChildren(new VoidProgressMonitor())) {
                if (siblingNode instanceof DBNDatabaseNode) {
                    DBSObject siblingObject = ((DBNDatabaseNode) siblingNode).getObject();
                    if (siblingObject.getClass() != object.getClass()) {
                        log.warn("Sibling object class " + siblingObject.getClass() + " differs from moving object class " + object.getClass().getName());
                    } else {
                        siblingObjects.add(siblingObject);
                    }
                } else {
                    log.warn("Wrong sibling node type: " + siblingNode);
                }
            }

            CommandTarget commandTarget = getCommandTarget(
                HandlerUtil.getActiveWorkbenchWindow(event),
                containerNode,
                object.getClass(),
                false);
            String actionId = event.getCommand().getId();

            switch (actionId) {
                case CoreCommands.CMD_OBJECT_MOVE_UP:
                    objectReorderer.setObjectOrdinalPosition(
                        commandTarget.getContext(),
                        object,
                        siblingObjects,
                        orderedObject.getOrdinalPosition() - 1);
                    break;
                case CoreCommands.CMD_OBJECT_MOVE_DOWN:
                    objectReorderer.setObjectOrdinalPosition(
                        commandTarget.getContext(),
                        object,
                        siblingObjects,
                        orderedObject.getOrdinalPosition() + 1);
                    break;
            }

            if (object.isPersisted() && commandTarget.getEditor() == null) {
                Map<String, Object> options = DBPScriptObject.EMPTY_OPTIONS;
                if (!showScript(HandlerUtil.getActiveWorkbenchWindow(event), commandTarget.getContext(), options, "Reorder script")) {
                    commandTarget.getContext().resetChanges();
                    return false;
                } else {
                    ObjectSaver orderer = new ObjectSaver(commandTarget.getContext(), options);
                    TasksJob.runTask("Change object '" + object.getName() + "' position", orderer);
                }
            }
        } catch (DBException e) {
            DBUserInterface.getInstance().showError("Object move", "Error during object reposition", e);
        }
        return null;
    }

}