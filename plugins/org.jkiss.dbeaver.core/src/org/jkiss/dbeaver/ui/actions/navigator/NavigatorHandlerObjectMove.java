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
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class NavigatorHandlerObjectMove extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node == null || !(node.getParentNode() instanceof DBNContainer)) {
            return null;
        }
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
            CommandTarget commandTarget = getCommandTarget(
                HandlerUtil.getActiveWorkbenchWindow(event),
                (DBNContainer) node.getParentNode(),
                object.getClass(),
                false);
            String actionId = event.getCommand().getId();

            switch (actionId) {
                case CoreCommands.CMD_OBJECT_MOVE_UP:
                    objectReorderer.setObjectOrdinalPosition(
                        commandTarget.getContext(),
                        object,
                        orderedObject.getOrdinalPosition() - 1);
                    break;
                case CoreCommands.CMD_OBJECT_MOVE_DOWN:
                    objectReorderer.setObjectOrdinalPosition(
                        commandTarget.getContext(),
                        object,
                        orderedObject.getOrdinalPosition() + 1);
                    break;
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(HandlerUtil.getActiveShell(event), "Object move", "Error during object reposition", e);
        }
        return null;
    }

}