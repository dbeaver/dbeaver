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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureContainerActive;
import org.jkiss.dbeaver.model.struct.DBSUtils;

/**
 * NavigatorTreeLabelProvider
*/
class NavigatorTreeLabelProvider extends LabelProvider implements IFontProvider, IColorProvider
{
    static Log log = LogFactory.getLog(NavigatorTreeLabelProvider.class);

    private NavigatorTreeView view;
    private Font normalFont;
    private Font defaultFont;

    NavigatorTreeLabelProvider(NavigatorTreeView view)
    {
        this.view = view;
        this.normalFont = view.getViewer().getControl().getFont();
        FontData[] fontData = this.normalFont.getFontData();
        if (fontData.length > 0) {
            fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
            this.defaultFont = new Font(normalFont.getDevice(), fontData[0]);
        }
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
            DBSStructureContainerActive activeContainer = DBSUtils.queryParentInterface(
                DBSStructureContainerActive.class, object);
            if (activeContainer != null) {
                try {
                    return activeContainer.getActiveChild() == object;
                } catch (DBException e) {
                    log.error("Can't check active object", e);
                }
            }
        }
        return false;
    }
}
