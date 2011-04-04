/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

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
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * EntityNodeEditor
 */
class NodeEditorSection implements ISection, ISearchContextProvider
{
    //static final Log log = LogFactory.getLog(EntityNodeEditor.class);

    private IDatabaseNodeEditor editor;
    private DBNNode node;
    private DBXTreeNode metaNode;
    private ItemListControl itemControl;
    private boolean activated;
    private ISelectionProvider prevSelectionProvider;

    private Composite parent;

    NodeEditorSection(IDatabaseNodeEditor editor, DBNNode node, DBXTreeNode metaNode)
    {
        this.editor = editor;
        this.node = node;
        this.metaNode = metaNode;
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

        if (!activated) {
            activated = true;
            itemControl.loadData();
        }
        prevSelectionProvider = editor.getSite().getSelectionProvider();
        editor.getSite().setSelectionProvider(itemControl.getSelectionProvider());
        itemControl.activate(true);
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

        // Hook context menu
        ViewUtils.addContextMenu(editor, itemControl.getNavigatorViewer());
        // Add drag and drop support
        ViewUtils.addDragAndDropSupport(itemControl.getNavigatorViewer());
        //ISelectionProvider selectionProvider = itemControl.getSelectionProvider();
        editor.getSite().setSelectionProvider(itemControl.getSelectionProvider());

        parent.layout();
    }

    public void dispose() {

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
        if (activated) {
            return;
        }
        // Check - do we need to load new content in editor
        // If this is DBM event then check node change type
        // UNLOAD usually means that connection was closed on connection's node is not removed but
        // is in "unloaded" state.
        // Without this check editor will try to reload it's content and thus will reopen just closed connection
        // (by calling getChildren() on DBNNode)
        boolean loadNewData = true;
//        if (source instanceof DBNEvent) {
//            DBNEvent.NodeChange nodeChange = ((DBNEvent) source).getNodeChange();
//            if (nodeChange == DBNEvent.NodeChange.UNLOAD) {
//                loadNewData = false;
//            }
//        }
        if (!itemControl.isDisposed()) {
            itemControl.clearData();
            if (loadNewData) {
                itemControl.loadData();
            }
        }
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
}