/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
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
    public static final String PROP_CAN_RENAME = "canRename";

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
            } else if (node.isManagable() && node instanceof DBSWrapper) {
                objectType = ((DBSWrapper)node).getObject() == null ? null : ((DBSWrapper)node).getObject().getClass();
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
            if (node instanceof DBSWrapper) {
                DBSObject object = ((DBSWrapper)node).getObject();
                return
                    node.getParentNode() instanceof DBNContainer &&
                    object != null &&
                    object.isPersisted() &&
                    hasExtendedManager(object.getClass());
            } else if (node instanceof DBNResource) {
                if ((((DBNResource)node).getFeatures() & DBPResourceHandler.FEATURE_DELETE) != 0) {
                    return true;
                }
            }
        } else if (property.equals(PROP_CAN_RENAME)) {
            return node.supportsRename();
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
