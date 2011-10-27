/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.NavigatorUtils;

public class NavigatorHandlerCopyObject extends NavigatorHandlerCopyAbstract {

    @Override
    protected String getObjectDisplayString(Object object)
    {
        DBPNamedObject adapted = RuntimeUtils.getObjectAdapter(object, DBPNamedObject.class);
        if (adapted != null) {
            return DBUtils.convertObjectToString(adapted);
        } else {
            return null;
        }
    }

    @Override
    protected String getSelectionTitle(IStructuredSelection selection)
    {
        if (selection.size() > 1) {
            return CoreMessages.actions_navigator_copy_object_copy_objects;
        }
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node != null) {
            return NLS.bind(CoreMessages.actions_navigator_copy_object_copy_node, node.getNodeType());
        }
        return null;
    }

}
