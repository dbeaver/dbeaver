/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * TreeContentProvider
 */
public class TreeContentProvider extends ListContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getChildren(Object parentElement)
    {
        return null;
    }

    @Override
    public Object getParent(Object element)
    {
        return null;
    }

    @Override
    public boolean hasChildren(Object element)
    {
        return false;
    }

}
