/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties.tabbed;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.IProgressControlProvider;
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

    private IDatabaseNodeEditor editor;
    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;
    //private ISelectionProvider prevSelectionProvider;

    private Composite parent;

    NodeEditorSection(IDatabaseNodeEditor editor, DBNNode node, DBXTreeNode metaNode)
    {
        this.editor = editor;
        this.node = node;
        this.metaNode = metaNode;

        if (editor instanceof IRefreshableContainer) {
            ((IRefreshableContainer) editor).addRefreshClient(this);
        }
    }

    public void dispose()
    {
        if (editor instanceof IRefreshableContainer) {
            ((IRefreshableContainer) editor).removeRefreshClient(this);
        }
    }

    public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage)
    {
        this.parent = parent;
    }

    public void setFocus()
    {
    }

    public void setInput(IWorkbenchPart part, ISelection selection)
    {
        this.editor = (IDatabaseNodeEditor)part;
    }

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

        itemControl = new ItemListControl(parent, SWT.SHEET, editor, node, metaNode);
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

    public int getMinimumHeight()
    {
        return SWT.DEFAULT;
    }

    public boolean shouldUseExtraSpace()
    {
        return true;
    }

    public void refresh()
    {
        // Do nothing
        // Property tab section is refreshed on every activation so just skip it
    }

    public IDatabaseNodeEditorInput getEditorInput()
    {
        return editor.getEditorInput();
    }

    public DBPDataSource getDataSource()
    {
        return getEditorInput().getDataSource();
    }

    public boolean isSearchPossible()
    {
        return itemControl.isSearchPossible();
    }

    public boolean isSearchEnabled()
    {
        return itemControl.isSearchEnabled();
    }

    public boolean performSearch(SearchType searchType)
    {
        return itemControl.performSearch(searchType);
    }

    public void refreshPart(Object source)
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