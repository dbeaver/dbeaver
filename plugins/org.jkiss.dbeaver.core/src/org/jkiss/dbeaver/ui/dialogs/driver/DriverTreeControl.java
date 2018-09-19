/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
 * DriverTreeControl
 *
 * @author Serge Rider
 */
public class DriverTreeControl extends FilteredTree {

    private static final String DRIVER_INIT_DATA = "driverInitData";

    public DriverTreeControl(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        super(
            saveInitParameters(parent, site, providers, expandRecent),
            SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER,
            new DriverFilter(),
            true);
        setInitialText(CoreMessages.dialog_connection_driver_treecontrol_initialText);
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
        UIUtils.asyncExec(() ->
            viewer.initDrivers(initData[0], (List<DataSourceProviderDescriptor>) initData[1], (Boolean) initData[2]));
        return viewer;
    }

    private static class DriverFilter extends PatternFilter {
        public DriverFilter() {
            setIncludeLeadingWildcard(true);
        }

        @Override
        public boolean isElementVisible(Viewer viewer, Object element) {
            Object parent = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
                .getContentProvider()).getParent(element);
            if (parent != null && isLeafMatch(viewer, parent)) {
                return true;
            }
            return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DriverDescriptor) {
                return wordMatches(((DriverDescriptor) element).getName()) ||
                    wordMatches(((DriverDescriptor) element).getDescription());
            }
            return super.isLeafMatch(viewer, element);
        }

    }
}
