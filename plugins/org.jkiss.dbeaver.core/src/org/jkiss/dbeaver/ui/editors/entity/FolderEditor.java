/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

/**
 * FolderEditor
 */
public class FolderEditor extends EditorPart implements INavigatorModelView, IRefreshablePart, ISearchContextProvider
{
    private ItemListControl itemControl;

    @Override
    public void createPartControl(Composite parent)
    {
        itemControl = new FolderListControl(parent);
        itemControl.createProgressPanel();
        itemControl.loadData();
        getSite().setSelectionProvider(itemControl.getSelectionProvider());
    }

    @Override
    public void setFocus() {
        itemControl.setFocus();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {

    }

    @Override
    public void doSaveAs() {

    }

    @Override
    public INavigatorEditorInput getEditorInput() {
        return (INavigatorEditorInput) super.getEditorInput();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        if (input != null) {
            final DBNNode navigatorNode = getEditorInput().getNavigatorNode();
            setTitleImage(DBeaverIcons.getImage(navigatorNode.getNodeIcon()));
            setPartName(navigatorNode.getNodeName());
        }
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public DBNNode getRootNode() {
        return getEditorInput().getNavigatorNode();
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
        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
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

    private class FolderListControl extends ItemListControl {
        public FolderListControl(Composite parent) {
            super(parent, SWT.NONE, FolderEditor.this.getSite(), FolderEditor.this.getEditorInput().getNavigatorNode(), null);
        }

        @Override
        protected void openNodeEditor(DBNNode node) {
            if (getRootNode() instanceof DBNContainer && node instanceof DBNLocalFolder) {
                setRootNode(node);
                loadData();
                setPartName(node.getNodeName());
                setTitleImage(DBeaverIcons.getImage(node.getNodeIcon()));
            } else {
                super.openNodeEditor(node);
            }
        }
    }
}
