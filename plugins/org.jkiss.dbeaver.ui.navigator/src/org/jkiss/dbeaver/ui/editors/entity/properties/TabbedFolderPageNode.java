/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.ui.IProgressControlProvider;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.itemlist.ItemListControl;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * EntityNodeEditor
 */
class TabbedFolderPageNode extends TabbedFolderPage implements ISearchContextProvider, IRefreshablePart, INavigatorModelView, DBPAdaptable
{

    private final IDatabaseEditor mainEditor;
    private final DBNNode node;
    private final DBXTreeNode metaNode;
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
        Control nodeItemsViewer = itemControl.getItemsViewer().getControl();
        nodeItemsViewer.addFocusListener(new FocusListener() {
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

        {
            final Accessible accessible = nodeItemsViewer.getAccessible();
            accessible.addAccessibleListener(new FolderAccessibleAdapter());
        }
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

    public IEditorInput getEditorInput()
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
    public RefreshResult refreshPart(Object source, boolean force)
    {
        if (!activated || itemControl == null || itemControl.isDisposed()) {
            return RefreshResult.IGNORED;
        }
        // Check - do we need to load new content in editor
        // If this is DBM event then check node change type
        // UNLOAD usually means that connection was closed on connection's node is not removed but
        // is in "unloaded" state.
        // Without this check editor will try to reload it's content and thus will reopen just closed connection
        // (by calling getChildren() on DBNNode)
        boolean loadNewData = true;
        if (!force && source instanceof DBNEvent) {
            DBNEvent event = (DBNEvent) source;
            DBNEvent.NodeChange nodeChange = event.getNodeChange();

            if (event.getAction() == DBNEvent.Action.UPDATE && nodeChange == DBNEvent.NodeChange.REFRESH) {
                // Do not refresh if refreshed object is not in the list
                loadNewData = isRefreshingEvent(event);
            } else if (nodeChange == DBNEvent.NodeChange.UNLOAD) {
                loadNewData = false;
            }
        }
        if (loadNewData) {
            itemControl.loadData(false);
            return RefreshResult.REFRESHED;
        }

        return RefreshResult.IGNORED;
    }

    private boolean isRefreshingEvent(DBNEvent event) {
        if (event.getSource() == DBNEvent.UPDATE_ON_SAVE) {
            return true;
        }
        if (!(event.getSource() instanceof DBPEvent)) {
            return false;
        }

        DBPEvent dbEvent = (DBPEvent)event.getSource();
        if (dbEvent.getData() == DBPEvent.REORDER) {
            DBNNode rootNode = getRootNode();
            // Reorder of child elements
            return rootNode instanceof DBNDatabaseNode &&
                dbEvent.getObject() == ((DBNDatabaseNode) rootNode).getValueObject();
        }
        Object itemsInput = itemControl.getItemsViewer().getInput();

        return itemsInput instanceof Collection &&
            ((Collection<?>) itemsInput).contains(dbEvent.getObject());
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
        if (itemControl == null) {
            return null;
        }
        if (adapter.isAssignableFrom(itemControl.getClass())) {
            return adapter.cast(itemControl);
        }
        return null;
    }


    private class FolderSimpleAccessibleAdapter extends AccessibleAdapter {
        public void getName(AccessibleEvent e) {
            if (e.childID == ACC.CHILDID_SELF) {
                e.result = CommonUtils.notNull(node.getNodeDescription(), node.getName());
            }
        }
    }

    private class FolderAccessibleAdapter extends AccessibleControlAdapter implements AccessibleListener {
        public void getName(AccessibleEvent e) {
            if (e.childID == ACC.CHILDID_SELF) {
                e.result = CommonUtils.notNull(node.getNodeDescription(), node.getName());
            }
        }

        @Override
        public void getDescription(AccessibleEvent e) {
            if (e.childID == ACC.CHILDID_SELF) {
                e.result = node.getNodeDescription();
            }
        }

        @Override
        public void getHelp(AccessibleEvent e) {

        }

        @Override
        public void getKeyboardShortcut(AccessibleEvent e) {

        }

        /////////////////////////////


        @Override
        public void getDefaultAction(AccessibleControlEvent e) {
        }

        @Override
        public void getFocus(AccessibleControlEvent e) {
            e.childID = ACC.CHILDID_SELF;
        }


        @Override
        public void getRole(AccessibleControlEvent e) {
            int role;
            int childID = e.childID;
            if (childID == ACC.CHILDID_SELF) {
                role = ACC.ROLE_TABFOLDER;
            } else {
                role = ACC.ROLE_TABITEM;
            }
            e.detail = role;
        }

        @Override
        public void getState(AccessibleControlEvent e) {
            int state = 0;
            int childID = e.childID;
            if (childID == ACC.CHILDID_SELF) {
                state = ACC.STATE_NORMAL;
            } else {
                state = ACC.STATE_SELECTABLE;
                if (itemControl.getItemsViewer().getControl().isFocusControl()) {
                    state |= ACC.STATE_FOCUSABLE;
                }
                if (!itemControl.getItemsViewer().getSelection().isEmpty()) {
                    state |= ACC.STATE_SELECTED;
                    if ((state & ACC.STATE_FOCUSABLE) != 0) {
                        state |= ACC.STATE_FOCUSED;
                    }
                }
            }
            e.detail = state;
        }
    }
}