/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NavigatorHandlerObjectMove extends NavigatorHandlerObjectBase {

    private static final Log log = Log.getLog(NavigatorHandlerObjectMove.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final List<DBNNode> nodes = NavigatorUtils.getSelectedNodes(selection);
        final DBNNode[][] consecutiveNodes = groupConsecutiveNodes(nodes);

        final int min = getNodePosition(nodes.get(0));
        final int max = getNodePosition(nodes.get(nodes.size() - 1));
        final String commandId = event.getCommand().getId();
        final boolean downwards = commandId.equals(NavigatorCommands.CMD_OBJECT_MOVE_BOTTOM) || commandId.equals(NavigatorCommands.CMD_OBJECT_MOVE_DOWN);

        if (downwards) {
            // Objects must be moved down in reverse order to avoid overlapping
            ArrayUtils.reverse(consecutiveNodes);
        }

        for (DBNNode[] partition : consecutiveNodes) {
            for (DBNNode node : partition) {
                if (!(node.getParentNode() instanceof DBNContainer)) {
                    return null;
                }
                DBSObject object = ((DBNDatabaseNode) node).getObject();
                if (!(object instanceof DBPOrderedObject)) {
                    return null;
                }

                @SuppressWarnings("unchecked")
                DBEObjectReorderer<DBSObject> objectReorderer = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectReorderer.class);
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
                        node.getParentNode(),
                        null, object.getClass(),
                        false);

                    final int shift;

                    switch (commandId) {
                        case NavigatorCommands.CMD_OBJECT_MOVE_TOP:
                            shift = -min + 1;
                            break;
                        case NavigatorCommands.CMD_OBJECT_MOVE_UP:
                            shift = -1;
                            break;
                        case NavigatorCommands.CMD_OBJECT_MOVE_BOTTOM:
                            shift = objectReorderer.getMaximumOrdinalPosition(object) - max + partition.length - 1;
                            break;
                        case NavigatorCommands.CMD_OBJECT_MOVE_DOWN:
                            shift = partition.length;
                            break;
                        default:
                            throw new ExecutionException("Unexpected command: " + event.getCommand());
                    }

                    objectReorderer.setObjectOrdinalPosition(
                        commandTarget.getContext(),
                        object,
                        siblingObjects,
                        orderedObject.getOrdinalPosition() + shift);

                    if (object.isPersisted() && commandTarget.getEditor() == null) {
                        Map<String, Object> options = DBPScriptObject.EMPTY_OPTIONS;
                        if (!showScript(HandlerUtil.getActiveWorkbenchWindow(event), commandTarget.getContext(), options, "Reorder script")) {
                            commandTarget.getContext().resetChanges(true);
                            return false;
                        } else {
                            ObjectSaver orderer = new ObjectSaver(commandTarget.getContext(), options);
                            TasksJob.runTask("Change object '" + object.getName() + "' position", orderer);
                        }
                    }
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Object move", "Error during object reposition", e);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Performs grouping of a consecutive nodes.
     *
     * @param nodes nodes to group
     * @return an array of arrays containing consecutive nodes
     */
    @NotNull
    private static DBNNode[][] groupConsecutiveNodes(@NotNull List<DBNNode> nodes) {
        final List<DBNNode[]> ranges = new ArrayList<>();
        final List<DBNNode> range = new ArrayList<>();

        for (int index = 1, length = nodes.size(); index <= length; index++) {
            range.add(nodes.get(index - 1));
            if (index == length || getNodePosition(nodes.get(index - 1)) != getNodePosition(nodes.get(index)) - 1) {
                ranges.add(range.toArray(new DBNNode[0]));
                range.clear();
            }
        }

        return ranges.toArray(new DBNNode[0][]);
    }

    private static int getNodePosition(@NotNull DBNNode node) {
        if (!(node instanceof DBNDatabaseNode)) {
            return -1;
        }
        final DBSObject object = ((DBNDatabaseNode) node).getObject();
        if (!(object instanceof DBPOrderedObject)) {
            return -1;
        }
        return ((DBPOrderedObject) object).getOrdinalPosition();
    }
}