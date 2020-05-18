/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.itemlist;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectReorderer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerFilterConfig;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectCreateNew;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private ISearchExecutor searcher;
    private Color searchHighlightColor;
    //private Color disabledCellColor;
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
        //this.disabledCellColor = UIStyles.getDefaultTextBackground();//parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
        this.normalFont = parent.getFont();
        this.boldFont = UIUtils.makeBoldFont(normalFont);
    }

    @Override
    public void fillCustomActions(IContributionManager contributionManager)
    {
        super.fillCustomActions(contributionManager);
        final DBNNode rootNode = getRootNode();
        if (rootNode instanceof DBNDatabaseFolder && ((DBNDatabaseFolder) rootNode).getItemsMeta() != null) {
            contributionManager.add(new Action(
                UINavigatorMessages.obj_editor_properties_control_action_filter_setting,
                DBeaverIcons.getImageDescriptor(UIIcon.FILTER))
            {
                @Override
                public void run()
                {
                    NavigatorHandlerFilterConfig.configureFilters(getShell(), rootNode);
                }
            });
        }
        addColumnConfigAction(contributionManager);
        IWorkbenchSite workbenchSite = getWorkbenchSite();
//        if (workbenchSite != null) {
//            contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
//        }

        // Object operations

        if (rootNode instanceof DBNDatabaseFolder) {
            contributionManager.add(new Separator());
            contributionManager.add(ActionUtils.makeCommandContribution(
                workbenchSite,
                NavigatorCommands.CMD_OBJECT_OPEN));
            {
                if (ObjectPropertyTester.canCreateObject(rootNode, true)) {
                    contributionManager.add(ActionUtils.makeCommandContribution(
                        workbenchSite,
                        NavigatorCommands.CMD_OBJECT_CREATE));
                } else if (ObjectPropertyTester.canCreateObject(rootNode, false)) {
                    contributionManager.add(new Action(null, Action.AS_DROP_DOWN_MENU) {
                        {
                            setActionDefinitionId(NavigatorCommands.CMD_OBJECT_CREATE);
                        }
                        @Override
                        public void run() {
                            super.run();
                        }

                        @Override
                        public IMenuCreator getMenuCreator() {
                            return new MenuCreator(control -> {
                                List<IContributionItem> items = NavigatorHandlerObjectCreateNew.fillCreateMenuItems((IWorkbenchPartSite) workbenchSite, rootNode);
                                MenuManager menuManager = new MenuManager();
                                for (IContributionItem cc : items) {
                                    menuManager.add(cc);
                                }
                                return menuManager;
                            });
                        }
                    });
                }
            }
            contributionManager.add(ActionUtils.makeCommandContribution(
                workbenchSite,
                NavigatorCommands.CMD_OBJECT_DELETE));
        }

        // Reorder

        if (rootNode instanceof DBNDatabaseNode && rootNode.isPersisted()) {
            boolean hasReorder = false;
            List<Class<?>> childrenTypes = ((DBNDatabaseNode) rootNode).getChildrenTypes(null);
            for (Class<?> chilType : childrenTypes) {
                if (DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(chilType, DBEObjectReorderer.class) != null) {
                    hasReorder = true;
                    break;
                }
            }
            if (hasReorder) {
                contributionManager.add(new Separator());
                contributionManager.add(ActionUtils.makeCommandContribution(
                    workbenchSite,
                    NavigatorCommands.CMD_OBJECT_MOVE_UP));
                contributionManager.add(ActionUtils.makeCommandContribution(
                    workbenchSite,
                    NavigatorCommands.CMD_OBJECT_MOVE_DOWN));
            }
        }

        if (rootNode instanceof DBNDatabaseNode) {
            // Expand/collapse
            final List<DBXTreeNode> inlineMetas = collectInlineMetas((DBNDatabaseNode) rootNode, ((DBNDatabaseNode) rootNode).getMeta());
            if (!inlineMetas.isEmpty()) {
                contributionManager.add(new Separator());
                contributionManager.add(
                    ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.NAVIGATE_COLLAPSE_ALL, null, UIIcon.TREE_COLLAPSE_ALL));
                contributionManager.add(
                    ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.NAVIGATE_EXPAND_ALL, null, UIIcon.TREE_EXPAND_ALL));
            }
        }

        // Save/revert
        if (workbenchSite instanceof MultiPageEditorSite) {
            final MultiPageEditorPart editor = ((MultiPageEditorSite) workbenchSite).getMultiPageEditor();
            if (editor instanceof EntityEditor) {
                contributionManager.add(new Separator());
                DatabaseEditorUtils.contributeStandardEditorActions(workbenchSite, contributionManager);
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
        //UIUtils.dispose(disabledCellColor);
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
                DBNNode parentNode = getRootNode();
                DBNNode[] children = DBNUtils.getNodeChildrenFiltered(monitor, parentNode, false);
                if (ArrayUtils.isEmpty(children)) {
                    return items;
                }
                // Cache statistics
                while (parentNode instanceof DBNDatabaseFolder) {
                    parentNode = parentNode.getParentNode();
                }
                if (parentNode instanceof DBNDatabaseNode) {
                    DBSObject parentObject = DBUtils.getPublicObject(((DBNDatabaseNode) parentNode).getObject());
                    if (parentObject instanceof DBPObjectStatisticsCollector) {
                        if (!((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected()) {
                            ((DBPObjectStatisticsCollector) parentObject).collectObjectStatistics(monitor, false, false);
                        }
                    }
                }
                // Filter children
                for (DBNNode item : children) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    if (metaNode != null) {
                        if (!(item instanceof DBNDatabaseNode)) {
                            continue;
                        }
                        DBNDatabaseNode dbNode = (DBNDatabaseNode) item;
                        if (dbNode.getMeta() != metaNode && !dbNode.getDataSourceContainer().getNavigatorSettings().isHideFolders()) {
                            // Wrong meta. It is ok if folders are hidden
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
            if (DBNUtils.isReadOnly(object)) {
                return false;
            }
            final ObjectPropertyDescriptor property = objectColumn.getProperty(getObjectValue(object));
            return property != null && property.isEditable(getObjectValue(object));
        }

        @Override
        protected Object getValue(Object element)
        {
            DBNNode object = (DBNNode) element;
            final ObjectPropertyDescriptor property = objectColumn.getProperty(getObjectValue(object));
            if (property != null) {
                return getListPropertySource().getPropertyValue(null, getObjectValue(object), property, true);
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
                DBWorkbench.getPlatformUI().showError("Error setting property value", "Error setting property '" + property.getId() + "' value", e);
            }
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
            return objectColumn.isNameColumn(object) && DBNUtils.isDefaultElement(element) ? boldFont : normalFont;
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
//            if (searcher instanceof SearcherHighligther && ((SearcherHighligther) searcher).hasObject(node)) {
//                return searchHighlightColor;
//            }
            if (isNewObject(node)) {
                final Object objectValue = getObjectValue(node);
                final ObjectPropertyDescriptor prop = objectColumn.getProperty(getObjectValue(node));
                if (prop != null && !prop.isEditable(objectValue)) {
                    return null;//disabledCellColor;
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
