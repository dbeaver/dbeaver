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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.*;

//todo log and show errors instead of silent nulls
//todo rename class and everything added alongside
public class NavigatorHandlerCreateColumnIndex extends NavigatorHandlerObjectCreateBase {
    private static final Log log = Log.getLog(NavigatorHandlerCreateColumnIndex.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBNNode node = NavigatorUtils.getSelectedNode(HandlerUtil.getCurrentSelection(event));
        if (node == null) {
            return null;
        }
        Collection<DBSEntityConstraintType> constraintTypes = extractConstraintTypes(node);
        if (constraintTypes.isEmpty()) {
            return null;
        }
        EditConstraintPage constraintPage = new EditConstraintPage(
            "Create constraint or index",
            (DBSEntity) ((DBNDatabaseItem) node).getObject().getParentObject(), //FIXME
            constraintTypes.toArray(new DBSEntityConstraintType[0]),
            false
        );
        boolean ok = constraintPage.edit();
        if (!ok) {
            return null;
        }
        return null;
    }

    @NotNull
    public static Collection<DBSEntityConstraintType> extractConstraintTypes(@NotNull DBNNode node) {
        if (!(node instanceof DBNDatabaseItem)) {
            return Collections.emptyList();
        }
        DBNDatabaseItem databaseItem = (DBNDatabaseItem) node;
        DBSObject attributeObject = databaseItem.getObject();
        if (!(attributeObject instanceof DBSEntityAttribute)) {
            return Collections.emptyList();
        }
        DBSObject entityObject = attributeObject.getParentObject();
        if (!(entityObject instanceof DBSEntity)) {
            return Collections.emptyList();
        }
        DBEStructEditor<?> entityEditor = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(entityObject.getClass(), DBEStructEditor.class);
        if (entityEditor == null) {
            return Collections.emptyList();
        }
        Collection<DBSEntityConstraintType> constraintTypes = new LinkedHashSet<>();
        for (Class<?> clazz: entityEditor.getChildTypes()) {
            DBEObjectMaker<?, ?> entityChildMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(clazz, DBEObjectMaker.class);
            if (entityChildMaker == null || !entityChildMaker.canCreateObject(entityObject)) {
                continue;
            }
            if (DBSTableIndex.class.isAssignableFrom(clazz)) {
                constraintTypes.add(DBSEntityConstraintType.INDEX);
            } else if (DBSTableConstraint.class.isAssignableFrom(clazz)) {
                constraintTypes.add(DBSEntityConstraintType.PRIMARY_KEY);
                constraintTypes.add(DBSEntityConstraintType.UNIQUE_KEY);
            }
        }
        return constraintTypes;
    }
}
