/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * TreeContentProvider
 */
public class TreeContentProvider extends ListContentProvider implements ITreeContentProvider {

    public Object[] getChildren(Object parentElement)
    {
        return null;
    }

    public Object getParent(Object element)
    {
        return null;
    }

    public boolean hasChildren(Object element)
    {
        return false;
    }

}
