/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeItem;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;

/**
 * ObjectPropertyTester
 */
public class ObjectPropertyTester extends PropertyTester
{
    static final Log log = LogFactory.getLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.object";
    public static final String PROP_CAN_CREATE = "canCreate";
    public static final String PROP_CAN_DELETE = "canDelete";

    public ObjectPropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBNNode)) {
            return false;
        }
        DBNNode node = (DBNNode)receiver;

        if (property.equals(PROP_CAN_CREATE)) {
            Class objectType = null;
            if (node instanceof DBNTreeFolder) {
                // Try to detect child type
                DBNTreeFolder folder = (DBNTreeFolder)node;
                if (!CommonUtils.isEmpty(folder.getMeta().getChildren())) {
                    DBXTreeNode childMeta = folder.getMeta().getChildren().get(0);
                    //childMeta.
                }
            } else if (node instanceof DBNTreeItem) {
                objectType = node.getObject() == null ? null : node.getObject().getClass();
            }
            return
                objectType != null &&
                hasExtendedManager(objectType);
        } else if (property.equals(PROP_CAN_DELETE)) {
            return
                node instanceof DBNTreeItem &&
                node.getObject() != null &&
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