/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.search;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.Page;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.itemlist.NodeListControl;
import org.jkiss.dbeaver.ui.search.internal.UISearchMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.collections.ListMultimap;
import org.jkiss.utils.collections.Multimap;

import java.util.*;

public abstract class AbstractSearchResultsPage <OBJECT_TYPE> extends Page implements ISearchResultPage,INavigatorModelView {

    private String id;
    private ISearchResult searchResult;
    private Object uiState;
    private ISearchResultViewPart viewPart;
    private ISearchResultListener resultListener;

    private SearchResultsControl itemList;

    public AbstractSearchResultsPage() {
        this.resultListener = e -> {
            List objects = null;
            if (e instanceof AbstractSearchResult.DatabaseSearchResultEvent) {
                objects = ((AbstractSearchResult.DatabaseSearchResultEvent) e).getObjects();
            } else if (e instanceof AbstractSearchResult.DatabaseSearchFinishEvent) {
                UIUtils.asyncExec(() -> {
                    itemList.setInfo("Found " + ((AbstractSearchResult.DatabaseSearchFinishEvent) e).getTotalObjects() + " objects");
                });
            } else if (e.getSearchResult() instanceof AbstractSearchResult) {
                final AbstractSearchResult result = (AbstractSearchResult) e.getSearchResult();
                objects = result.getObjects();
            }
            if (objects != null) {
                final List newObjects = objects;
                UIUtils.syncExec(() -> populateObjects(newObjects));
            }

        };
    }

    @Override
    public void createControl(Composite parent)
    {
        itemList = createResultControl(parent);
        itemList.createProgressPanel();
        itemList.setInfo(UISearchMessages.dialog_search_objects_item_list_info);
        itemList.setFitWidth(true);
        itemList.setLayoutData(new GridData(GridData.FILL_BOTH));

        getSite().setSelectionProvider(itemList.getSelectionProvider());
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    protected SearchResultsControl createResultControl(Composite parent) {
        return new SearchResultsControl(parent);
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

    public void populateObjects(Collection<OBJECT_TYPE> objects)
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

    @Override
    public Object getUIState()
    {
        return uiState;
    }

    @Override
    public void setInput(ISearchResult search, Object uiState)
    {
        itemList.setInfo(search == null ? "" : "Searching for '" + search.getLabel() + "'");

        if (this.searchResult != null) {
            this.searchResult.removeListener(this.resultListener);
        }
        this.searchResult = search;
        this.uiState = uiState;
        if (this.searchResult != null) {
            this.searchResult.addListener(this.resultListener);
        }
        if (this.searchResult == null) {
            clearObjects();
        } else if (searchResult instanceof AbstractSearchResult) {
            populateObjects(((AbstractSearchResult) searchResult).getObjects());
        }
    }

    @Override
    public void setViewPart(ISearchResultViewPart part)
    {
        this.viewPart = part;
    }

    @Override
    public void restoreState(IMemento memento)
    {

    }

    @Override
    public void saveState(IMemento memento)
    {

    }

    @Override
    public void setID(String id)
    {
        this.id = id;
    }

    @Override
    public String getID()
    {
        return this.id;
    }

    @Override
    public String getLabel()
    {
        return searchResult == null ? "" : searchResult.getLabel();
    }

    protected class SearchResultsControl extends NodeListControl {
        public SearchResultsControl(Composite resultsGroup)
        {
            super(resultsGroup, SWT.SHEET, getSite(),
                DBWorkbench.getPlatform().getNavigatorModel().getRoot(),
                new ResultsContentProvider());
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager)
        {
        }

        @Override
        protected Class<?>[] getListBaseTypes(Collection<DBNNode> items)
        {
            return new Class<?>[] {DBPNamedObject.class};
        }

        @Override
        protected LoadingJob<Collection<DBNNode>> createLoadService(boolean forUpdate)
        {
            // No load service
            return null;
        }
    }

    private class ResultsContentProvider extends TreeContentProvider {
        // We maintain tree structure using two maps because we need fast lookup for parents and children for a given DBNNode.
        // Using an explicit tree gives us quadratic complexity.

        /**
         * A map that links nodes with their parents.
         */
        private final Map<DBNNode, DBNNode> parentMap = new HashMap<>();

        /**
         * A multimap that links nodes with their children.
         */
        private final Multimap<DBNNode, DBNNode> childrenMultimap = new ListMultimap<>();

        private DBNNode rootNode;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput instanceof Collection) {
                rebuildObjectTree((Collection<DBNNode>) newInput);
            }
        }

        @Override
        public Object getParent(Object element) {
            return parentMap.get(element);
        }

        @Override
        public boolean hasChildren(Object element) {
            Collection<DBNNode> children = childrenMultimap.get((DBNNode) element);
            return !CommonUtils.isEmpty(children);
        }

        @Nullable
        @Override
        public Object[] getChildren(Object parentElement) {
            Collection<DBNNode> children = childrenMultimap.get((DBNNode) parentElement);
            return CommonUtils.safeCollection(children).toArray();
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return getChildren(rootNode);
        }

        private void rebuildObjectTree(@NotNull Collection<? extends DBNNode> nodes) {
            parentMap.clear();
            childrenMultimap.clear();
            rootNode = getRootNode();

            // The nodes collection possibly does not contain all the necessary nodes to display.
            // For example, it may contain a table, but not its container, such as schema.
            // The root node is not a part of the tree (we don't show it).
            // Since we maintain the structure of the tree using two maps (see above), we need to populate them.
            // It means finding a parent for every node and updating the children multimap.
            Queue<DBNNode> queue = new ArrayDeque<>(nodes);
            while (!queue.isEmpty()) {
                DBNNode node = queue.poll();
                if (node == rootNode || parentMap.get(node) != null) {
                    continue;
                }
                DBNNode parent = node.getParentNode();
                while (parent != rootNode && (parent instanceof DBNContainer || parent instanceof DBNResource)) {
                    parent = parent.getParentNode();
                }
                if (parent == null) {
                    continue;
                }
                parentMap.put(node, parent);
                childrenMultimap.put(parent, node);
                queue.add(parent);
            }
        }
    }
}
