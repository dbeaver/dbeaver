/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
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
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerFilterConfig;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectCreateNew;
import org.jkiss.dbeaver.ui.properties.PropertyEditorUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * ItemListControl
 */
public class ItemListControl extends NodeListControl
{
    private static final Log log = Log.getLog(ItemListControl.class);

    private static final String COLOR_NEW = "org.jkiss.dbeaver.ui.navigator.node.new.background";
    private static final String COLOR_MODIFIED = "org.jkiss.dbeaver.ui.navigator.node.modified.background";

    private final ISearchExecutor searcher;
    private final Color searchHighlightColor;

    private final Map<DBNNode, Map<String, Object>> changedProperties = new HashMap<>();
    private CommandContributionItem createObjectCommand;

    public ItemListControl(
        Composite parent,
        int style,
        final IWorkbenchSite workbenchSite,
        DBNNode node,
        DBXTreeNode metaNode)
    {
        super(parent, style, workbenchSite, node, metaNode);

        BaseThemeSettings.instance.addPropertyListener(
            UIFonts.DBEAVER_FONTS_MAIN_FONT,
            s -> super.getItemsViewer().refresh(),
            this);

        this.searcher = new SearcherFilter();
        this.searchHighlightColor = new Color(parent.getDisplay(), 170, 255, 170);
        //this.disabledCellColor = UIStyles.getDefaultTextBackground();//parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
    }

