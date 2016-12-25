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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionSetActiveObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerRefresh;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Navigator utils
 */
public class NavigatorUtils {

    public static final String MB_NAVIGATOR_ADDITIONS = "navigator_additions";

    private static final Log log = Log.getLog(NavigatorUtils.class);

    public static DBNNode getSelectedNode(ISelectionProvider selectionProvider)
    {
        if (selectionProvider == null) {
            return null;
        }
        return getSelectedNode(selectionProvider.getSelection());
    }

    public static DBNNode getSelectedNode(ISelection selection)
    {
        if (selection.isEmpty()) {
            return null;
        }
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
            if (selectedObject instanceof DBNNode) {
                return (DBNNode) selectedObject;
            }
        }
        return null;
    }

    /**
     * Find selected node for specified UI element
     * @param element ui element
     * @return ndoe or null
     */
    public static DBNNode getSelectedNode(UIElement element)
    {
        ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null) {
            return NavigatorUtils.getSelectedNode(selectionProvider);
        } else {
            return null;
        }
    }

    public static DBSObject getSelectedObject(IStructuredSelection selection)
    {
        if (selection.isEmpty()) {
            return null;
        }
        return DBUtils.getFromObject(selection.getFirstElement());
    }

    public static List<DBSObject> getSelectedObjects(ISelection selection)
    {
        if (selection.isEmpty()) {
            return Collections.emptyList();
        }
        List<DBSObject> result = new ArrayList<>();
        if (selection instanceof IStructuredSelection) {
            for (Iterator iter = ((IStructuredSelection)selection).iterator(); iter.hasNext(); ) {
                DBSObject selectedObject = DBUtils.getFromObject(iter.next());
                if (selectedObject != null) {
                    result.add(selectedObject);
                }
            }
        }
        return result;
    }

    public static void addContextMenu(final IWorkbenchSite workbenchSite, final Viewer viewer)
    {
        addContextMenu(workbenchSite, viewer, null);
    }

    public static void addContextMenu(final IWorkbenchSite workbenchSite, final Viewer viewer, final IMenuListener menuListener)
    {
        MenuManager menuMgr = createContextMenu(workbenchSite, viewer, menuListener);
        if (workbenchSite instanceof IWorkbenchPartSite) {
            ((IWorkbenchPartSite)workbenchSite).registerContextMenu(menuMgr, viewer);
        } else if (workbenchSite instanceof IPageSite) {
            ((IPageSite)workbenchSite).registerContextMenu("navigatorMenu", menuMgr, viewer);
        }
    }

    public static MenuManager createContextMenu(final IWorkbenchSite workbenchSite, final Viewer viewer, IMenuListener menuListener)
    {
        final Control control = viewer.getControl();
        final MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menu.addMenuListener(new MenuListener()
        {
            @Override
            public void menuHidden(MenuEvent e)
            {
            }

            @Override
            public void menuShown(MenuEvent e)
            {
                Menu m = (Menu)e.widget;
                DBNNode node = getSelectedNode(viewer.getSelection());
                if (node != null && !node.isLocked() && node.allowsOpen()) {
                    // Dirty hack
                    // Get contribution item from menu item and check it's ID
                    for (MenuItem item : m.getItems()) {
                        Object itemData = item.getData();
                        if (itemData instanceof IContributionItem) {
                            String contribId = ((IContributionItem)itemData).getId();
                            if (contribId != null && contribId.equals(CoreCommands.CMD_OBJECT_OPEN)) {
                                m.setDefaultItem(item);
                            }
                        }
                    }
                }
            }
        });
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager)
            {
                ViewerColumnController columnController = ViewerColumnController.getFromControl(control);
                if (columnController != null && columnController.isClickOnHeader()) {
                    columnController.fillConfigMenu(manager);
                    manager.add(new Separator());
                    return;
                }
                // Fill context menu
                final IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();

                final DBNNode selectedNode = getSelectedNode(viewer);
                if (selectedNode == null || selectedNode.isLocked()) {
                    //manager.
                    return;
                }

                manager.add(new GroupMarker(MB_NAVIGATOR_ADDITIONS));

                if (workbenchSite != null) {
                    // Add "Set active object" menu
                    if (selectedNode.isPersisted() && selectedNode instanceof DBNDatabaseNode && !(selectedNode instanceof DBNDatabaseFolder) && ((DBNDatabaseNode)selectedNode).getObject() != null) {
                        final DBSObjectSelector activeContainer = DBUtils.getParentAdapter(
                            DBSObjectSelector.class, ((DBNDatabaseNode) selectedNode).getObject());
                        if (activeContainer != null && activeContainer.supportsDefaultChange()) {
                            DBSObject activeChild;
                            activeChild = activeContainer.getDefaultObject();
                            if (activeChild != ((DBNDatabaseNode)selectedNode).getObject()) {
                                DBNDatabaseNode databaseNode = (DBNDatabaseNode)selectedNode;
                                if (databaseNode.getObject() != null && (activeChild == null || activeChild.getClass() == databaseNode.getObject().getClass())) {
                                    String text = "Set Active ";// + databaseNode.getNodeType();
                                    IAction action = ActionUtils.makeAction(new NavigatorActionSetActiveObject(), workbenchSite, selection, text, null, null);

                                    manager.add(action);
                                }
                            }
                        }
                    }
                }

                manager.add(new Separator());

                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
                manager.add(new GroupMarker(IActionConstants.MB_ADDITIONS_END));

                IServiceLocator serviceLocator;
                if (workbenchSite != null) {
                    serviceLocator = workbenchSite;
                } else {
                    serviceLocator = DBeaverUI.getActiveWorkbenchWindow();
                }
                // Add properties button
                if (PreferencesUtil.hasPropertiesContributors(selection.getFirstElement())) {
                    //propertyDialogAction.selectionChanged(selection);
                    //manager.add(propertyDialogAction);
                    manager.add(ActionUtils.makeCommandContribution(serviceLocator, IWorkbenchCommandConstants.FILE_PROPERTIES));
                }

                if (selectedNode.isPersisted()) {
                    // Add refresh button
                    manager.add(ActionUtils.makeCommandContribution(serviceLocator, IWorkbenchCommandConstants.FILE_REFRESH));
                }

                manager.add(new GroupMarker(CoreCommands.GROUP_TOOLS));
            }
        });
        if (menuListener != null) {
            menuMgr.addMenuListener(menuListener);
        }

        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                menuMgr.dispose();
            }
        });
        return menuMgr;
    }

    public static void addDragAndDropSupport(final Viewer viewer)
    {
        Transfer[] types = new Transfer[] {TextTransfer.getInstance(), TreeNodeTransfer.getInstance(), DatabaseObjectTransfer.getInstance()};
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(viewer.getControl(), operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceListener() {

            private IStructuredSelection selection;

            @Override
            public void dragStart(DragSourceEvent event) {
                selection = (IStructuredSelection) viewer.getSelection();
            }

            @Override
            public void dragSetData (DragSourceEvent event) {
                if (!selection.isEmpty()) {
                    List<DBNNode> nodes = new ArrayList<>();
                    List<DBPNamedObject> objects = new ArrayList<>();
                    String lineSeparator = CommonUtils.getLineSeparator();
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> i = selection.iterator(); i.hasNext(); ) {
                        Object nextSelected = i.next();
                        if (!(nextSelected instanceof DBNNode)) {
                            continue;
                        }
                        nodes.add((DBNNode)nextSelected);
                        String nodeName;
                        if (nextSelected instanceof DBNDatabaseNode) {
                            DBSObject object = ((DBNDatabaseNode)nextSelected).getObject();
                            if (object == null) {
                                continue;
                            }
                            nodeName = DBUtils.getObjectFullName(object, DBPEvaluationContext.UI);
                            objects.add(object);
                        } else {
                            nodeName = ((DBNNode)nextSelected).getNodeName();
                        }
                        if (buf.length() > 0) {
                            buf.append(lineSeparator);
                        }
                        buf.append(nodeName);
                    }
                    if (TreeNodeTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = nodes;
                    } else if (DatabaseObjectTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = objects;
                    } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = buf.toString();
                    }
                } else {
                    if (TreeNodeTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = Collections.emptyList();
                    } else if (DatabaseObjectTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = Collections.emptyList();
                    } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = "";
                    }
                }
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
            }
        });

        DropTarget dropTarget = new DropTarget(viewer.getControl(), DND.DROP_MOVE);
        dropTarget.setTransfer(new Transfer[] {TreeNodeTransfer.getInstance()});
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event)
            {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    moveNodes(event);
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event)
            {
                event.detail = isDropSupported(event) ? DND.DROP_MOVE : DND.DROP_NONE;
                event.feedback = DND.FEEDBACK_SELECT;
            }

            private boolean isDropSupported(DropTargetEvent event)
            {
                if (TreeNodeTransfer.getInstance().isSupportedType(event.currentDataType) && event.item instanceof TreeItem) {
                    TreeItem treeItem = (TreeItem)event.item;
                    Object curObject = treeItem.getData();
                    if (curObject instanceof DBNNode) {
                        @SuppressWarnings("unchecked")
                        Collection<DBNNode> nodesToDrop = (Collection<DBNNode>) event.data;
                        if (!CommonUtils.isEmpty(nodesToDrop)) {
                            for (DBNNode node : nodesToDrop) {
                                if (!((DBNNode)curObject).supportsDrop(node)) {
                                    return false;
                                }
                            }
                            return true;
                        } else {
                            return ((DBNNode)curObject).supportsDrop(null);
                        }
                    }
                }
                return false;
            }

            private void moveNodes(DropTargetEvent event)
            {
                if (TreeNodeTransfer.getInstance().isSupportedType(event.currentDataType) && event.item instanceof TreeItem) {
                    TreeItem treeItem = (TreeItem)event.item;
                    Object curObject = treeItem.getData();
                    if (curObject instanceof DBNNode) {
                        Collection<DBNNode> nodesToDrop = TreeNodeTransfer.getInstance().getObject();
                        try {
                            ((DBNNode)curObject).dropNodes(nodesToDrop);
                        } catch (DBException e) {
                            UIUtils.showErrorDialog(viewer.getControl().getShell(), "Drop error", "Can't drop node", e);
                        }
                    }
                }
            }
        });
    }

    public static boolean isDefaultElement(Object element)
    {
        if (element instanceof DBSWrapper) {
            DBSObject object = ((DBSWrapper) element).getObject();
            DBSObjectSelector activeContainer = DBUtils.getParentAdapter(
                DBSObjectSelector.class, object);
            if (activeContainer != null) {
                return activeContainer.getDefaultObject() == object;
            }
        } else if (element instanceof DBNProject) {
            if (((DBNProject)element).getProject() == DBeaverCore.getInstance().getProjectRegistry().getActiveProject()) {
                return true;
            }
        }
        return false;
    }

    public static NavigatorViewBase getActiveNavigatorView(ExecutionEvent event) {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof NavigatorViewBase) {
            return (NavigatorViewBase) activePart;
        }
        final IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        activePart = activePage.findView(DatabaseNavigatorView.VIEW_ID);
        if (activePart instanceof NavigatorViewBase && activePage.isPartVisible(activePart)) {
            return (NavigatorViewBase) activePart;
        }
        activePart = activePage.findView(ProjectNavigatorView.VIEW_ID);
        if (activePart instanceof NavigatorViewBase && activePage.isPartVisible(activePart)) {
            return (NavigatorViewBase) activePart;
        }
        return null;
    }

    public static void filterSelection(final ISelection selection, boolean exclude)
    {
        if (selection instanceof IStructuredSelection) {
            Map<DBNDatabaseFolder, DBSObjectFilter> folders = new HashMap<>();
            for (Object item : ((IStructuredSelection)selection).toArray()) {
                if (item instanceof DBNNode) {
                    final DBNNode node = (DBNNode) item;
                    DBNDatabaseFolder folder = (DBNDatabaseFolder) node.getParentNode();
                    DBSObjectFilter nodeFilter = folders.get(folder);
                    if (nodeFilter == null) {
                        nodeFilter = folder.getNodeFilter(folder.getItemsMeta(), true);
                        if (nodeFilter == null) {
                            nodeFilter = new DBSObjectFilter();
                        }
                        folders.put(folder, nodeFilter);
                    }
                    if (exclude) {
                        nodeFilter.addExclude(node.getNodeName());
                    } else {
                        nodeFilter.addInclude(node.getNodeName());
                    }
                    nodeFilter.setEnabled(true);
                }
            }
            // Save folders
            for (Map.Entry<DBNDatabaseFolder, DBSObjectFilter> entry : folders.entrySet()) {
                entry.getKey().setNodeFilter(entry.getKey().getItemsMeta(), entry.getValue());
            }
            // Refresh all folders
            NavigatorHandlerRefresh.refreshNavigator(folders.keySet());
        }
    }

    public static boolean syncEditorWithNavigator(INavigatorModelView navigatorView, IEditorPart activeEditor) {
        if (!(activeEditor instanceof IDataSourceContainerProviderEx)) {
            return false;
        }
        IDataSourceContainerProviderEx dsProvider = (IDataSourceContainerProviderEx) activeEditor;
        Viewer navigatorViewer = navigatorView.getNavigatorViewer();
        if (navigatorViewer == null) {
            return false;
        }
        DBNNode selectedNode = getSelectedNode(navigatorViewer.getSelection());
        if (!(selectedNode instanceof DBNDatabaseNode)) {
            return false;
        }
        final DBPDataSourceContainer ds = ((DBNDatabaseNode) selectedNode).getDataSourceContainer();
        if (ds == null) {
            return false;
        }
        if (dsProvider.getDataSourceContainer() != ds) {
            dsProvider.setDataSourceContainer(ds);
        }
        // Now check if we can change default object
        DBSObject dbObject = ((DBNDatabaseNode) selectedNode).getObject();
        if (dbObject != null && dbObject.getParentObject() != null) {
            DBPObject parentObject = DBUtils.getPublicObject(dbObject.getParentObject());
            if (parentObject instanceof DBSObjectSelector) {
                DBSObjectSelector selector = (DBSObjectSelector) parentObject;
                DBSObject curDefaultObject = selector.getDefaultObject();
                if (curDefaultObject != dbObject) {
                    if (curDefaultObject != null && curDefaultObject.getClass() != dbObject.getClass()) {
                        // Wrong object type
                        return true;
                    }
                    try {
                        selector.setDefaultObject(VoidProgressMonitor.INSTANCE, dbObject);
                    } catch (Throwable e) {
                        log.debug(e);
                    }
                }
            }
        }
        return true;
    }

    public static DBNDataSource getDataSourceNode(DBNNode node) {
        for (DBNNode pn = node; pn != null; pn = pn.getParentNode()) {
            if (pn instanceof DBNDataSource) {
                return (DBNDataSource) pn;
            }
        }
        return null;
    }

    public static DBNNode[] getNodeChildrenFiltered(DBRProgressMonitor monitor, DBNNode node, boolean forTree) throws DBException {
        DBNNode[] children = node.getChildren(monitor);
        if (children != null && children.length > 0) {
            children = filterNavigableChildren(children, forTree);
        }
        return children;
    }

    public static DBNNode[] filterNavigableChildren(DBNNode[] children, boolean forTree)
    {
        if (ArrayUtils.isEmpty(children)) {
            return children;
        }
        List<DBNNode> filtered = null;
        if (forTree) {
            for (int i = 0; i < children.length; i++) {
                DBNNode node = children[i];
                if (node instanceof DBNDatabaseNode && !((DBNDatabaseNode) node).getMeta().isNavigable()) {
                    if (filtered == null) {
                        filtered = new ArrayList<>(children.length);
                        for (int k = 0; k < i; k++) {
                            filtered.add(children[k]);
                        }
                    }
                } else if (filtered != null) {
                    filtered.add(node);
                }
            }
        }
        DBNNode[] result = filtered == null ? children : filtered.toArray(new DBNNode[filtered.size()]);
        sortNodes(result);
        return result;
    }

    private static void sortNodes(DBNNode[] children)
    {
        final DBPPreferenceStore prefStore = DBeaverCore.getGlobalPreferenceStore();

        // Sort children is we have this feature on in preferences
        // and if children are not folders
        if (children.length > 0 && prefStore.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY)) {
            if (!(children[0] instanceof DBNContainer)) {
                Arrays.sort(children, NodeNameComparator.INSTANCE);
            }
        }

        if (children.length > 0 && prefStore.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)) {
            Arrays.sort(children, NodeFolderComparator.INSTANCE);
        }
    }

    public static void openNavigatorNode(Object node, IWorkbenchWindow window) {
        if (node instanceof DBNResource) {
            NavigatorHandlerObjectOpen.openResource(
                ((DBNResource) node).getResource(),
                window);
        } else if (node instanceof DBNNode && ((DBNNode) node).allowsOpen()) {
            NavigatorHandlerObjectOpen.openEntityEditor(
                (DBNNode) node,
                null,
                window);
        }
    }

    private static class NodeNameComparator implements Comparator<DBNNode> {
        static NodeNameComparator INSTANCE = new NodeNameComparator();
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            return node1.getNodeName().compareToIgnoreCase(node2.getNodeName());
        }
    }

    private static class NodeFolderComparator implements Comparator<DBNNode> {
        static NodeFolderComparator INSTANCE = new NodeFolderComparator();
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            int first = node1 instanceof DBNContainer ? -1 : 1;
            int second = node2 instanceof DBNContainer ? -1 : 1;
            return first - second;
        }
    }
}
