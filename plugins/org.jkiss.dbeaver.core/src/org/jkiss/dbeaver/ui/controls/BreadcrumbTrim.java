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
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractPageListener;
import org.jkiss.dbeaver.ui.controls.breadcrumb.BreadcrumbViewer;
import org.jkiss.dbeaver.ui.editors.ILazyEditorInput;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.utils.ArrayUtils;

import java.util.function.Consumer;

public class BreadcrumbTrim {
    private static final Log log = Log.getLog(BreadcrumbTrim.class);

    @PostConstruct
    public void createControls(Composite parent) {
        var breadcrumb = new BreadcrumbViewer(parent) {
            @Override
            protected void configureDropDownViewer(@NotNull TreeViewer viewer, @NotNull Object input) {
                viewer.setContentProvider(new BreadcrumbNodeContentProvider(true));
                viewer.setLabelProvider(new BreadcrumbNodeLabelProvider());
            }
        };
        breadcrumb.setLabelProvider(new BreadcrumbNodeLabelProvider());
        breadcrumb.setContentProvider(new BreadcrumbNodeContentProvider(false));
        breadcrumb.addOpenListener(e -> openEditor(e.getSelection()));
        breadcrumb.addDoubleClickListener(e -> openEditor(e.getSelection()));

        installListeners(breadcrumb);
    }

    private static void openEditor(@NotNull ISelection selection) {
        if (selection instanceof IStructuredSelection ss && ss.getFirstElement() instanceof DBNNode node) {
            DBWorkbench.getPlatformUI().openEntityEditor(node, null);
        }
    }

    private static void installListeners(@NotNull BreadcrumbViewer viewer) {
        var propertyListener = new IPropertyListener() {
            @Override
            public void propertyChanged(Object source, int propId) {
                if (propId == IEditorPart.PROP_INPUT && source instanceof IEditorPart editorPart) {
                    setInput(viewer, editorPart.getEditorInput());
                }
            }
        };

        var partListener = new AbstractPartListener() {
            IEditorPart lastEditorPart;

            @Override
            public void partActivated(IWorkbenchPart part) {
                if (part instanceof IEditorPart editorPart) {
                    setLastEditorPart(editorPart);
                }
            }

            @Override
            public void partDeactivated(IWorkbenchPart part) {
                if (part instanceof IEditorPart editorPart) {
                    setLastEditorPart(editorPart);
                }
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if (part instanceof IEditorPart && part == lastEditorPart) {
                    setLastEditorPart(null);
                }
            }

            private void setLastEditorPart(@Nullable IEditorPart part) {
                if (lastEditorPart != null) {
                    lastEditorPart.removePropertyListener(propertyListener);
                    lastEditorPart = null;
                    viewer.setInput(null);
                }
                if (part != null) {
                    lastEditorPart = part;
                    lastEditorPart.addPropertyListener(propertyListener);
                    setInput(viewer, part.getEditorInput());
                }
            }
        };

        var pageListener = new AbstractPageListener() {
            @Override
            public void pageOpened(IWorkbenchPage page) {
                page.addPartListener(partListener);
            }

            @Override
            public void pageClosed(IWorkbenchPage page) {
                page.removePartListener(partListener);
            }
        };

        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        window.addPageListener(pageListener);

        for (IWorkbenchPage page : window.getPages()) {
            page.addPartListener(partListener);
        }
    }

    private static void setInput(@NotNull BreadcrumbViewer viewer, @NotNull IEditorInput input) {
        if (!tryExtractNode(input, viewer::setInput)) {
            viewer.setInput(null);
        }
    }

    private static boolean tryExtractNode(@NotNull IEditorInput input, @NotNull Consumer<? super DBNNode> consumer) {
        if (input instanceof ILazyEditorInput lazyEditorInput && input instanceof DBPDataSourceContainerProvider provider) {
            DBPProject project = lazyEditorInput.getProject();
            if (!project.isOpen() || !project.isRegistryLoaded()) {
                return false;
            }
            DBNModel navigatorModel = project.getNavigatorModel();
            if (navigatorModel != null) {
                DBNDatabaseNode node = navigatorModel.findNode(provider.getDataSourceContainer());
                if (node != null) {
                    consumer.accept(node);
                    return true;
                }
            }
        }
        if (input instanceof INavigatorEditorInput navigatorEditorInput) {
            DBNNode node = navigatorEditorInput.getNavigatorNode();
            if (node != null) {
                consumer.accept(node);
                return true;
            }
        }
        return false;
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

    private record BreadcrumbNodeContentProvider(boolean allowFoldersOnly) implements ITreeContentProvider {
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
            if (!allowFoldersOnly || element instanceof DBNLocalFolder) {
                return !ArrayUtils.isEmpty(getCachedChildren((DBNNode) element));
            } else {
                return false;
            }
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
