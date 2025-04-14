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
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainerProvider;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractPageListener;
import org.jkiss.dbeaver.ui.controls.breadcrumb.BreadcrumbViewer;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorPreferences.BreadcrumbLocation;
import org.jkiss.dbeaver.ui.editors.ILazyEditorInput;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.dbeaver.ui.navigator.breadcrumb.NodeBreadcrumbViewer;

import java.util.function.Consumer;

public class BreadcrumbTrim {
    private static final Log log = Log.getLog(BreadcrumbTrim.class);

    private static final String BREADCRUMBS_ID = "org.jkiss.dbeaver.core.ui.Breadcrumb"; //$NON-NLS-1$
    private static final String BOTTOM_TRIM_ID = "org.eclipse.ui.trim.status"; //$NON-NLS-1$

    @PostConstruct
    public void createControls(Composite parent) {
        var viewer = new NodeBreadcrumbViewer(parent, SWT.BOTTOM);

        installListeners(viewer);
        UIUtils.asyncExec(BreadcrumbTrim::updateElementVisibility);
    }

    private static void updateElementVisibility() {
        for (IWorkbenchWindow window : Workbench.getInstance().getWorkbenchWindows()) {
            if (window instanceof WorkbenchWindow workbenchWindow) {
                updateElementVisibility(workbenchWindow);
            }
        }
    }

    private static void updateElementVisibility(@NotNull WorkbenchWindow window) {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        var model = window.getModel();
        var modelService = window.getService(EModelService.class);

        boolean dirty = false;

        var breadcrumbsElement = modelService.find(BREADCRUMBS_ID, model);
        var breadcrumbsVisible = BreadcrumbLocation.get(store) == BreadcrumbLocation.IN_STATUS_BAR;
        if (breadcrumbsElement != null && breadcrumbsElement.isToBeRendered() != breadcrumbsVisible) {
            breadcrumbsElement.setToBeRendered(breadcrumbsVisible);
            dirty = true;
        }

        var statusLineElement = modelService.find(WorkbenchWindow.STATUS_LINE_ID, model);
        var statusLineVisible = store.getBoolean(DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE);
        if (statusLineElement != null && statusLineElement.isToBeRendered() != statusLineVisible) {
            statusLineElement.setToBeRendered(statusLineVisible);
            dirty = true;
        }

        if (dirty) {
            MUIElement element = modelService.find(BOTTOM_TRIM_ID, model);
            if (element != null && element.getWidget() instanceof Composite composite) {
                composite.layout(true, true);
            }
        }
    }

    private static void installListeners(@NotNull BreadcrumbViewer viewer) {
        var propertyListener = new IPropertyListener() {
            @Override
            public void propertyChanged(Object source, int propId) {
                if (propId != IEditorPart.PROP_INPUT && propId != IEditorPart.PROP_DIRTY) {
                    return;
                }
                if (source instanceof IEditorPart editorPart) {
                    setInput(viewer, editorPart.getEditorInput());
                }
            }
        };

        var partListener = new AbstractPartListener() {
            IEditorPart lastEditorPart;

            @Override
            public void partActivated(IWorkbenchPart part) {
                if (part instanceof IEditorPart editorPart) {
                    UIExecutionQueue.queueExec(() -> setLastEditorPart(editorPart));
                }
            }

            @Override
            public void partDeactivated(IWorkbenchPart part) {
                if (part instanceof IEditorPart editorPart) {
                    UIExecutionQueue.queueExec(() -> setLastEditorPart(editorPart));
                }
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if (part instanceof IEditorPart && part == lastEditorPart) {
                    setLastEditorPart(null);
                }
            }

            private void setLastEditorPart(@Nullable IEditorPart part) {
                if (lastEditorPart == part || viewer.getControl() == null || viewer.getControl().isDisposed()) {
                    return;
                }
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

        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(event -> {
            switch (event.getProperty()) {
                case DBeaverPreferences.UI_STATUS_BAR_SHOW_BREADCRUMBS:
                case DBeaverPreferences.UI_STATUS_BAR_SHOW_STATUS_LINE:
                    updateElementVisibility();
                    break;
                default:
                    break;
            }
        });
    }

    private static void setInput(@NotNull BreadcrumbViewer viewer, @NotNull IEditorInput input) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        if (!tryExtractNode(input, viewer::setInput)) {
            viewer.setInput(null);
        }
    }

    private static boolean tryExtractNode(@NotNull IEditorInput input, @NotNull Consumer<? super DBNNode> consumer) {
        if (input instanceof ILazyEditorInput lazyEditorInput && input instanceof DBPDataSourceContainerProvider provider) {
            DBPProject project = lazyEditorInput.getProject();
            if (project == null || !project.isOpen() || !project.isRegistryLoaded()) {
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
}
