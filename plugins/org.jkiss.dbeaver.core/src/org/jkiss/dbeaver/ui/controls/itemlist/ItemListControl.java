/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.itemlist;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerFilterConfig;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
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
        final DBNNode rootNode = getRootNode();
        if (rootNode instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) rootNode).getItemsMeta() != null) {
            contributionManager.add(new Action(
                "Filter settings",
                DBeaverIcons.getImageDescriptor(UIIcon.FILTER))
            {
                @Override
                public void run()
                {
                    NavigatorHandlerFilterConfig.configureFilters(getShell(), rootNode);
                }
            });
        }
        {
            Action configColumnsAction = new Action(
                    "Configure columns",
                    DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
                @Override
                public void run() {
                    columnController.configureColumns();
                }
            };
            configColumnsAction.setDescription("Configure columns visibility");
            contributionManager.add(configColumnsAction);
        }
        IWorkbenchSite workbenchSite = getWorkbenchSite();
        if (workbenchSite != null) {
            contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
        }


        if (rootNode instanceof DBNDatabaseNode) {
            contributionManager.add(new Separator());
            contributionManager.add(ActionUtils.makeCommandContribution(
                workbenchSite,
                CoreCommands.CMD_OBJECT_OPEN));
            contributionManager.add(ActionUtils.makeCommandContribution(
                workbenchSite,
                CoreCommands.CMD_OBJECT_CREATE));
            contributionManager.add(ActionUtils.makeCommandContribution(
                workbenchSite,
                CoreCommands.CMD_OBJECT_DELETE));
        }

        if (rootNode instanceof DBNDatabaseNode && rootNode.isPersisted()) {
            boolean hasReorder = false;
            List<Class<?>> childrenTypes = ((DBNDatabaseNode) rootNode).getChildrenTypes(null);
            for (Class<?> chilType : childrenTypes) {
                if (EntityEditorsRegistry.getInstance().getObjectManager(chilType, DBEObjectReorderer.class) != null) {
                    hasReorder = true;
                    break;
                }
            }
            if (hasReorder) {
                contributionManager.add(new Separator());
                contributionManager.add(ActionUtils.makeCommandContribution(
                    workbenchSite,
                    CoreCommands.CMD_OBJECT_MOVE_UP));
                contributionManager.add(ActionUtils.makeCommandContribution(
                    workbenchSite,
                    CoreCommands.CMD_OBJECT_MOVE_DOWN));
            }
        }

        if (workbenchSite instanceof MultiPageEditorSite) {
            final MultiPageEditorPart editor = ((MultiPageEditorSite) workbenchSite).getMultiPageEditor();
            if (editor instanceof EntityEditor) {
                contributionManager.add(new Separator());
                contributionManager.add(ActionUtils.makeCommandContribution(
                    workbenchSite,
                    IWorkbenchCommandConstants.FILE_SAVE,
                    null,
                    UIIcon.SAVE,
                    null,
                    true));
                contributionManager.add(ActionUtils.makeCommandContribution(
                    workbenchSite,
                    IWorkbenchCommandConstants.FILE_REVERT,
                    null,
                    UIIcon.RESET,
                    null,
                    true));
            }
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
                return UIUtils.createPropertyEditor(getWorkbenchSite(), getControl(), property.getSource(), property, SWT.NONE);
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
                DBUserInterface.getInstance().showError("Error setting property value", "Error setting property '" + property.getId() + "' value", e);
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

    private class PackColumnsAction extends Action {
        public PackColumnsAction() {
            super("Pack columns", DBeaverIcons.getImageDescriptor(UIIcon.TREE_EXPAND));
        }

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
    }
}
