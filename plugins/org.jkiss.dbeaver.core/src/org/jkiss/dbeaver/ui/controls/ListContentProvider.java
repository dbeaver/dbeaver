/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.Arrays;
import java.util.Collection;

/**
 * ListContentProvider
 */
public class ListContentProvider implements IStructuredContentProvider {

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }

    public Object[] getElements(Object inputElement)
    {
        if (inputElement instanceof Collection) {
            return ((Collection<?>)inputElement).toArray();
        } else if (inputElement.getClass().isArray()) {
            if (inputElement.getClass().getComponentType().isPrimitive()) {
                return null;
            } else {
                return (Object[])inputElement;
            }
        }
        return null;
    }

}
