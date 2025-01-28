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
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractPageListener;
import org.jkiss.dbeaver.ui.controls.breadcrumb.BreadcrumbViewer;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.ArrayUtils;

public class BreadcrumbTrim {
    private static final Log log = Log.getLog(BreadcrumbTrim.class);

    @PostConstruct
    public void createControls(Composite parent) {
        var breadcrumb = new BreadcrumbViewer(parent) {
            @Override
            protected void configureDropDownViewer(@NotNull TreeViewer viewer, @NotNull Object input) {
                log.debug("configureDropDownViewer");
                viewer.setContentProvider(new BreadcrumbNodeContentProvider(false));
                viewer.setLabelProvider(new BreadcrumbNodeLabelProvider());
            }
        };
        breadcrumb.setLabelProvider(new BreadcrumbNodeLabelProvider());
        breadcrumb.setContentProvider(new BreadcrumbNodeContentProvider(true));
        breadcrumb.addOpenListener(e -> openEditor(((IStructuredSelection) e.getSelection())));
        breadcrumb.addDoubleClickListener(e -> openEditor((IStructuredSelection) e.getSelection()));

        installListeners(breadcrumb);
    }

    private static void openEditor(@NotNull IStructuredSelection selection) {
        NavigatorHandlerObjectOpen.openEntityEditor(
            (DBNNode) selection.getFirstElement(),
            null,
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
        );
    }

    private static void installListeners(@NotNull BreadcrumbViewer viewer) {
        var selectionListener = new ISelectionListener() {
            @Override
            public void selectionChanged(IWorkbenchPart part, ISelection selection) {
                log.debug("selectionChanged(" + part + ", " + selection + ")");
            }
        };

        var partListener = new AbstractPartListener() {
            @Override
            public void partActivated(IWorkbenchPart part) {
                if (part instanceof IEditorPart editorPart && editorPart.getEditorInput() instanceof INavigatorEditorInput input) {
                    viewer.setInput(input.getNavigatorNode());
                } else {
                    viewer.setInput(null);
                }
            }

            @Override
            public void partDeactivated(IWorkbenchPart part) {
                viewer.setInput(null);
            }
        };

        var pageListener = new AbstractPageListener() {
            @Override
            public void pageOpened(IWorkbenchPage page) {
                page.addPartListener(partListener);
                page.addSelectionListener(selectionListener);
            }

            @Override
            public void pageClosed(IWorkbenchPage page) {
                page.removePartListener(partListener);
                page.removeSelectionListener(selectionListener);
            }
        };

        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        window.addPageListener(pageListener);

        for (IWorkbenchPage page : window.getPages()) {
            page.addPartListener(partListener);
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

    private record BreadcrumbNodeContentProvider(boolean allowChildren) implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            DBNNode child = (DBNNode) inputElement;
            DBNNode parent = child.getParentNode();
            if (parent != null) {
                return getChildren(parent);
            }
            return new Object[0];
        }

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
            var children = getCachedChildren((DBNNode) parentElement);
            if (children != null) {
                return children;
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object element) {
            return allowChildren && !ArrayUtils.isEmpty(getCachedChildren((DBNNode) element));
        }

        @Nullable
        private static DBNNode[] getCachedChildren(@NotNull DBNNode parent) {
            try {
                return parent.getChildren(new LocalCacheProgressMonitor(new VoidProgressMonitor()));
            } catch (DBException e) {
                log.error("Error getting children", e); // should not happen
                return null;
            }
        }
    }
}
