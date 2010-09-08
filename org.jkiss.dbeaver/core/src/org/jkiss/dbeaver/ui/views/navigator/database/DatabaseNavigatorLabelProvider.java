/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DatabaseNavigatorLabelProvider
*/
class DatabaseNavigatorLabelProvider extends LabelProvider implements IFontProvider, IColorProvider
{
    static final Log log = LogFactory.getLog(DatabaseNavigatorLabelProvider.class);

    private DatabaseNavigatorView view;
    private Font normalFont;
    private Font defaultFont;
    private Color lockedForeground;

    DatabaseNavigatorLabelProvider(DatabaseNavigatorView view)
    {
        this.view = view;
        this.normalFont = view.getNavigatorViewer().getControl().getFont();
        this.defaultFont = UIUtils.makeBoldFont(normalFont);
        this.lockedForeground = view.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
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
        String text;
        if (obj instanceof ILabelProvider) {
            text = ((ILabelProvider)obj).getText(obj);
/*
        } else if (obj instanceof DBSObject) {
            text = ((DBSObject) obj).getName();
*/
        } else if (obj instanceof DBNNode) {
            text = ((DBNNode) obj).getNodeName();
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
        if (obj instanceof DBNNode) {
            return ((DBNNode)obj).getNodeIconDefault();
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
        if (element instanceof DBNNode && ((DBNNode)element).isLocked()) {
            return lockedForeground;
        }
        return null;
    }

    public Color getBackground(Object element)
    {
        return null;
    }

    private boolean isDefaultElement(Object element)
    {
        if (element instanceof DBNNode) {
            DBSObject object = ((DBNNode) element).getObject();
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
