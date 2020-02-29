/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPRegistryListener;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.navigator.INavigatorListener;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractPageListener;
import org.jkiss.dbeaver.ui.actions.DataSourcePropertyTester;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSourceToolbarHandler implements DBPRegistryListener, DBPEventListener, INavigatorListener {

    private final List<DBPDataSourceRegistry> handledRegistries = new ArrayList<>();
    private final IWorkbenchWindow workbenchWindow;
    private IWorkbenchPart activePart;
    private IPageListener pageListener;
    private IPartListener partListener;

    public DataSourceToolbarHandler(IWorkbenchWindow workbenchWindow) {
        this.workbenchWindow = workbenchWindow;
        DBWorkbench.getPlatform().getNavigatorModel().addListener(this);

        final ISelectionListener selectionListener = (part, selection) -> {
            if (part == activePart && part instanceof IEditorPart && selection instanceof IStructuredSelection) {
                final Object element = ((IStructuredSelection) selection).getFirstElement();
                if (element != null) {
                    if (RuntimeUtils.getObjectAdapter(element, DBSObject.class) != null) {
                        updateToolbar();
                    }
                }
            }
        };
        pageListener = new AbstractPageListener() {
            @Override
            public void pageClosed(IWorkbenchPage page) {
                page.removePartListener(partListener);
                page.removeSelectionListener(selectionListener);
            }

            @Override
            public void pageOpened(IWorkbenchPage page) {
                page.addPartListener(partListener);
                page.addSelectionListener(selectionListener);
            }
        };
        partListener = new AbstractPartListener() {
            @Override
            public void partActivated(IWorkbenchPart part) {
                setActivePart(part);
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if (part == activePart) {
                    setActivePart(null);
                }
            }
        };

        workbenchWindow.addPageListener(pageListener);
        for (IWorkbenchPage page : workbenchWindow.getPages()) {
            pageListener.pageOpened(page);
        }

        // Register as datasource listener in all datasources
        // We need it because at this moment there could be come already loaded registries (on startup)
        DataSourceProviderRegistry.getInstance().addDataSourceRegistryListener(this);
        for (DBPDataSourceRegistry registry : DBUtils.getAllRegistries(false)) {
            handleRegistryLoad(registry);
        }
        // We'll miss a lot of DBP events because  we'll be activated only after UI will be instantiated
        // So we need to update toolbar explicitly right after UI will initialize
        UIUtils.asyncExec(this::updateToolbar);
    }

    public void dispose() {
        DataSourceProviderRegistry.getInstance().removeDataSourceRegistryListener(this);

        for (DBPDataSourceRegistry registry : this.handledRegistries) {
            registry.removeDataSourceListener(this);
        }
        handledRegistries.clear();

        DBWorkbench.getPlatform().getNavigatorModel().removeListener(this);

        if (this.pageListener != null) {
            this.workbenchWindow.removePageListener(this.pageListener);
            this.pageListener = null;
        }
    }

    public void setActivePart(@Nullable IWorkbenchPart part) {
        activePart = part;
        if (activePart instanceof IEditorPart) {
            updateToolbar();
        }
    }

    @Override
    public void handleDataSourceEvent(final DBPEvent event) {
        if (workbenchWindow.getWorkbench().isClosing()) {
            return;
        }
        DBPDataSourceContainer currentDataSource = DataSourceToolbarUtils.getCurrentDataSource(workbenchWindow);

        if ((event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getObject() == currentDataSource) ||
            (event.getAction() == DBPEvent.Action.OBJECT_SELECT && Boolean.TRUE.equals(event.getEnabled()) &&
                DBUtils.getContainer(event.getObject()) == currentDataSource)
            ) {
            UIUtils.asyncExec(
                this::updateToolbar
            );
        }
        // This is a hack. We need to update main toolbar. By design toolbar should be updated along with command state
        // but in fact it doesn't. I don't know better way than trigger update explicitly.
        // TODO: replace with something smarter
        if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && event.getEnabled() != null) {
            DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_CONNECTED);
            DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
            UIUtils.asyncExec(
                () -> {
                    if (workbenchWindow instanceof WorkbenchWindow) {
                        ((WorkbenchWindow) workbenchWindow).updateActionBars();
                    }
                }
            );
        }

    }

    private void updateToolbar() {
        DataSourceToolbarUtils.refreshSelectorToolbar(workbenchWindow);
    }

    @Override
    public void handleRegistryLoad(DBPDataSourceRegistry registry) {
        registry.addDataSourceListener(this);
        handledRegistries.add(registry);
    }

    @Override
    public void handleRegistryUnload(DBPDataSourceRegistry registry) {
        handledRegistries.remove(registry);
        registry.removeDataSourceListener(this);
    }

    @Override
    public void nodeChanged(DBNEvent event) {
        final DBNNode node = event.getNode();
        if (!(node instanceof DBNResource)) {
            return;
        }

        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage == null) {
            return;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor == null) {
            return;
        }
        IFile activeFile = EditorUtils.getFileFromInput(activeEditor.getEditorInput());
        if (activeFile == null) {
            return;
        }
        if (activeFile.equals(((DBNResource) node).getResource())) {
            //DBPDataSourceContainer visibleContainer = DataSourceToolbarUtils.getCurrentDataSource(workbenchWindow);
            //DBPDataSourceContainer newContainer = EditorUtils.getFileDataSource(activeFile);
            updateToolbar();
        }
    }

}