/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * FolderEditor
 */
public class FolderEditor extends SinglePageDatabaseEditor<FolderEditorInput> implements INavigatorModelView, ISearchContextProvider
{
    //static final Log log = LogFactory.getLog(FolderEditor.class);

    private ItemListControl itemControl;

    public void createPartControl(Composite parent)
    {
        itemControl = new ItemListControl(parent, SWT.NONE, this, getEditorInput().getTreeNode(), null);
        itemControl.createProgressPanel();
        itemControl.loadData();
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    public DBNNode getRootNode() {
        return getEditorInput().getTreeNode();
    }

    public Viewer getNavigatorViewer()
    {
        return itemControl.getNavigatorViewer();
    }

    public void refreshPart(Object source)
    {
        Display.getDefault().asyncExec(new Runnable() {
            public void run()
            {

                if (!itemControl.isDisposed()) {
                    itemControl.loadData(false);
                }

            }
        });
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
