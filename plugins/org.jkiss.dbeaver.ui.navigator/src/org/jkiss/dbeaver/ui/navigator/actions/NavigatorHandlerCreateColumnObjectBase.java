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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

public abstract class NavigatorHandlerCreateColumnObjectBase extends NavigatorHandlerObjectBase {
    private static final Log log = Log.getLog(NavigatorHandlerCreateColumnObjectBase.class);

    static Object createColumnObject(@NotNull ExecutionEvent event, @NotNull Class<?> columnObjectSuperType) {
        DBNNode node = NavigatorHandlerObjectCreateNew.getNodeFromSelection(HandlerUtil.getCurrentSelection(event));
        if (!(node instanceof DBNDatabaseItem)) {
            log.error("Selected node is not a database item");
            return null;
        }
        DBNNode containerNode = node.getParentNode();
        if (containerNode == null) {
            log.error("Selected node has no parent");
            return null;
        }
        DBSObject attributeObject = ((DBNDatabaseItem) node).getObject();
        if (!(attributeObject instanceof DBSEntityAttribute)) {
            log.error("Selected node's object is not an attribute");
            return null;
        }
        DBSObject entityObject = attributeObject.getParentObject();
        if (!(entityObject instanceof DBSEntity)) {
            log.error("Selected node's attribute has no parent");
            return null;
        }
        DBEStructEditor<?> structEditor = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(entityObject.getClass(), DBEStructEditor.class);
        if (structEditor == null) {
            log.error("No struct editor exists for entity");
            return null;
        }
        DBEObjectMaker<?, ?> maker = null;
        Class<?> columnObjectConcreteType = null;
        for (Class<?> childType: structEditor.getChildTypes()) {
            maker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(childType, DBEObjectMaker.class);
            if (maker != null && maker.canCreateObject(entityObject) && columnObjectSuperType.isAssignableFrom(childType)) {
                columnObjectConcreteType = childType;
                break;
            }
        }
        if (columnObjectConcreteType == null) {
            log.error("Unable to find appropriate child type and it's maker");
            return null;
        }

        DBECommandContext commandContext;
        try {
            commandContext = getCommandTarget(HandlerUtil.getActiveWorkbenchWindow(event), containerNode, null, columnObjectConcreteType, false).getContext();
            createNewObject(maker, commandContext, entityObject);
        } catch (DBException e) {
            log.warn("Unable to create object of type " + columnObjectConcreteType.getName(), e);
        }

        return null;
    }

    static void createNewObject(@NotNull DBEObjectMaker<?, ?> maker, DBECommandContext commandContext,
                                        DBSObject entityObject) throws DBException {
        DBException[] dbExceptions = new DBException[]{null};
        try {
            UIUtils.getDefaultRunnableContext().run(false, false, monitor -> {
                try {
                    maker.createNewObject(monitor, commandContext, entityObject, null, Collections.emptyMap());
                } catch (DBException e) {
                    dbExceptions[0] = e;
                }
            });
        } catch (InvocationTargetException e) {
            log.warn("Unexpected invocation target exception while creating new object", e);
        } catch (InterruptedException e) {
            //ignore
        }

        if (dbExceptions[0] != null) {
            throw dbExceptions[0];
        }
    }
}
