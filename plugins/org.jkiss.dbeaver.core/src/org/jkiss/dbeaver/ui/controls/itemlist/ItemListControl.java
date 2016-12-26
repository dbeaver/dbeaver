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
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerFilterConfig;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private ISearchExecutor searcher;
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

        this.searcher = new SearcherFilter();
        this.searchHighlightColor = new Color(parent.getDisplay(), 170, 255, 170);
        this.disabledCellColor = parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
        this.normalFont = parent.getFont();
        this.boldFont = UIUtils.makeBoldFont(normalFont);
    }

    @Override
    protected void fillCustomActions(IContributionManager contributionManager)
    {
        super.fillCustomActions(contributionManager);
        if (getRootNode() instanceof DBNDatabaseFolder && ((DBNDatabaseFolder)getRootNode()).getItemsMeta() != null) {
            contributionManager.add(new Action(
                "Filter",
                DBeaverIcons.getImageDescriptor(UIIcon.FILTER))
            {
                @Override
                public void run()
                {
                    NavigatorHandlerFilterConfig.configureFilters(getShell(), getRootNode());
                }
            });
        }
        IWorkbenchSite workbenchSite = getWorkbenchSite();
        if (workbenchSite != null) {
            contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
        }
        contributionManager.add(new Action(
            "Pack columns",
            DBeaverIcons.getImageDescriptor(UIIcon.TREE_EXPAND))
        {
            @Override
            public void run()
            {
                ColumnViewer itemsViewer = getItemsViewer();
                if (itemsViewer instanceof TreeViewer) {
                    UIUtils.packColumns(((TreeViewer) itemsViewer).getTree());
                } else {
                    UIUtils.packColumns(((TableViewer) itemsViewer).getTable());
                }
            }
        });
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
        return LoadingJob.createService(
            new ItemLoadService(getNodeMeta()),
            new ObjectsLoadVisualizer());
    }

    @Override
    protected EditingSupport makeEditingSupport(ObjectColumn objectColumn)
    {
        return new CellEditingSupport(objectColumn);
    }

    @Override
    protected CellLabelProvider getColumnLabelProvider(ObjectColumn objectColumn)
    {
        return new ItemColorProvider(objectColumn);
    }

    private class ItemLoadService extends DatabaseLoadService<Collection<DBNNode>> {

        private DBXTreeNode metaNode;

        protected ItemLoadService(DBXTreeNode metaNode)
        {
            super("Loading items", getRootNode() instanceof DBSWrapper ? (DBSWrapper)getRootNode() : null);
            this.metaNode = metaNode;
        }

        @Override
        public Collection<DBNNode> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBNNode> items = new ArrayList<>();
                DBNNode[] children = NavigatorUtils.getNodeChildrenFiltered(monitor, getRootNode(), false);
                if (ArrayUtils.isEmpty(children)) {
                    return items;
                }
                for (DBNNode item : children) {
                    if (monitor.isCanceled()) {
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

        private ObjectColumn objectColumn;

        public CellEditingSupport(ObjectColumn objectColumn)
        {
            super(getItemsViewer());
            this.objectColumn = objectColumn;
        }

        @Override
        protected CellEditor getCellEditor(Object element)
        {
            DBNNode object = (DBNNode) element;
            // Set cur list object to let property see it in createPropertyEditor
            setCurListObject(object);
            final ObjectPropertyDescriptor property = objectColumn.getProperty(getObjectValue(object));
            if (property != null && property.isEditable(getObjectValue(object))) {
                setFocusCell(object, objectColumn);
                return UIUtils.createPropertyEditor(getWorkbenchSite(), getControl(), property.getSource(), property);
            }
            return null;
        }

        @Override
        protected boolean canEdit(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = objectColumn.getProperty(getObjectValue(object));
            return property != null && property.isEditable(getObjectValue(object));
        }

        @Override
        protected Object getValue(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = objectColumn.getProperty(getObjectValue(object));
            if (property != null) {
                return getListPropertySource().getPropertyValue(null, getObjectValue(object), property);
            }
            return null;
        }

        @Override
        protected void setValue(Object element, Object value)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = objectColumn.getProperty(getObjectValue(object));
            try {
                if (property != null) {
                    getListPropertySource().setPropertyValue(null, getObjectValue(object), property, value);
                    if (value instanceof Boolean) {
                        // Redraw control to let it repaint checkbox
                        getItemsViewer().getControl().redraw();
                    }
                }
            } catch (Exception e) {
                UIUtils.showErrorDialog(null, "Error setting property value", "Error setting property '" + property.getId() + "' value", e);
            }
        }

    }

    private class SearcherFilter implements ISearchExecutor {

        @Override
        public boolean performSearch(String searchString, int options) {
            try {
                SearchFilter searchFilter = new SearchFilter(
                    searchString,
                    (options & SEARCH_CASE_SENSITIVE) != 0);
                getItemsViewer().setFilters(new ViewerFilter[]{searchFilter});
                return true;
            } catch (PatternSyntaxException e) {
                log.error(e.getMessage());
                return false;
            }
        }

        @Override
        public void cancelSearch() {
            getItemsViewer().setFilters(new ViewerFilter[]{});
        }
    }

    private class SearchFilter extends ViewerFilter {
        final Pattern pattern;

        public SearchFilter(String searchString, boolean caseSensitiveSearch) throws PatternSyntaxException {
            pattern = Pattern.compile(SQLUtils.makeLikePattern(searchString), caseSensitiveSearch ? 0 : Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (element instanceof DBNNode) {
                return pattern.matcher(((DBNNode) element).getName()).find();
            }
            return false;
        }
    }

    private class SearcherHighligther extends ObjectSearcher<DBNNode> {
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

        ItemColorProvider(ObjectColumn objectColumn)
        {
            super(objectColumn);
        }

        @Override
        public Font getFont(Object element)
        {
            final Object object = getObjectValue((DBNNode) element);
            return objectColumn.isNameColumn(object) && NavigatorUtils.isDefaultElement(element) ? boldFont : normalFont;
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
            if (searcher instanceof SearcherHighligther && ((SearcherHighligther) searcher).hasObject(node)) {
                return searchHighlightColor;
            }
            if (isNewObject(node)) {
                final Object objectValue = getObjectValue(node);
                final ObjectPropertyDescriptor prop = objectColumn.getProperty(getObjectValue(node));
                if (prop != null && !prop.isEditable(objectValue)) {
                    return disabledCellColor;
                }
            }
            return null;
        }
    }

}
