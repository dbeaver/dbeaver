/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerConfigureFilter;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

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
    private Font normalFont;
    private Font boldFont;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchSite workbenchSite,
        DBNNode node,
        DBXTreeNode metaNode)
    {
        super(parent, style, workbenchSite, node, metaNode);

        this.searcher = new Searcher();
        this.searchHighlightColor = new Color(parent.getDisplay(), 170, 255, 170);
        this.disabledCellColor = parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
        this.normalFont = parent.getFont();
        this.boldFont = UIUtils.makeBoldFont(normalFont);
    }

    @Override
    protected void fillCustomToolbar(ToolBarManager toolbarManager)
    {
        if (getRootNode() instanceof DBNDatabaseFolder && ((DBNDatabaseFolder)getRootNode()).getItemsMeta() != null) {
            toolbarManager.add(new Action(
                "Filter",
                DBeaverIcons.getImageDescriptor(UIIcon.FILTER))
            {
                @Override
                public void run()
                {
                    NavigatorHandlerConfigureFilter.configureFilters(getShell(), getRootNode());
                }
            });
        }
    }

    @Override
    public void disposeControl()
    {
//        if (objectEditorHandler != null) {
//            objectEditorHandler.dispose();
//            objectEditorHandler = null;
//        }
        UIUtils.dispose(searchHighlightColor);
        UIUtils.dispose(disabledCellColor);
        UIUtils.dispose(boldFont);
        super.disposeControl();
    }

    @Override
    protected ISearchExecutor getSearchRunner()
    {
        return searcher;
    }

    @Override
    protected LoadingJob<Collection<DBNNode>> createLoadService()
    {
        return RuntimeUtils.createService(
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
                List<DBNNode> items = new ArrayList<>();
                List<? extends DBNNode> children = getRootNode().getChildren(getProgressMonitor());
                if (CommonUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (getProgressMonitor().isCanceled()) {
                        break;
                    }
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
                throw new InvocationTargetException(ex);
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
                return UIUtils.createPropertyEditor(getWorkbenchSite(), getControl(), property.getSource(), property);
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
                return getListPropertySource().getPropertyValue(null, getObjectValue(object), property);
            }
            return null;
        }

        @Override
        protected void setValue(Object element, Object value)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = getObjectProperty(object, columnIndex);
            if (property != null) {
                getListPropertySource().setPropertyValue(null, getObjectValue(object), property, value);
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
        public Font getFont(Object element)
        {
            return columnIndex == 0 && NavigatorUtils.isDefaultElement(element) ? boldFont : normalFont;
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
