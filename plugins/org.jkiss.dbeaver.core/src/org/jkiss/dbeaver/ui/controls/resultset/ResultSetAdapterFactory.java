/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.dialogs.IPageChangeProvider;

/**
 * ResultSetAdapterFactory
 */
public class ResultSetAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { ResultSetViewer.class };

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == ResultSetViewer.class) {
            if (adaptableObject instanceof ResultSetViewer) {
                return adaptableObject;
            } else if (adaptableObject instanceof IResultSetContainer) {
                return ((IResultSetContainer) adaptableObject).getResultSetController();
            }
            if (adaptableObject instanceof IPageChangeProvider) {
                return getAdapter(((IPageChangeProvider) adaptableObject).getSelectedPage(), ResultSetViewer.class);
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}