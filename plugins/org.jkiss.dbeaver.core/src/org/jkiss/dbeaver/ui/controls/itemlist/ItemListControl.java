/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.ISearchTextRunner;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.properties.ObjectPropertyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private Searcher searcher;
    private SearchColorProvider searchColorProvider;
    private Color searchHighlightColor;

    private String curSearchString;
    private boolean caseSensitiveSearch;
    private Set<DBNNode> curSearchResult = null;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node,
        DBXTreeNode metaNode)
    {
        super(parent, style, workbenchPart, node, metaNode);
        searcher = new Searcher();
        searchColorProvider = new SearchColorProvider();
        searchHighlightColor = new Color(parent.getDisplay(), 128, 255, 128);
    }

    @Override
    public void dispose()
    {
//        if (objectEditorHandler != null) {
//            objectEditorHandler.dispose();
//            objectEditorHandler = null;
//        }
        UIUtils.dispose(searchHighlightColor);
        super.dispose();
    }

    @Override
    protected ISearchTextRunner getSearchRunner()
    {
        return searcher;
    }

    @Override
    protected LoadingJob<Collection<DBNNode>> createLoadService()
    {
        return LoadingUtils.createService(
            new ItemLoadService(getNodeMeta()),
            new ObjectsLoadVisualizer());
    }

    @Override
    protected EditingSupport makeEditingSupport(ViewerColumn viewerColumn, int columnIndex)
    {
        return new CellEditingSupport(columnIndex);
    }

    @Override
    public IColorProvider getObjectColorProvider()
    {
        return searchColorProvider;
    }

    private boolean matchesSearch(Object element)
    {
        if (CommonUtils.isEmpty(curSearchString)) {
            return false;
        }
        if (element instanceof DBNNode) {
            DBNNode node = (DBNNode)element;
            String nodeName = node.getNodeName();
            if (!caseSensitiveSearch) {
                nodeName = nodeName.toUpperCase();
            }
            return nodeName.indexOf(curSearchString) != -1;
        } else {
            return false;
        }
    }

    private class ItemLoadService extends DatabaseLoadService<Collection<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items", getRootNode() instanceof DBSWrapper ? (DBSWrapper)getRootNode() : null);
            this.metaNode = metaNode;
        }

        public Collection<DBNNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBNNode> items = new ArrayList<DBNNode>();
                List<? extends DBNNode> children = getRootNode().getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
/*
                    if (item instanceof DBNDatabaseFolder) {
                        continue;
                    }
*/
                    if (metaNode != null) {
                        if (!(item instanceof DBNDatabaseNode)) {
                            continue;
                        }
                        if (((DBNDatabaseNode)item).getMeta() != metaNode) {
                            continue;
                        }
                    }
                    items.add(item);
                }
                return items;
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

    private class CellEditingSupport extends EditingSupport {

        private int columnIndex;

        public CellEditingSupport(int columnIndex)
        {
            super(getItemsViewer());
            this.columnIndex = columnIndex;
        }

        @Override
        protected CellEditor getCellEditor(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null && property.isEditable()) {
                return property.createPropertyEditor(getControl());
            }
            return null;
        }

        @Override
        protected boolean canEdit(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            return property != null && property.isEditable();
        }

        protected Object getValue(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                return getListPropertySource().getPropertyValue(property.getId());
            }
            return null;
        }

        protected void setValue(Object element, Object value)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                getListPropertySource().setPropertyValue(element, value);
            }
        }

    }

    private class Searcher implements ISearchTextRunner {

        public boolean performSearch(String searchString, int options)
        {
            caseSensitiveSearch = (options & SEARCH_CASE_SENSITIVE) != 0;
            if (!caseSensitiveSearch) {
                searchString = searchString.toUpperCase();
            }
            if (!CommonUtils.equalObjects(curSearchString, searchString)) {
                curSearchString = searchString;
                Set<DBNNode> oldSearchResult = curSearchResult;
                curSearchResult = null;
                boolean found = false;
                Collection<DBNNode> nodes = (Collection<DBNNode>) getItemsViewer().getInput();
                if (!CommonUtils.isEmpty(nodes)) {
                    for (DBNNode node : nodes) {
                        if (matchesSearch(node)) {
                            if (curSearchResult == null) {
                                curSearchResult = new LinkedHashSet<DBNNode>(50);
                            }
                            curSearchResult.add(node);
                            getItemsViewer().update(node, null);
                            found = true;
                        }
                    }
                }
                if (!CommonUtils.isEmpty(oldSearchResult)) {
                    for (DBNNode oldNode : oldSearchResult) {
                        if (curSearchResult == null || !curSearchResult.contains(oldNode)) {
                            getItemsViewer().update(oldNode, null);
                        }
                    }
                }
                return found;
            } else {
                return true;
            }
        }

        public void cancelSearch()
        {
            if (curSearchString != null) {
                curSearchString = null;
                if (curSearchResult != null) {
                    Set<DBNNode> oldSearchResult = curSearchResult;
                    curSearchResult = null;
                    for (DBNNode oldNode : oldSearchResult) {
                        getItemsViewer().update(oldNode, null);
                    }
                }
            }
        }
    }

    private class SearchColorProvider implements IColorProvider {

        public Color getForeground(Object element)
        {
            return null;
        }

        public Color getBackground(Object element)
        {
            return curSearchResult != null && curSearchResult.contains((DBNNode) element) ? searchHighlightColor : null;
        }
    }
}