    @Override
    protected NodeSelectionProvider createSelectionProvider(ISelectionProvider selectionProvider) {
        return new NodeSelectionProvider(selectionProvider) {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                super.selectionChanged(event);
                if (createObjectCommand != null) {
                    DBNNode selectedNode = NavigatorUtils.getSelectedNode(event.getSelection());
                    boolean isEnabled = ObjectPropertyTester.canCreateObject(selectedNode, true);
                    if (isEnabled != createObjectCommand.isVisible()) {
                        createObjectCommand.setVisible(isEnabled);
                        IContributionManager toolbarManager = createObjectCommand.getParent();
                        if (toolbarManager != null) {
                            toolbarManager.update(true);
                        }
                    }
                }
            }
        };
    }

    @Override
    public void fillCustomActions(IContributionManager contributionManager) {
        IWorkbenchSite workbenchSite = getWorkbenchSite();
        // Save/revert
        if (workbenchSite instanceof MultiPageEditorSite) {
            final MultiPageEditorPart editor = ((MultiPageEditorSite) workbenchSite).getMultiPageEditor();
            if (editor instanceof EntityEditor) {
                DatabaseEditorUtils.contributeStandardEditorActions(workbenchSite, contributionManager);
                contributionManager.add(new Separator());
            }
        }
        super.fillCustomActions(contributionManager);
        final DBNNode rootNode = getRootNode();
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
                    createObjectCommand = ActionUtils.makeCommandContribution(
                        workbenchSite,
                        NavigatorCommands.CMD_OBJECT_CREATE);
                    contributionManager.add(createObjectCommand);
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
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, NavigatorCommands.CMD_OBJECT_MOVE_TOP));
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, NavigatorCommands.CMD_OBJECT_MOVE_UP));
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, NavigatorCommands.CMD_OBJECT_MOVE_DOWN));
                contributionManager.add(ActionUtils.makeCommandContribution(workbenchSite, NavigatorCommands.CMD_OBJECT_MOVE_BOTTOM));
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
        contributionManager.add(new Separator());
        if (rootNode instanceof DBNDatabaseNode dbNode && dbNode.getItemsMeta() != null) {
            contributionManager.add(new Action(
                UINavigatorMessages.obj_editor_properties_control_action_filter_setting,
                DBeaverIcons.getImageDescriptor(UIIcon.FILTER))
            {
                @Override
                public void run()
                {
                    NavigatorHandlerFilterConfig.configureFilters(getShell(), dbNode);
                }
            });
        }
        addColumnConfigAction(contributionManager);
    }

    @Override
    public void disposeControl() {
        UIUtils.dispose(searchHighlightColor);

        super.disposeControl();
    }

    @Override
    protected void setListData(Collection<DBNNode> items, boolean append, boolean forUpdate) {
        if (!append && !forUpdate) {
            changedProperties.clear();
        }
        super.setListData(items, append, forUpdate);
    }

    @Override
    protected ISearchExecutor getSearchRunner()
    {
        return searcher;
    }

    @Override
    protected LoadingJob<Collection<DBNNode>> createLoadService(boolean forUpdate)
    {
        return LoadingJob.createService(
            new ItemLoadService(getNodeMeta()),
            new ObjectsLoadVisualizer(forUpdate));
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

        ItemLoadService(DBXTreeNode metaNode)
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

                DBPDataSourceContainer ds = getDataSourceContainer();
                // If we in folder-less mode then filter children by meta
                if (ds != null && ds.getNavigatorSettings().isHideFolders()) {
                    List<DBNNode> filteredChildrenList = new ArrayList<>();
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseNode) {
                            DBXTreeNode meta = ((DBNDatabaseNode) child).getMeta();
                            if (meta.getParent() == metaNode || meta == metaNode) {
                                filteredChildrenList.add(child);
                            }
                        }
                    }
                    children = filteredChildrenList.toArray(new DBNNode[0]);
                }

                // Cache statistics
                while (parentNode instanceof DBNDatabaseFolder) {
                    parentNode = parentNode.getParentNode();
                }
                if (parentNode instanceof DBNDatabaseNode) {
                    DBSObject parentObject = DBUtils.getPublicObject(((DBNDatabaseNode) parentNode).getObject());
                    if (parentObject instanceof DBPObjectStatisticsCollector) {
                        try {
                            if (!((DBPObjectStatisticsCollector) parentObject).isStatisticsCollected()) {
                                ((DBPObjectStatisticsCollector) parentObject).collectObjectStatistics(monitor, false, false);
                            }
                        } catch (Exception e) {
                            log.debug("Error reading statistics of '" + parentObject.getName() + "'", e);
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
                return PropertyEditorUtils.createPropertyEditor(getWorkbenchSite(), getControl(), property.getSource(), property, SWT.NONE);
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
            Object objectValue = getObjectValue(object);
            final ObjectPropertyDescriptor property = objectColumn.getProperty(objectValue);
            if (property != null) {
                return getListPropertySource().getPropertyValue(null, objectValue, property, true);
            }
            return null;
        }

        @Override
        protected void setValue(Object element, Object value)
        {
            DBNNode object = (DBNNode) element;
            Object objectValue = getObjectValue(object);
            final ObjectPropertyDescriptor property = objectColumn.getProperty(objectValue);
            try {
                if (property != null) {
                    Object oldValue = getListPropertySource().getPropertyValue(null, objectValue, property, false);
                    getListPropertySource().setPropertyValue(null, objectValue, property, UIUtils.normalizePropertyValue(value));
                    Object newValue = getListPropertySource().getPropertyValue(null, objectValue, property, false);
                    if (value instanceof Boolean) {
                        // Redraw control to let it repaint checkbox
                        getItemsViewer().getControl().redraw();
                    }
                    if (!CommonUtils.equalObjects(oldValue, newValue)) {
                        Map<String, Object> propMap = changedProperties.computeIfAbsent(object, dbnNode -> new HashMap<>());
                        Object savedValue = propMap.get(property.getId());
                        if (CommonUtils.equalObjects(savedValue, newValue)) {
                            // Reset to original value
                            propMap.remove(property.getId());
                        } else if (!propMap.containsKey(property.getId())) {
                            // Save change
                            propMap.put(property.getId(), oldValue);
                        }
                    }
                    getItemsViewer().update(object, null);
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
            if (!(element instanceof DBNNode node)) {
                return BaseThemeSettings.instance.baseFont;
            }
            final Object object = getObjectValue(node);
            return objectColumn.isNameColumn(object) && DBNUtils.isDefaultElement(element) ?
                BaseThemeSettings.instance.baseFontBold : BaseThemeSettings.instance.baseFont;
        }

        @Override
        public Color getForeground(Object element)
        {
            return null;
        }

        @Override
        public Color getBackground(Object element)
        {
            if (!(element instanceof DBNNode)) {
                return null;
            }
            DBNNode node = (DBNNode) element;
            if (node.isDisposed()) {
                return null;
            }

            if (isNewObject(node)) {
                if (!isNewObject(getRootNode())) {
                    return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(COLOR_NEW);
                }
            } else {
                Map<String, Object> propMap = changedProperties.get(node);
                if (propMap != null) {
                    final Object objectValue = getObjectValue(node);
                    final ObjectPropertyDescriptor prop = objectColumn.getProperty(objectValue);
                    if (prop != null && propMap.containsKey(prop.getId())) {
                        return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(COLOR_MODIFIED);
                    }
                }
            }
//            if (searcher instanceof SearcherHighligther && ((SearcherHighligther) searcher).hasObject(node)) {
//                return searchHighlightColor;
//            }
/*
            if (isNewObject(node)) {
                final Object objectValue = getObjectValue(node);
                final ObjectPropertyDescriptor prop = objectColumn.getProperty(objectValue);
                if (prop != null && !prop.isEditable(objectValue)) {
                    return null;//disabledCellColor;
                }
            }
*/
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
