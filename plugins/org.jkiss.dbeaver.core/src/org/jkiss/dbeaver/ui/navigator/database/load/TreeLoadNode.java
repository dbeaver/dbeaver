/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIIcon;

import java.util.HashMap;
import java.util.Map;


public class TreeLoadNode implements ILabelProvider {

    private static final Map<Object, Object> loadingFiles = new HashMap<>();
    private static final Map<Object, Object> placeHolders = new HashMap<>();

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
        imgLoading1 = DBeaverIcons.getImage(UIIcon.LOADING1);
        imgLoading2 = DBeaverIcons.getImage(UIIcon.LOADING2);
        imgLoading3 = DBeaverIcons.getImage(UIIcon.LOADING3);
        imgLoading4 = DBeaverIcons.getImage(UIIcon.LOADING4);
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