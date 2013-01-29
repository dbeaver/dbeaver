/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * ObjectPropertyTester
 */
public class ObjectPropertyTester extends PropertyTester
{
    //static final Log log = LogFactory.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.object";
    public static final String PROP_CAN_OPEN = "canOpen";
    public static final String PROP_CAN_CREATE = "canCreate";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_DELETE = "canDelete";
    public static final String PROP_CAN_RENAME = "canRename";
    public static final String PROP_CAN_FILTER = "canFilter";
    public static final String PROP_HAS_TOOLS = "hasTools";

    public ObjectPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBNNode)) {
            return false;
        }
        Display display = Display.getCurrent();
        if (display == null || DBeaverUI.getActiveWorkbenchShell() != display.getActiveShell()) {
            return false;
        }
        DBNNode node = (DBNNode)receiver;

        if (property.equals(PROP_CAN_OPEN)) {
            return node.isPersisted();
        } else if (property.equals(PROP_CAN_CREATE) || property.equals(PROP_CAN_PASTE)) {
            Class objectType = null;
            if (!(node instanceof DBNContainer)) {
                if (node.getParentNode() instanceof DBNContainer) {
                    node = node.getParentNode();
                }
            }
            if (node instanceof DBNContainer) {
                // Try to detect child type
                objectType = ((DBNContainer)node).getChildrenClass();
            }/* else if (node.isManagable() && node instanceof DBSWrapper) {
                objectType = ((DBSWrapper)node).getObject() == null ? null : ((DBSWrapper)node).getObject().getClass();
            }*/
            if (node instanceof DBSWrapper && isReadOnly(((DBSWrapper) node).getObject())) {
                return false;
            }
            if (objectType == null || !hasObjectManager(objectType, DBEObjectMaker.class)) {
                return false;
            }
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
            return true;
        } else if (property.equals(PROP_CAN_DELETE)) {
            if (node instanceof DBNDataSource) {
                return true;
            }
            if (node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper)node).getObject();
                return
                    object != null &&
                    !isReadOnly(object) &&
                    node.getParentNode() instanceof DBNContainer &&
                    hasObjectManager(object.getClass(), DBEObjectMaker.class);
            } else if (node instanceof DBNResource) {
                if ((((DBNResource)node).getFeatures() & DBPResourceHandler.FEATURE_DELETE) != 0) {
                    return true;
                }
            }
        } else if (property.equals(PROP_CAN_RENAME)) {
            if (node.supportsRename()) {
                return true;
            }
            if (node instanceof DBNDatabaseNode) {
                DBSObject object = ((DBNDatabaseNode)node).getObject();
                return
                    !isReadOnly(object) &&
                    node.getParentNode() instanceof DBNContainer &&
                    object != null &&
                    hasObjectManager(object.getClass(), DBEObjectRenamer.class);
            }
        } else if (property.equals(PROP_CAN_FILTER)) {
            if (node instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) node).getItemsMeta() != null) {
                return true;
            }
        } else if (property.equals(PROP_HAS_TOOLS)) {
            if (node instanceof DBNDatabaseNode) {
                DBSObject object = ((DBNDatabaseNode)node).getObject();
                if (object.getDataSource() != null) {
                    DriverDescriptor driver = (DriverDescriptor) object.getDataSource().getContainer().getDriver();
                    return !CommonUtils.isEmpty(driver.getProviderDescriptor().getTools(object));
                }
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

    private static boolean hasObjectManager(Class objectType, Class<? extends DBEObjectManager> managerType)
    {
        return DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(objectType, managerType) != null;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}
