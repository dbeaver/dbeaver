/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

public class NavigatorHandlerCopyObject extends NavigatorHandlerCopyAbstract {

    @Override
    protected String getObjectDisplayString(Object object)
    {
        DBPNamedObject adapted = RuntimeUtils.getObjectAdapter(object, DBPNamedObject.class);
        if (adapted != null) {
            return ViewUtils.convertObjectToString(adapted);
        } else {
            return null;
        }
    }

    @Override
    protected String getSelectionTitle(IStructuredSelection selection)
    {
        if (selection.size() > 1) {
            return "Copy Objects";
        }
        DBNNode node = ViewUtils.getSelectedNode(selection);
        if (node != null) {
            return "Copy " + node.getNodeType();
        }
        return null;
    }

}
