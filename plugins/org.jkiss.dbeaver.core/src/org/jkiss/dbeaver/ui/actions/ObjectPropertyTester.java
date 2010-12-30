/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
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
    //static final Log log = LogFactory.getLog(ObjectPropertyTester.class);

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
            if (node instanceof DBNContainer) {
                // Try to detect child type
                objectType = ((DBNContainer)node).getItemsClass();
            } else if (node.isManagable()) {
                objectType = node.getObject() == null ? null : node.getObject().getClass();
            }
            if (objectType == null || !hasExtendedManager(objectType)) {
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
                if (nodeObject.isManagable()) {
                    DBSObject pasteObject = nodeObject.getObject();
                    if (pasteObject == null || objectType != pasteObject.getClass()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        } else if (property.equals(PROP_CAN_DELETE)) {
            return
                node.getParentNode() instanceof DBNContainer &&
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
        return entityManager != null && DBOCreator.class.isAssignableFrom(entityManager.getManagerClass());
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}
