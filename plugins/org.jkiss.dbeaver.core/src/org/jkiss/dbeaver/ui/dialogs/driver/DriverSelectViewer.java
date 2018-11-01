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

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

/**
 * DriverSelectViewer
 *
 * @author Serge Rider
 */
public class DriverSelectViewer extends Viewer {

    private FilteredTree driverTree;

    public DriverSelectViewer(Composite parent, Object site, List<DataSourceProviderDescriptor> providers, boolean expandRecent) {
        driverTree = new FilteredTree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, new DriverFilter(), true) {
            @Override
            protected DriverTreeViewer doCreateTreeViewer(Composite parent, int style) {
                DriverTreeViewer viewer = new DriverTreeViewer(parent, style);
                UIUtils.asyncExec(() ->
                    viewer.initDrivers(site, providers, expandRecent));
                return viewer;
            }
        };
        driverTree.setInitialText(CoreMessages.dialog_connection_driver_treecontrol_initialText);
    }

    public Control getControl() {
        return driverTree;
    }

    @Override
    public Object getInput() {
        return driverTree.getViewer().getInput();
    }

    @Override
    public ISelection getSelection() {
        return driverTree.getViewer().getSelection();
    }

    @Override
    public void refresh() {
        driverTree.getViewer().refresh();
    }

    public void refresh(DBPDriver driver) {
        driverTree.getViewer().refresh(driver);
    }
    @Override
    public void setInput(Object input) {
        driverTree.getViewer().setInput(input);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        driverTree.getViewer().setSelection(selection, reveal);
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
