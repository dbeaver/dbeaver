/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.model.DBPDeletableObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeItem;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.util.Collection;

/**
 * ObjectPropertyTester
 */
public class ObjectPropertyTester extends PropertyTester
{
    static final Log log = LogFactory.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.object";
    public static final String PROP_CAN_CREATE = "canCreate";
    public static final String PROP_CAN_PASTE = "canPaste";
    public static final String PROP_CAN_DELETE = "canDelete";

    public ObjectPropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBNNode)) {
            return false;
        }
        DBNNode node = (DBNNode)receiver;

        if (property.equals(PROP_CAN_CREATE) || property.equals(PROP_CAN_PASTE)) {
            Class objectType = null;
            if (node instanceof DBNTreeFolder) {
                // Try to detect child type
                objectType = ((DBNTreeFolder)node).getChildrenType(); 
            } else if (node instanceof DBNTreeItem) {
                objectType = node.getObject() == null ? null : node.getObject().getClass();
            }
            if (objectType == null || !hasExtendedManager(objectType)) {
                return false;
            }
            if (property.equals(PROP_CAN_CREATE)) {
                return true;
            }
            // Check objects in clipboard
            Clipboard clipboard = new Clipboard(Display.getDefault());
            Object cbNodes = clipboard.getContents(TreeNodeTransfer.getInstance());
            if (!(cbNodes instanceof Collection)) {
                return false;
            }
            for (Object nodeObject : (Collection)cbNodes) {
                if (nodeObject instanceof DBNTreeNode) {
                    DBSObject pasteObject = ((DBNTreeNode) nodeObject).getObject();
                    if (pasteObject == null || objectType != pasteObject.getClass()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        } else if (property.equals(PROP_CAN_DELETE)) {
            return node instanceof DBPDeletableObject ||
                node instanceof DBNTreeItem &&
                node.getObject() != null &&
                node.getObject().isPersisted() &&
                hasExtendedManager(node.getObject().getClass());
        }
        return false;
    }

    private static boolean hasExtendedManager(Class objectType)
    {
        EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
        EntityManagerDescriptor entityManager = editorsRegistry.getEntityManager(objectType);
        return entityManager != null && IDatabaseObjectManagerEx.class.isAssignableFrom(entityManager.getManagerClass());
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}