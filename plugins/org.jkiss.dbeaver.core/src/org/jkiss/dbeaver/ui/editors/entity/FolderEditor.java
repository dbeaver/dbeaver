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
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.itemlist.ItemListControl;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * FolderEditor
 */
public class FolderEditor extends EditorPart implements INavigatorModelView, IRefreshablePart, ISearchContextProvider
{
    private static final Log log = Log.getLog(FolderEditor.class);

    private FolderListControl itemControl;
    private List<String> history = new ArrayList<>();
    private int historyPosition = 0;

    @Override
    public void createPartControl(Composite parent)
    {
        itemControl = new FolderListControl(parent);
        itemControl.createProgressPanel();
        itemControl.loadData();
        getSite().setSelectionProvider(itemControl.getSelectionProvider());

        history.add(getRootNode().getNodeItemPath());
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

    public int getHistoryPosition() {
        return historyPosition;
    }

    public int getHistorySize() {
        return history.size();
    }

    public void navigateHistory(int position) {
        if (position >= history.size()) {
            position = history.size() - 1;
        } else if (position < 0) {
            position = -1;
        }
        if (position < 0 || position >= history.size()) {
            return;
        }
        String nodePath = history.get(position);
        try {
            DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByPath(VoidProgressMonitor.INSTANCE, nodePath);
            if (node != null) {
                historyPosition = position;
                itemControl.changeCurrentNode(node);
            }
        } catch (DBException e) {
            log.error(e);
        }

    }

    private class FolderListControl extends ItemListControl {
        public FolderListControl(Composite parent) {
            super(parent, SWT.SHEET, FolderEditor.this.getSite(), FolderEditor.this.getEditorInput().getNavigatorNode(), null);
        }

        @Override
        protected void setListData(Collection<DBNNode> items, boolean append) {
            if (!append) {
                // Add parent node reference (we actually add DBNRoot to avoid unneeded parent properties columns loading)
                final DBNNode rootNode = getRootNode();
                final DBNNode parentNode = rootNode.getParentNode();
                if (parentNode instanceof DBNProjectDatabases || parentNode instanceof DBNLocalFolder || parentNode instanceof DBNResource) {
                    List<DBNNode> nodesWithParent = new ArrayList<>(items);
                    nodesWithParent.add(0, DBeaverCore.getInstance().getNavigatorModel().getRoot());
                    items = nodesWithParent;
                }
            }
            super.setListData(items, append);
        }

        @Nullable
        @Override
        protected Object getCellValue(Object element, ObjectColumn objectColumn) {
            if (element instanceof DBNRoot) {
                return objectColumn.isNameColumn(getObjectValue((DBNRoot)element)) ? ".." : "";
            }
            return super.getCellValue(element, objectColumn);
        }

        @Override
        protected void openNodeEditor(DBNNode node) {
            final DBNNode rootNode = getRootNode();
            if (!(node instanceof DBNDatabaseNode)) {
                if (node instanceof DBNRoot) {
                    if (rootNode instanceof DBNLocalFolder) {
                        node = ((DBNLocalFolder) rootNode).getLogicalParent();
                    } else {
                        node = rootNode.getParentNode();
                    }
                }

                if (historyPosition >= 0) {
                    while (historyPosition < history.size() - 1) {
                        history.remove(historyPosition + 1);
                    }
                }
                historyPosition++;
                history.add(node.getNodeItemPath());
                changeCurrentNode(node);
            } else {
                super.openNodeEditor(node);
            }
        }

        private void changeCurrentNode(DBNNode node) {
            if (getRootNode() != null && node.getClass() != getRootNode().getClass()) {
                // Different node type - cleanup
                clearListData();
            }
            setRootNode(node);
            loadData();
            setPartName(node.getNodeName());
            setTitleImage(DBeaverIcons.getImage(node.getNodeIcon()));
            updateActions();

            // Update editor input
            final INavigatorEditorInput editorInput = getEditorInput();
            if (editorInput instanceof NodeEditorInput) {
                ((NodeEditorInput) editorInput).setNavigatorNode(node);
            }
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            contributionManager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_BACK));
            contributionManager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_FORWARD));
            contributionManager.add(new Separator());
            super.fillCustomActions(contributionManager);
        }
    }

}
