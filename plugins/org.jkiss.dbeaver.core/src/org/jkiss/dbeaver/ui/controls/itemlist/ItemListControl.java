/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.ISearchExecutor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.ObjectPropertyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private Searcher searcher;
    private Color searchHighlightColor;
    private Color disabledCellColor;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchPart workbenchPart,
        DBNNode node,
        DBXTreeNode metaNode)
    {
        super(parent, style, workbenchPart, node, metaNode);
        searcher = new Searcher();
        searchHighlightColor = new Color(parent.getDisplay(), 170, 255, 170);
        disabledCellColor = new Color(parent.getDisplay(), 0xEA, 0xEA, 0xEA);
    }

    @Override
    public void dispose()
    {
//        if (objectEditorHandler != null) {
//            objectEditorHandler.dispose();
//            objectEditorHandler = null;
//        }
        UIUtils.dispose(searchHighlightColor);
        UIUtils.dispose(disabledCellColor);
        super.dispose();
    }

    @Override
    protected ISearchExecutor getSearchRunner()
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
    protected CellLabelProvider getColumnLabelProvider(int columnIndex)
    {
        return new ItemColorProvider(columnIndex);
    }

    private class ItemLoadService extends DatabaseLoadService<Collection<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items", getRootNode() instanceof DBSWrapper ? (DBSWrapper)getRootNode() : null);
            this.metaNode = metaNode;
        }

        @Override
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
            // Set cur list object to let property see it in createPropertyEditor
            setCurListObject(object);
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null && property.isEditable(getObjectValue(object))) {
                return property.createPropertyEditor(getControl());
            }
            return null;
        }

        @Override
        protected boolean canEdit(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            return property != null && property.isEditable(getObjectValue(object));
        }

        @Override
        protected Object getValue(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                return getListPropertySource().getPropertyValue(getObjectValue(object), property);
            }
            return null;
        }

        @Override
        protected void setValue(Object element, Object value)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                getListPropertySource().setPropertyValue(getObjectValue(object), property, value);
                //System.out.println("UPDATE " + value + " " + System.currentTimeMillis());
                if (value instanceof Boolean) {
                    // Redraw control to let it repaint checkbox
                    getItemsViewer().getControl().redraw();
                }
            }
        }

    }

    private class Searcher extends ObjectSearcher<DBNNode> {
        @Override
        protected void setInfo(String message)
        {
            ItemListControl.this.setInfo(message);
        }

        @Override
        protected Collection<DBNNode> getContent()
        {
            return (Collection<DBNNode>) getItemsViewer().getInput();
        }

        @Override
        protected void selectObject(DBNNode object)
        {
            getItemsViewer().setSelection(object == null ? new StructuredSelection() : new StructuredSelection(object));
        }

        @Override
        protected void updateObject(DBNNode object)
        {
            getItemsViewer().update(object, null);
        }

        @Override
        protected void revealObject(DBNNode object)
        {
            getItemsViewer().reveal(object);
        }

    }

    private class ItemColorProvider extends ObjectColumnLabelProvider {

        ItemColorProvider(int columnIndex)
        {
            super(columnIndex);
        }

        @Override
        public Color getForeground(Object element)
        {
            return null;
        }

        @Override
        public Color getBackground(Object element)
        {
            DBNNode node = (DBNNode) element;
            if (node.isDisposed()) {
                return null;
            }
            if (searcher != null && searcher.hasObject(node)) {
                return searchHighlightColor;
            }
            final Object objectValue = getObjectValue(node);
            if (isNewObject(node)) {
                final ObjectPropertyDescriptor prop = getColumn(columnIndex).getProperty(objectValue);
                if (prop != null && !prop.isEditable(objectValue)) {
                    return disabledCellColor;
                }
            }
            return null;
        }
    }

}
