/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.FolderPage;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;

/**
 * EntityNodeEditor
 */
class FolderPageNode extends FolderPage implements ISearchContextProvider, IRefreshablePart, INavigatorModelView
{

    private IDatabaseEditor editor;
    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;

    FolderPageNode(IDatabaseEditor editor, DBNNode node, DBXTreeNode metaNode)
    {
        this.editor = editor;
        this.node = node;
        this.metaNode = metaNode;

        if (editor instanceof IRefreshableContainer) {
            ((IRefreshableContainer) editor).addRefreshClient(this);
        }
    }

    @Override
    public void dispose()
    {
        if (editor instanceof IRefreshableContainer) {
            ((IRefreshableContainer) editor).removeRefreshClient(this);
        }
    }

    public void setFocus()
    {
        itemControl.setFocus();
    }

    @Override
    public void createControl(Composite parent) {
        itemControl = new ItemListControl(parent, SWT.SHEET, editor.getSite(), node, metaNode);
        //itemControl.getLayout().marginHeight = 0;
        //itemControl.getLayout().marginWidth = 0;
        ProgressPageControl progressControl = null;
        if (editor instanceof IProgressControlProvider) {
            progressControl = ((IProgressControlProvider)editor).getProgressControl();
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
                editor.getSite().setSelectionProvider(selectionProvider);
                selectionProvider.setSelection(selectionProvider.getSelection());
                itemControl.activate(true);

                // Notify owner MultiPart editor about page change
                // We need it to update search actions and other contributions provided by node editor
                if (editor.getSite() instanceof MultiPageEditorSite) {
                    ((MultiPageEditorSite) editor.getSite()).getMultiPageEditor().setActiveEditor(editor);
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
        return editor.getEditorInput();
    }

    public DBPDataSource getDataSource()
    {
        return getEditorInput().getDataSource();
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
        if (loadNewData && !itemControl.isDisposed()) {
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
}