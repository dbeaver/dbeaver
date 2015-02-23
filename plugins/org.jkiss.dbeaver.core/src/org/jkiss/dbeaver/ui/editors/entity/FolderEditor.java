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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.Nullable;
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
    //static final Log log = Log.getLog(FolderEditor.class);

    private ItemListControl itemControl;

    @Override
    public void createPartControl(Composite parent)
    {
        itemControl = new ItemListControl(parent, SWT.NONE, this.getSite(), getEditorInput().getTreeNode(), null);
        itemControl.createProgressPanel();
        itemControl.loadData();
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    @Override
    public DBNNode getRootNode() {
        return getEditorInput().getTreeNode();
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer()
    {
        return itemControl.getNavigatorViewer();
    }

    @Override
    public void refreshPart(Object source, boolean force)
    {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run()
            {

                if (!itemControl.isDisposed()) {
                    itemControl.loadData(false);
                }

            }
        });
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
}
