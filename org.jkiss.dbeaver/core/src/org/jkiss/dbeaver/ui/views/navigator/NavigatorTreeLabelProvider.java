package org.jkiss.dbeaver.ui.views.navigator;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NavigatorTreeLabelProvider
*/
class NavigatorTreeLabelProvider extends LabelProvider
{
    static Log log = LogFactory.getLog(NavigatorTreeLabelProvider.class);

    private NavigatorTreeView view;

    NavigatorTreeLabelProvider(NavigatorTreeView view)
    {
        this.view = view;
    }

    public String getText(Object obj)
    {
        if (obj instanceof ILabelProvider) {
            return ((ILabelProvider)obj).getText(obj);
        } else if (obj instanceof DBSObject) {
            return ((DBSObject) obj).getName();
        } else if (obj instanceof DBMNode) {
            return ((DBMNode) obj).getNodeName();
        }
        return obj.toString();
    }

    public Image getImage(Object obj)
    {
        if (obj instanceof ILabelProvider) {
            return ((ILabelProvider)obj).getImage(obj);
        }
        DBMNode node = view.getMetaModel().findNode(obj);
        if (node != null) {
            return node.getNodeIconDefault();
        } else {
            return null;
        }
    }

}
