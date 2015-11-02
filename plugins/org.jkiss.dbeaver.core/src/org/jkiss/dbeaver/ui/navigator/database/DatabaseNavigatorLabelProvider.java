/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DatabaseNavigatorLabelProvider
*/
class DatabaseNavigatorLabelProvider extends LabelProvider implements IFontProvider, IColorProvider
{
    private Font normalFont;
    private Font boldFont;
    //private Font italicFont;
    //private Font boldItalicFont;
    private Color lockedForeground;
    private Color transientForeground;

    DatabaseNavigatorLabelProvider(Viewer viewer)
    {
        //this.view = view;
        this.normalFont = viewer.getControl().getFont();
        this.boldFont = UIUtils.makeBoldFont(normalFont);
        //this.italicFont = UIUtils.modifyFont(normalFont, SWT.ITALIC);
        //this.boldItalicFont = UIUtils.modifyFont(normalFont, SWT.BOLD | SWT.ITALIC);
        this.lockedForeground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
        this.transientForeground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(boldFont);
        boldFont = null;
//        UIUtils.dispose(italicFont);
//        italicFont = null;
//        UIUtils.dispose(boldItalicFont);
//        boldItalicFont = null;
        super.dispose();
    }

    @Override
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
        if (isFilteredElement(obj)) {
            text += " (...)";
        }
        return text;
    }

    @Override
    public Image getImage(Object obj)
    {
        if (obj instanceof ILabelProvider) {
            return ((ILabelProvider)obj).getImage(obj);
        }
        if (obj instanceof DBNNode) {
            return DBeaverIcons.getImage(((DBNNode) obj).getNodeIconDefault());
        } else {
            return null;
        }
    }

    @Override
    public Font getFont(Object element)
    {
        if (boldFont == null || !NavigatorUtils.isDefaultElement(element)) {
            return normalFont;
        } else {
            return boldFont;
        }
    }

    @Override
    public Color getForeground(Object element)
    {
        if (element instanceof DBNNode) {
            DBNNode node = (DBNNode)element;
            if (node.isLocked()) {
                return lockedForeground;
            }
            if (node instanceof DBSWrapper && ((DBSWrapper)node).getObject() != null && !((DBSWrapper)node).getObject().isPersisted()) {
                return transientForeground;
            }
        }
        return null;
    }

    @Override
    public Color getBackground(Object element)
    {
        if (element instanceof DBNDataSource) {
            DBPDataSourceContainer ds = ((DBNDatabaseNode) element).getDataSourceContainer();
            if (ds != null) {
                return UIUtils.getConnectionColor(ds.getConnectionConfiguration());
            }
        }
        return null;
    }

    private boolean isFilteredElement(Object element)
    {
        return element instanceof DBNNode && ((DBNNode) element).isFiltered();
    }

}
