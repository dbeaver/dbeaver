/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.*;

//todo log and show errors instead of silent nulls
public class NavigatorHandlerCreateColumnIndex extends NavigatorHandlerObjectCreateBase {
    private static final Log log = Log.getLog(NavigatorHandlerCreateColumnIndex.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBNNode node = NavigatorUtils.getSelectedNode(HandlerUtil.getCurrentSelection(event));
        if (node == null) {
            return null;
        }
        if (!(node instanceof DBNDatabaseItem)) {
            return null;
        }
        DBNDatabaseItem databaseItem = (DBNDatabaseItem) node;
        DBSObject attributeObject = databaseItem.getObject();
        if (!(attributeObject instanceof DBSEntityAttribute)) {
            return null;
        }
        DBSObject entityObject = attributeObject.getParentObject();
        if (!(entityObject instanceof DBSEntity)) {
            return null;
        }
        DBEStructEditor<?> structEditor = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(entityObject.getClass(), DBEStructEditor.class);
        if (structEditor == null) {
            return null;
        }
        DBEObjectMaker<?, ?> maker = null;
        Class<?> indexClass = null;
        for (Class<?> childType: structEditor.getChildTypes()) {
            maker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(childType, DBEObjectMaker.class);
            if (maker != null && maker.canCreateObject(entityObject) && DBSTableIndex.class.isAssignableFrom(childType)) {
                indexClass = childType;
                break;
            }
        }
        if (indexClass == null) {
            return null;
        }
        DBNNode containerNode = node.getParentNode();
        if (containerNode == null) {
            return null;
        }
        DBECommandContext commandContext;
        try {
            commandContext = getCommandTarget(HandlerUtil.getActiveWorkbenchWindow(event), containerNode, indexClass, false).getContext();
        } catch (DBException e) {
            throw new ExecutionException("msg", e);
        }
        //UIUtils.getDefaultRunnableContext().run(); //TODO
        try {
            maker.createNewObject(new VoidProgressMonitor(), commandContext, entityObject, null, Collections.emptyMap());
        } catch (DBException e) {
            throw new ExecutionException("msg", e);
        }
        return null;
    }
}
