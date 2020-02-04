/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.ObjectManagerRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectCreateNew;

import java.util.List;

/**
 * ObjectPropertyTester
 */
public class ObjectPropertyTester extends PropertyTester
{
    //static final Log log = Log.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.object";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE_SINGLE = "canCreateSingle";
    public static final String PROP_CAN_CREATE_MULTI = "canCreateMulti";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_CAN_RENAME = "canRename";
    public static final String PROP_CAN_MOVE_UP = "canMoveUp";
    public static final String PROP_CAN_MOVE_DOWN = "canMoveDown";
    public static final String PROP_CAN_FILTER = "canFilter";
    public static final String PROP_CAN_FILTER_OBJECT = "canFilterObject";
    public static final String PROP_HAS_FILTER = "hasFilter";
    public static final String PROP_HAS_TOOLS = "hasTools";

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
            case PROP_CAN_CREATE_SINGLE: {
                return canCreateObject(node, true);
            }
            case PROP_CAN_CREATE_MULTI: {
                return canCreateObject(node, false);
            }
            case PROP_CAN_PASTE: {
                // We cannot interact with clipboard in property testers (#6489).
                // It breaks context menu (and maybe something else) omn some OSes.
/*
                Clipboard clipboard = new Clipboard(display);
                try {
                    if (clipboard.getContents(TreeNodeTransfer.getInstance()) == null) {
                        return false;
                    }
                } finally {
                    clipboard.dispose();
                }
*/
                if (node instanceof DBNResource) {
                    return property.equals(PROP_CAN_PASTE);
                }
                return canCreateObject(node, null);
                // Do not check PASTE command state. It requires clipboard contents check
                // which means UI interaction which can break menu popup [RedHat]
                // and also is a slow operation. So let paste be always enabled.
/*
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
            }
            case PROP_CAN_DELETE: {
                if (node instanceof DBNDataSource || node instanceof DBNLocalFolder) {
                    return true;
                }

                if (DBNUtils.isReadOnly(node)) {
                    return false;
                }

                if (node instanceof DBSWrapper) {
                    DBSObject object = ((DBSWrapper) node).getObject();
                    if (object == null || DBUtils.isReadOnly(object) || !(node.getParentNode() instanceof DBNContainer)) {
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
                    if (DBNUtils.isReadOnly(node)) {
                        return false;
                    }
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    return
                        object != null &&
                            !DBUtils.isReadOnly(object) &&
                            object.isPersisted() &&
                            node.getParentNode() instanceof DBNContainer &&
                            getObjectManager(object.getClass(), DBEObjectRenamer.class) != null;
                }
                break;
            }
            case PROP_CAN_MOVE_UP:
            case PROP_CAN_MOVE_DOWN: {
                if (node instanceof DBNDatabaseNode) {
                    if (DBNUtils.isReadOnly(node)) {
                        return false;
                    }
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

    public static boolean canCreateObject(DBNNode node, Boolean onlySingle) {
        if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode)node).isVirtual()) {
            // Can't create virtual objects
            return false;
        }
        if (onlySingle == null) {
            // Just try to find first create handler
            if (node instanceof DBNDataSource) {
                // We always can create datasource
                return true;
            }

            Class objectType;
            if (!(node instanceof DBNContainer)) {
                if (node.getParentNode() instanceof DBNContainer) {
                    node = node.getParentNode();
                }
            }
            DBNContainer container;
            if (node instanceof DBNContainer) {
                // Try to detect child type
                objectType = ((DBNContainer) node).getChildrenClass();
                container = (DBNContainer) node;
            } else {
                return false;
            }
            if (DBNUtils.isReadOnly(node)) {
                return false;
            }

            if (node instanceof DBSWrapper && DBUtils.isReadOnly(((DBSWrapper) node).getObject())) {
                return false;
            }
            if (objectType == null) {
                return false;
            }
            DBEObjectMaker objectMaker = getObjectManager(objectType, DBEObjectMaker.class);
            if (objectMaker == null) {
                return false;
            }
            return objectMaker.canCreateObject(container.getValueObject());
        }
        if (DBNUtils.isReadOnly(node)) {
            return false;
        }

        // Check whether only single object type can be created or multiple ones
        List<IContributionItem> createItems = NavigatorHandlerObjectCreateNew.fillCreateMenuItems(null, node);

        if (onlySingle) {
            return createItems.size() == 1;
        } else {
            return createItems.size() > 1;
        }
    }

    private static <T extends DBEObjectManager> T getObjectManager(Class<?> objectType, Class<T> managerType)
    {
        return ObjectManagerRegistry.getInstance().getObjectManager(objectType, managerType);
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
