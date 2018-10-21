/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.ui.controls.IProgressControlProvider;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

/**
 * EntityNodeEditor
 */
class TabbedFolderPageNode extends TabbedFolderPage implements ISearchContextProvider, IRefreshablePart, INavigatorModelView, IAdaptable
{

    private IDatabaseEditor mainEditor;
    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;

    TabbedFolderPageNode(IDatabaseEditor mainEditor, DBNNode node, DBXTreeNode metaNode)
    {
        this.mainEditor = mainEditor;
        this.node = node;
        this.metaNode = metaNode;
    }

    public void setFocus()
    {
        if (itemControl != null) {
            itemControl.setFocus();
        }
    }

    @Override
    public void createControl(Composite parent) {
        itemControl = new ItemListControl(parent, SWT.SHEET, mainEditor.getSite(), node, metaNode);
        //itemControl.getLayout().marginHeight = 0;
        //itemControl.getLayout().marginWidth = 0;
        ProgressPageControl progressControl = null;
        if (mainEditor instanceof IProgressControlProvider) {
            progressControl = ((IProgressControlProvider) mainEditor).getProgressControl();
        }
        if (progressControl != null) {
            itemControl.substituteProgressPanel(progressControl);
        } else {
            itemControl.createProgressPanel();
        }

        parent.layout();

        // Activate items control on focus
        itemControl.getItemsViewer().getControl().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Update selection provider and selection
                final ISelectionProvider selectionProvider = itemControl.getSelectionProvider();
                if (mainEditor.getSite().getSelectionProvider() != selectionProvider) {
                    mainEditor.getSite().setSelectionProvider(selectionProvider);
                    selectionProvider.setSelection(selectionProvider.getSelection());
                }
                itemControl.activate(true);

                // Notify owner MultiPart editor about page change
                // We need it to update search actions and other contributions provided by node editor
                if (mainEditor.getSite() instanceof MultiPageEditorSite) {
                    MultiPageEditorPart multiPageEditor = ((MultiPageEditorSite) mainEditor.getSite()).getMultiPageEditor();
                    if (multiPageEditor.getSelectedPage() != mainEditor) {
                        multiPageEditor.setActiveEditor(mainEditor);
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                itemControl.activate(false);
            }
        });
    }

    @Override
    public void aboutToBeShown()
    {
        if (!activated) {
            activated = true;
            boolean isLazy = !(node instanceof DBNDatabaseNode) || ((DBNDatabaseNode) node).needsInitialization();
            itemControl.loadData(isLazy);
        }
    }

    @Override
    public void aboutToBeHidden()
    {
    }

    public IDatabaseEditorInput getEditorInput()
    {
        return mainEditor.getEditorInput();
    }

    @Override
    public boolean isSearchPossible()
    {
        return itemControl.isSearchPossible();
    }

    @Override
    public boolean isSearchEnabled()
    {
        return itemControl.isSearchEnabled();
    }

    @Override
    public boolean performSearch(SearchType searchType)
    {
        return itemControl.performSearch(searchType);
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        if (!activated || itemControl == null || itemControl.isDisposed()) {
            return;
        }
        if (!force) {
            return;
        }
        // Check - do we need to load new content in editor
        // If this is DBM event then check node change type
        // UNLOAD usually means that connection was closed on connection's node is not removed but
        // is in "unloaded" state.
        // Without this check editor will try to reload it's content and thus will reopen just closed connection
        // (by calling getChildren() on DBNNode)
        boolean loadNewData = true;
        if (source instanceof DBNEvent) {
            DBNEvent.NodeChange nodeChange = ((DBNEvent) source).getNodeChange();
            if (nodeChange == DBNEvent.NodeChange.UNLOAD) {
                loadNewData = false;
            }
        }
        if (loadNewData) {
            itemControl.loadData(false);
        }
    }

    @Override
    public DBNNode getRootNode() {
        return itemControl.getRootNode();
    }

    @Override
    public Viewer getNavigatorViewer() {
        return itemControl.getNavigatorViewer();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(itemControl.getClass())) {
            return adapter.cast(itemControl);
        }
        return null;
    }
}