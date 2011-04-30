/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.Map;

public class NavigatorHandlerCopyObject extends NavigatorHandlerCopyAbstract {

    @Override
    protected String getObjectDisplayString(Object object)
    {
        Object adapted = Platform.getAdapterManager().getAdapter(object, DBPNamedObject.class);
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
