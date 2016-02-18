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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.NodeListControl;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public abstract class AbstractSearchResultsPage <OBJECT_TYPE> extends Page implements IObjectSearchResultPage<OBJECT_TYPE>, INavigatorModelView {

    private SearchResultsControl itemList;

    @Override
    public void createControl(Composite parent)
    {
        itemList = createResultControl(parent);
        itemList.createProgressPanel();
        itemList.setInfo(CoreMessages.dialog_search_objects_item_list_info);
        itemList.setFitWidth(true);
        itemList.setLayoutData(new GridData(GridData.FILL_BOTH));

        getSite().setSelectionProvider(itemList.getSelectionProvider());
    }

    protected SearchResultsControl createResultControl(Composite parent) {
        return new SearchResultsControl(parent);
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public Control getControl()
    {
        return itemList;
    }

    @Override
    public void setFocus()
    {
        if (itemList != null && !itemList.isDisposed()) {
            itemList.setFocus();
        }
    }

    @Override
    public void populateObjects(DBRProgressMonitor monitor, Collection<OBJECT_TYPE> objects)
    {
        if (itemList != null && !itemList.isDisposed()) {
            List<DBNNode> nodes = new ArrayList<>(objects.size());
            for (OBJECT_TYPE object : objects) {
                nodes.add(getNodeFromObject(object));
            }
            TreeViewer itemsViewer = (TreeViewer) itemList.getItemsViewer();
            Collection<DBNNode> oldNodes = itemList.getListData();
            List<DBNNode> newNodes = new ArrayList<>();
            if (!CommonUtils.isEmpty(oldNodes)) {
                newNodes.addAll(oldNodes);
            }
            newNodes.addAll(nodes);
            ((ResultsContentProvider)itemsViewer.getContentProvider()).rebuildObjectTree(newNodes);
            itemList.appendListData(nodes);
            itemsViewer.expandAll();
        }
    }

    protected abstract DBNNode getNodeFromObject(OBJECT_TYPE object);

    @Override
    public void clearObjects()
    {
        itemList.clearListData();
    }

    @Override
    public DBNNode getRootNode()
    {
        return itemList.getRootNode();
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer()
    {
        return itemList.getNavigatorViewer();
    }

    protected class SearchResultsControl extends NodeListControl {
        public SearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup, SWT.SHEET, getSite(),
                DBeaverCore.getInstance().getNavigatorModel().getRoot(),
                new ResultsContentProvider());
        }

        @Override
        protected void fillCustomToolbar(ToolBarManager toolbarManager)
        {
        }

        @Override
        protected Class<?>[] getListBaseTypes(Collection<DBNNode> items)
        {
            return new Class<?>[] {DBPNamedObject.class};
        }

        @Override
        protected LoadingJob<Collection<DBNNode>> createLoadService()
        {
            // No load service
            return null;
        }
    }

    private static class ResultsNode {
        DBNNode node;
        ResultsNode parent;
        final List<ResultsNode> children = new ArrayList<>();

        public ResultsNode(DBNNode node, ResultsNode parent)
        {
            this.node = node;
            this.parent = parent;
        }
        DBNNode[] getChildrenNodes()
        {
            DBNNode[] nodes = new DBNNode[children.size()];
            for (int i = 0; i < children.size(); i++) {
                nodes[i] = children.get(i).node;
            }
            return nodes;
        }
    }

    private class ResultsContentProvider extends TreeContentProvider {

        private ResultsNode rootResults;
        private Map<DBNNode,ResultsNode> nodeMap;

        private ResultsContentProvider() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput instanceof Collection) {
                rebuildObjectTree((Collection<DBNNode>) newInput);
            }
        }

        @Override
        public Object getParent(Object element)
        {
            if (element instanceof DBNNode) {
                ResultsNode results = nodeMap.get(element);
                if (results != null && results.parent != null) {
                    return results.parent.node;
                }
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object parentElement)
        {
            if (parentElement instanceof DBNNode) {
                ResultsNode results = nodeMap.get(parentElement);
                return results != null && !results.children.isEmpty();
            }
            return false;
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof DBNNode) {
                ResultsNode results = nodeMap.get(parentElement);
                if (results != null) {
                    return results.getChildrenNodes();
                }
            }
            return null;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return rootResults.getChildrenNodes();
        }

        private void rebuildObjectTree(Collection<DBNNode> nodeList)
        {
            rootResults = new ResultsNode(getRootNode(), null);
            nodeMap = new IdentityHashMap<>();
            final List<DBNNode> allParents = new ArrayList<>();
            for (DBNNode node : nodeList) {
                // Collect parent nodes
                allParents.clear();
                for (DBNNode parent = node.getParentNode(); parent != null && parent != getRootNode(); parent = parent.getParentNode()) {
                    if (parent instanceof DBNContainer || parent instanceof DBNResource) {
                        continue;
                    }
                    allParents.add(0, parent);
                }
                // Construct hierarchy
                ResultsNode curParentResults = rootResults;
                for (DBNNode parent : allParents) {
                    ResultsNode parentResults = nodeMap.get(parent);
                    if (parentResults == null) {
                        parentResults = new ResultsNode(parent, curParentResults);
                        nodeMap.put(parent, parentResults);
                        curParentResults.children.add(parentResults);
                    }
                    curParentResults = parentResults;
                }
                // Make leaf
                ResultsNode leaf = new ResultsNode(node, curParentResults);
                nodeMap.put(node, leaf);
                curParentResults.children.add(leaf);
            }
        }

    }
}
