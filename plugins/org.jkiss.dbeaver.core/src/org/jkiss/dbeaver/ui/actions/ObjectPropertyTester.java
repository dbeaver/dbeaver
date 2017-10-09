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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * ObjectPropertyTester
 */
public class ObjectPropertyTester extends PropertyTester
{
    //static final Log log = Log.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.object";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE = "canCreate";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_CAN_RENAME = "canRename";
    public static final String PROP_CAN_MOVE_UP = "canMoveUp";
    public static final String PROP_CAN_MOVE_DOWN = "canMoveDown";
    public static final String PROP_CAN_FILTER = "canFilter";
    public static final String PROP_CAN_FILTER_OBJECT = "canFilterObject";
    public static final String PROP_HAS_FILTER = "hasFilter";

    public ObjectPropertyTester() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (!(receiver instanceof DBNNode)) {
            return false;
        }
        Display display = Display.getCurrent();
        if (display == null) {
            return false;
        }
        DBNNode node = (DBNNode)receiver;
//System.out.println("TEST " + property + " ON " + node.getName());

        switch (property) {
            case PROP_CAN_OPEN:
                return node.isPersisted();
            case PROP_CAN_CREATE:
            case PROP_CAN_PASTE: {

                if (node instanceof DBNResource) {
                    return property.equals(PROP_CAN_PASTE);
                }

                Class objectType;
                if (!(node instanceof DBNContainer)) {
                    if (node.getParentNode() instanceof DBNContainer) {
                        node = node.getParentNode();
                    }
                }
                DBNContainer container;
                if (node instanceof DBNDataSource) {
                    return true;
                }
                if (node instanceof DBNContainer) {
                    // Try to detect child type
                    objectType = ((DBNContainer) node).getChildrenClass();
                    container = (DBNContainer) node;
                } else {
                    return false;
                }

                if (node instanceof DBSWrapper && isReadOnly(((DBSWrapper) node).getObject())) {
                    return false;
                }
                if (objectType == null) {
                    return false;
                }
                DBEObjectMaker objectMaker = getObjectManager(objectType, DBEObjectMaker.class);
                if (objectMaker == null) {
                    return false;
                }
                if (!objectMaker.canCreateObject(container.getValueObject())) {
                    return false;
                }
                // Do not check PASTE command state. It requires clipboard contents check
                // which means UI interaction which can break menu popup [RedHat]
                // and also is a slow operation. So let paste be always enabled.
/*
            if (property.equals(PROP_CAN_CREATE)) {
                return true;
            }
            // Check objects in clipboard
            Collection<DBNNode> cbNodes = TreeNodeTransfer.getFromClipboard();
            if (cbNodes == null) {
                return false;
            }
            for (DBNNode nodeObject : cbNodes) {
                if (nodeObject.isManagable() && nodeObject instanceof DBSWrapper) {
                    DBSObject pasteObject = ((DBSWrapper)nodeObject).getObject();
                    if (pasteObject == null || objectType != pasteObject.getClass()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
*/
                return true;
            }
            case PROP_CAN_DELETE: {
                if (node instanceof DBNDataSource || node instanceof DBNLocalFolder) {
                    return true;
                }
                if (node instanceof DBSWrapper) {
                    DBSObject object = ((DBSWrapper) node).getObject();
                    if (object == null || isReadOnly(object) || !(node.getParentNode() instanceof DBNContainer)) {
                        return false;
                    }
                    DBEObjectMaker objectMaker = getObjectManager(object.getClass(), DBEObjectMaker.class);
                    return objectMaker != null && objectMaker.canDeleteObject(object);
                } else if (node instanceof DBNResource) {
                    if ((((DBNResource) node).getFeatures() & DBPResourceHandler.FEATURE_DELETE) != 0) {
                        return true;
                    }
                }
                break;
            }
            case PROP_CAN_RENAME: {
                if (node.supportsRename()) {
                    return true;
                }
                if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    return
                        object != null &&
                            !isReadOnly(object) &&
                            object.isPersisted() &&
                            node.getParentNode() instanceof DBNContainer &&
                            getObjectManager(object.getClass(), DBEObjectRenamer.class) != null;
                }
                break;
            }
            case PROP_CAN_MOVE_UP:
            case PROP_CAN_MOVE_DOWN: {
                if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    if (object instanceof DBPOrderedObject) {
                        DBEObjectReorderer objectReorderer = getObjectManager(object.getClass(), DBEObjectReorderer.class);
                        if (objectReorderer != null) {
                            final int position = ((DBPOrderedObject) object).getOrdinalPosition();
                            if (property.equals(PROP_CAN_MOVE_UP)) {
                                return position > objectReorderer.getMinimumOrdinalPosition(object);
                            }
                            return position < objectReorderer.getMaximumOrdinalPosition(object);
                        }
                    }
                }
                break;
            }
            case PROP_CAN_FILTER: {
                if (node instanceof DBNDatabaseItem) {
                    node = node.getParentNode();
                }
                if (node instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) node).getItemsMeta() != null) {
                    return true;
                }
                break;
            }
            case PROP_CAN_FILTER_OBJECT: {
                if (node.getParentNode() instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) node.getParentNode()).getItemsMeta() != null) {
                    return true;
                }
                break;
            }
            case PROP_HAS_FILTER: {
                if (node instanceof DBNDatabaseItem) {
                    node = node.getParentNode();
                }
                if (node instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) node).getItemsMeta() != null) {
                    DBSObjectFilter filter = ((DBNDatabaseFolder) node).getNodeFilter(((DBNDatabaseFolder) node).getItemsMeta(), true);
                    if ("defined".equals(expectedValue)) {
                        return filter != null && !filter.isEmpty();
                    } else {
                        return filter != null && !filter.isNotApplicable();
                    }
                }
                break;
            }
        }
        return false;
    }

    private boolean isReadOnly(DBSObject object)
    {
        if (object == null) {
            return true;
        }
        DBPDataSource dataSource = object.getDataSource();
        return dataSource == null || dataSource.getContainer().isConnectionReadOnly();
    }

    private static <T extends DBEObjectManager> T getObjectManager(Class<?> objectType, Class<T> managerType)
    {
        return EntityEditorsRegistry.getInstance().getObjectManager(objectType, managerType);
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
