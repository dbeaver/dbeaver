/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * NavigatorTreeLabelProvider
*/
class NavigatorTreeLabelProvider extends LabelProvider implements IFontProvider, IColorProvider
{
    static final Log log = LogFactory.getLog(NavigatorTreeLabelProvider.class);

    private NavigatorTreeView view;
    private Font normalFont;
    private Font defaultFont;

    NavigatorTreeLabelProvider(NavigatorTreeView view)
    {
        this.view = view;
        this.normalFont = view.getViewer().getControl().getFont();
        this.defaultFont = UIUtils.makeBoldFont(normalFont);
    }

    @Override
    public void dispose()
    {
        if (defaultFont != null) {
            defaultFont.dispose();
            defaultFont = null;
        }
        super.dispose();
    }

    public String getText(Object obj)
    {
        String text = null;
        if (obj instanceof ILabelProvider) {
            text = ((ILabelProvider)obj).getText(obj);
        } else if (obj instanceof DBSObject) {
            text = ((DBSObject) obj).getName();
        } else if (obj instanceof DBMNode) {
            text = ((DBMNode) obj).getNodeName();
        } else {
            text = obj.toString();
        }
        if (text == null) {
            text = "?";
        }
        return text;
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

    public Font getFont(Object element)
    {
        if (defaultFont == null || !isDefaultElement(element)) {
            return normalFont;
        } else {
            return defaultFont;
        }
    }

    public Color getForeground(Object element)
    {
        return null;
    }

    public Color getBackground(Object element)
    {
        return null;
    }

    private boolean isDefaultElement(Object element)
    {
        if (element instanceof DBSObject) {
            DBSObject object = (DBSObject) element;
            DBSEntitySelector activeContainer = DBUtils.queryParentInterface(
                DBSEntitySelector.class, object);
            if (activeContainer != null) {
                try {
                    // Check child with null monitor
                    // Actually this child is already read (it is the object we checking) so there is a
                    // big chance that no additional database roundtrips are needed
                    return activeContainer.getActiveChild(VoidProgressMonitor.INSTANCE) == object;
                } catch (DBException e) {
                    log.error("Can't check active object", e);
                }
            }
        }
        return false;
    }
}
