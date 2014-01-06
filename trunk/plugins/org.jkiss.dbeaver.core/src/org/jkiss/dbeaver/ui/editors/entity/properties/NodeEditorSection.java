/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.ui.IProgressControlProvider;
import org.jkiss.dbeaver.ext.ui.IRefreshableContainer;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;

/**
 * EntityNodeEditor
 */
class NodeEditorSection implements ISection, ISearchContextProvider, IRefreshablePart
{
    //static final Log log = LogFactory.getLog(EntityNodeEditor.class);

    private IDatabaseEditor editor;
    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;
    //private ISelectionProvider prevSelectionProvider;

    private Composite parent;

    NodeEditorSection(IDatabaseEditor editor, DBNNode node, DBXTreeNode metaNode)
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

    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage)
    {
        this.parent = parent;
    }

    public void setFocus()
    {
    }

    @Override
    public void setInput(IWorkbenchPart part, ISelection selection)
    {
        this.editor = (IDatabaseEditor)part;
    }

    @Override
    public void aboutToBeShown()
    {
        if (itemControl == null) {
            createSectionControls();
        }

        //prevSelectionProvider = editor.getSite().getSelectionProvider();
        // Update selection provider and selection
        final ISelectionProvider selectionProvider = itemControl.getSelectionProvider();
        editor.getSite().setSelectionProvider(selectionProvider);
        selectionProvider.setSelection(selectionProvider.getSelection());
        itemControl.activate(true);

        if (!activated) {
            activated = true;
            boolean isLazy = !(node instanceof DBNDatabaseNode) || ((DBNDatabaseNode) node).isLazyNode();
            itemControl.loadData(isLazy);
        }
    }

    @Override
    public void aboutToBeHidden()
    {
        if (itemControl != null) {
            itemControl.activate(false);
        }
/*
        if (prevSelectionProvider != null) {
            editor.getSite().setSelectionProvider(prevSelectionProvider);
        }
*/
    }

    private void createSectionControls()
    {
        if (itemControl != null) {
            return;
        }

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

        //ISelectionProvider selectionProvider = itemControl.getSelectionProvider();
        //editor.getSite().setSelectionProvider(itemControl.getSelectionProvider());

        parent.layout();
    }

    @Override
    public int getMinimumHeight()
    {
        return SWT.DEFAULT;
    }

    @Override
    public boolean shouldUseExtraSpace()
    {
        return true;
    }

    @Override
    public void refresh()
    {
        // Do nothing
        // Property tab section is refreshed on every activation so just skip it
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
        if (!itemControl.isDisposed()) {
            if (loadNewData) {
                itemControl.loadData(false);
            }
        }
    }
}