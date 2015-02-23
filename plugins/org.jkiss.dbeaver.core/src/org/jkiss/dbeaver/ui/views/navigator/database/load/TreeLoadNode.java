/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.navigator.database.load;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverIcons;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.HashMap;
import java.util.Map;


public class TreeLoadNode implements ILabelProvider {

    private static final Map<Object, Object> loadingFiles = new HashMap<Object, Object>();
    private static final Map<Object, Object> placeHolders = new HashMap<Object, Object>();

    public static final Object LOADING_FAMILY = new Object();

    private DBNNode parent;
    private String text;
    private String text1;
    private String text2;
    private String text3;
    private int count = 0;
    private boolean disposed = false;
    private Image imgLoading1, imgLoading2, imgLoading3, imgLoading4;

    public static synchronized TreeLoadNode createPlaceHolder(DBNNode parent)
    {
        TreeLoadNode node = null;
        if (!placeHolders.containsKey(parent)) {
            placeHolders.put(parent, node = new TreeLoadNode(parent));
        }
        return node;
    }

    private TreeLoadNode(DBNNode parent)
    {
        this.parent = parent;
        text = "Loading";
        text1 = text + "."; //$NON-NLS-1$
        text2 = text + ".."; //$NON-NLS-1$
        text3 = text + "..."; //$NON-NLS-1$
        imgLoading1 = DBeaverIcons.getImage(DBIcon.LOADING1);
        imgLoading2 = DBeaverIcons.getImage(DBIcon.LOADING2);
        imgLoading3 = DBeaverIcons.getImage(DBIcon.LOADING3);
        imgLoading4 = DBeaverIcons.getImage(DBIcon.LOADING4);
    }

    public DBNNode getParent()
    {
        return parent;
    }

    @Override
    public String getText(Object element)
    {
        switch (count % 4) {
            case 0: return text;
            case 1: return text1;
            case 2: return text2;
            case 3:
            default: return text3;
        }
    }

    @Override
    public Image getImage(Object element)
    {
        switch (count = (++count % 4)) {
            case 0: return imgLoading1;
            case 1: return imgLoading2;
            case 2: return imgLoading3;
            case 3:
            default: return imgLoading4;
        }
    }

    public boolean isDisposed()
    {
        return disposed;
    }

    public void dispose(Object parent)
    {
        disposed = true;

        loadingFiles.remove(parent);
        placeHolders.remove(parent);
    }

    public static synchronized boolean canBeginLoading(Object parent)
    {
        if (!loadingFiles.containsKey(parent)) {
            loadingFiles.put(parent, null);
            return true;
        }
        return false;
    }

    public static boolean isLoading()
    {
        synchronized (loadingFiles) {
            return !loadingFiles.isEmpty();
        }
    }

    @Override
    public void addListener(ILabelProviderListener listener)
    {
    }

    @Override
    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener)
    {
    }

    @Override
    public void dispose()
    {
    }

}