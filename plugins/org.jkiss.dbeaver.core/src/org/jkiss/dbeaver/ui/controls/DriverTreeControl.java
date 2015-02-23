/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;

import java.util.List;

/**
 * DriverTreeControl
 *
 * @author Serge Rieder
 */
public class DriverTreeControl extends FilteredTree {

    private static final String DRIVER_INIT_DATA = "driverInitData";

    public DriverTreeControl(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        super(
            saveInitParameters(parent, site, providers, expandRecent),
            SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER,
            new DriverFilter(),
            true);
    }

    private static Composite saveInitParameters(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        parent.setData("driverInitData", new Object[] {site, providers, expandRecent} );
        return parent;
    }

    @Override
    protected DriverTreeViewer doCreateTreeViewer(Composite parent, int style) {
        Object[] initData = (Object[]) getParent().getData(DRIVER_INIT_DATA);
        parent.setData(DRIVER_INIT_DATA, null);
        DriverTreeViewer viewer = new DriverTreeViewer(parent, style);
        viewer.initDrivers(initData[0], (List<DataSourceProviderDescriptor>) initData[1], (Boolean) initData[2]);
        return viewer;
    }

    private static class DriverFilter extends PatternFilter {
        @Override
        public boolean isElementVisible(Viewer viewer, Object element) {
            Object parent = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
                .getContentProvider()).getParent(element);
            if (parent != null && isLeafMatch(viewer, parent)) {
                return true;
            }
            return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
        }
    }
}
