/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import jakarta.annotation.PostConstruct;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.Workbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.breadcrumb.BreadcrumbViewer;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class BreadcrumbTrim {
    @PostConstruct
    public void createControls(Composite parent) {
        var breadcrumb = new BreadcrumbViewer(parent);
        breadcrumb.setLabelProvider(new BreadcrumbNodeLabelProvider());
        breadcrumb.setContentProvider(new BreadcrumbNodeContentProvider());
        breadcrumb.addMenuDetectListener(e -> {
            IWorkbenchWindow window = Workbench.getInstance().getActiveWorkbenchWindow();
            IWorkbenchPart part = window.getActivePage().getActivePart();

            MenuManager manager = new MenuManager();
            NavigatorUtils.addStandardMenuItem(part.getSite(), manager, breadcrumb);
            part.getSite().registerContextMenu(manager, breadcrumb);

            Menu menu = manager.createContextMenu(breadcrumb.getControl());
            menu.setLocation(e.x + 10, e.y + 10);
            menu.setVisible(true);
        });

        installListeners(breadcrumb);
    }

    private static void installListeners(@NotNull BreadcrumbViewer viewer) {
        var activeWindow = UIUtils.getActiveWorkbenchWindow();
        var activePage = activeWindow.getActivePage();
        if (activePage != null) {
            var listener = new AbstractPartListener() {
                private final ISelectionChangedListener listener = event -> {
                    if (event.getStructuredSelection().getFirstElement() instanceof DBNDatabaseNode node) {
                        viewer.setInput(node);
                    }
                };

                @Override
                public void partActivated(IWorkbenchPart part) {
                    if (part instanceof IEditorPart ep && ep.getEditorInput() instanceof INavigatorEditorInput input) {
                        viewer.setInput(input.getNavigatorNode());
                    } else if (part instanceof INavigatorModelView view) {
                        Viewer viewer = view.getNavigatorViewer();
                        if (viewer != null) {
                            viewer.addSelectionChangedListener(listener);
                        }
                    }
                }

                @Override
                public void partDeactivated(IWorkbenchPart part) {
                    if (part instanceof INavigatorModelView view) {
                        Viewer viewer = view.getNavigatorViewer();
                        if (viewer != null) {
                            viewer.removeSelectionChangedListener(listener);
                        }
                    }
                }
            };

            activePage.addPartListener(listener);
            viewer.getControl().addDisposeListener(e -> activePage.removePartListener(listener));
        }
    }

    private static class BreadcrumbNodeLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element) {
            return DBeaverIcons.getImage(((DBNNode) element).getNodeIconDefault());
        }

        @Override
        public String getText(Object element) {
            return ((DBNNode) element).getNodeDisplayName();
        }
    }

    private static class BreadcrumbNodeContentProvider extends TreeContentProvider {
        @Override
        public Object getParent(Object element) {
            DBNNode child = (DBNNode) element;
            if (child instanceof DBNDataSource) {
                return null; // don't show anything below data sources
            }

            DBNNode parent = child.getParentNode();
            while (parent instanceof DBNDatabaseFolder) {
                parent = parent.getParentNode(); // skip folder nodes
            }

            return parent;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasChildren(Object element) {
            return ((DBNNode) element).hasChildren(true);
        }
    }
}
