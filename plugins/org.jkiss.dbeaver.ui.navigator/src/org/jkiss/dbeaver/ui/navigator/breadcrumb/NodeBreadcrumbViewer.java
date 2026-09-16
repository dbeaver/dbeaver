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
package org.jkiss.dbeaver.ui.navigator.breadcrumb;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.breadcrumb.BreadcrumbViewer;
import org.jkiss.dbeaver.ui.navigator.NavigatorPropertyTester;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link DBNNode}-oriented specialization of {@link BreadcrumbViewer}.
 */
public class NodeBreadcrumbViewer extends BreadcrumbViewer {
    private static final Log log = Log.getLog(NodeBreadcrumbViewer.class);
    private static final String CONTEXT_MENU_ID = "entityBreadcrumbsMenu"; //$NON-NLS-1$

    private final Map<IWorkbenchSite, MenuManager> contextMenuManagers = new IdentityHashMap<>();
    private IWorkbenchSite contextMenuSite;
    private Supplier<? extends IWorkbenchPartSite> selectionSiteSupplier;
    private IWorkbenchPartSite selectionSite;
    private ISelectionProvider savedSelectionProvider;
    private IWorkbenchPartSite navigatorContextSite;

    public NodeBreadcrumbViewer(@NotNull Composite parent, int style) {
        super(parent, style);

        setLabelProvider(new BreadcrumbNodeLabelProvider());
        setContentProvider(new BreadcrumbNodeContentProvider(false));
        setDropDownContentProvider(new BreadcrumbNodeContentProvider(true));

        addOpenListener(e -> openEditor(e.getSelection()));
        addDoubleClickListener(e -> openEditor(e.getSelection()));
    }

    public void setContextMenuSite(@Nullable IWorkbenchSite site) {
        if (contextMenuSite == site) {
            return;
        }
        contextMenuSite = site;
        setContextMenu(null);
        if (site == null) {
            getControl().setMenu(null);
            return;
        }

        MenuManager manager = contextMenuManagers.computeIfAbsent(site, this::createContextMenu);
        getControl().setMenu(manager.getMenu());
        setContextMenu(manager.getMenu());
    }

    public void setSelectionSiteSupplier(@Nullable Supplier<? extends IWorkbenchPartSite> selectionSiteSupplier) {
        this.selectionSiteSupplier = selectionSiteSupplier;
    }

    public void disposeContextMenuSite(@NotNull IWorkbenchSite site) {
        MenuManager manager = contextMenuManagers.remove(site);
        if (manager != null) {
            if (contextMenuSite == site) {
                restoreSelectionProvider();
                contextMenuSite = null;
                setContextMenu(null);
                if (!getControl().isDisposed()) {
                    getControl().setMenu(null);
                }
            }
            manager.dispose();
        }
    }

    @Override
    protected void handleDispose(DisposeEvent event) {
        restoreSelectionProvider();
        setContextMenu(null);
        for (MenuManager manager : contextMenuManagers.values()) {
            manager.dispose();
        }
        contextMenuManagers.clear();
        super.handleDispose(event);
    }

    @Override
    protected void contextMenuAboutToShow() {
        restoreSelectionProvider();
        if (contextMenuSite instanceof IWorkbenchPartSite contextPartSite) {
            contextPartSite.getPage().activate(contextPartSite.getPart());
            setNavigatorContextSite(contextPartSite);
            selectionSite = selectionSiteSupplier != null ? selectionSiteSupplier.get() : contextPartSite;
            if (selectionSite == null) {
                selectionSite = contextPartSite;
            }
            savedSelectionProvider = selectionSite.getSelectionProvider();
            selectionSite.setSelectionProvider(this);
            fireSelectionChanged(new SelectionChangedEvent(this, getSelection()));
        }
    }

    @NotNull
    private MenuManager createContextMenu(@NotNull IWorkbenchSite site) {
        MenuManager manager = NavigatorUtils.createContextMenu(site, this, this, null);
        if (site instanceof IWorkbenchPartSite partSite) {
            partSite.registerContextMenu(CONTEXT_MENU_ID, manager, this);
        }
        manager.getMenu().addMenuListener(new MenuAdapter() {
            @Override
            public void menuHidden(MenuEvent e) {
                UIUtils.asyncExec(NodeBreadcrumbViewer.this::restoreSelectionProvider);
            }
        });
        return manager;
    }

    private void restoreSelectionProvider() {
        if (selectionSite != null) {
            selectionSite.setSelectionProvider(savedSelectionProvider);
            selectionSite = null;
            savedSelectionProvider = null;
        }
        setNavigatorContextSite(null);
    }

    private void setNavigatorContextSite(@Nullable IWorkbenchPartSite site) {
        IWorkbenchPartSite oldSite = navigatorContextSite;
        navigatorContextSite = site;
        NavigatorPropertyTester.setBreadcrumbContextMenuPart(site != null ? site.getPart() : null);

        IWorkbenchPartSite evaluationSite = site != null ? site : oldSite;
        if (evaluationSite != null) {
            IEvaluationService service = evaluationSite.getService(IEvaluationService.class);
            if (service != null) {
                service.requestEvaluation(NavigatorPropertyTester.NAMESPACE + "." + NavigatorPropertyTester.PROP_FOCUSED);
            }
        }
    }

    private static void openEditor(@NotNull ISelection selection) {
        if (selection instanceof IStructuredSelection ss && ss.getFirstElement() instanceof DBNNode node) {
            DBWorkbench.getPlatformUI().openEntityEditor(node, null);
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
