/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.tree;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverIcons;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.HashMap;
import java.util.Map;


class TreeLoadNode implements ILabelProvider {

    private static final Map<Object, Object> loadingFiles = new HashMap<Object, Object>();
    private static final Map<Object, Object> placeHolders = new HashMap<Object, Object>();

    public static final Object LOADING_FAMILY = new Object();

    private String text;
    private String text1;
    private String text2;
    private String text3;
    private int count = 0;
    private boolean disposed = false;
    private Image imgLoading1, imgLoading2, imgLoading3, imgLoading4;

    public static synchronized TreeLoadNode createPlaceHolder(Object parent)
    {
        TreeLoadNode node = null;
        if (!placeHolders.containsKey(parent)) {
            placeHolders.put(parent, node = new TreeLoadNode());
        }
        return node;
    }

    private TreeLoadNode()
    {
        text = "Loading";
        text1 = text + "."; //$NON-NLS-1$
        text2 = text + ".."; //$NON-NLS-1$
        text3 = text + "..."; //$NON-NLS-1$
        imgLoading1 = DBeaverIcons.getImage(DBIcon.LOADING1);
        imgLoading2 = DBeaverIcons.getImage(DBIcon.LOADING2);
        imgLoading3 = DBeaverIcons.getImage(DBIcon.LOADING3);
        imgLoading4 = DBeaverIcons.getImage(DBIcon.LOADING4);
    }

    public String getText(Object element)
    {
        switch (count % 4) {
            case 0:
                return text;
            case 1:
                return text1;
            case 2:
                return text2;
            case 3:
            default:
                return text3;
        }
    }

    public Image getImage(Object element)
    {
        switch (count = (++count % 4)) {
            case 0:
                return imgLoading1;
            case 1:
                return imgLoading2;
            case 2:
                return imgLoading3;
            case 3:
            default:
                return imgLoading4;
        }
    }

    public boolean isDisposed()
    {
        return disposed;
    }

    public void dispose(Object parent)
    {
        disposed = true;

/*
        imgLoading1.dispose();
        imgLoading2.dispose();
        imgLoading3.dispose();
        imgLoading4.dispose();
*/

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

    public void addListener(ILabelProviderListener listener)
    {
    }

    public boolean isLabelProperty(Object element, String property)
    {
        return false;
    }

    public void removeListener(ILabelProviderListener listener)
    {
    }

    public void dispose()
    {
    }

}