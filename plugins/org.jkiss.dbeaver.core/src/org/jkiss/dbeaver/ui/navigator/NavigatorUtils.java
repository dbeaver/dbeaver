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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionSetActiveObject;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Navigator utils
 */
public class NavigatorUtils {

    public static final String MB_NAVIGATOR_ADDITIONS = "navigator_additions";
    public static final String NAVIGATOR_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.navigator";

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
        Object selectedObject = selection.getFirstElement();
        if (selectedObject instanceof DBSWrapper) {
            return ((DBSWrapper) selectedObject).getObject();
        } else if (selectedObject instanceof DBSObject) {
            return (DBSObject) selectedObject;
        } else {
            return null;
        }
    }

    public static List<DBSObject> getSelectedObjects(IStructuredSelection selection)
    {
        if (selection.isEmpty()) {
            return Collections.emptyList();
        }
        List<DBSObject> result = new ArrayList<DBSObject>();
        for (Iterator iter = selection.iterator(); iter.hasNext(); ) {
            Object selectedObject = iter.next();
            if (selectedObject instanceof DBSWrapper) {
                result.add(((DBSWrapper) selectedObject).getObject());
            } else if (selectedObject instanceof DBSObject) {
                result.add((DBSObject)selectedObject);
            }
        }
        return result;
    }

    public static void addContextMenu(final IWorkbenchSite workbenchSite, final ISelectionProvider selectionProvider, final Control control)
    {
        addContextMenu(workbenchSite, selectionProvider, control, null);
    }

    public static void addContextMenu(final IWorkbenchSite workbenchSite, final ISelectionProvider selectionProvider, final Control control, final IMenuListener menuListener)
    {
        MenuManager menuMgr = createContextMenu(workbenchSite, selectionProvider, control, menuListener);
        if (workbenchSite instanceof IWorkbenchPartSite) {
            ((IWorkbenchPartSite)workbenchSite).registerContextMenu(menuMgr, selectionProvider);
        } else if (workbenchSite instanceof IPageSite) {
            ((IPageSite)workbenchSite).registerContextMenu("navigatorMenu", menuMgr, selectionProvider);
        }
    }

    public static MenuManager createContextMenu(final IWorkbenchSite workbenchSite, final ISelectionProvider selectionProvider, final Control control, IMenuListener menuListener)
    {
        MenuManager menuMgr = new MenuManager();
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
                DBNNode node = getSelectedNode(selectionProvider.getSelection());
                if (node != null && !node.isLocked() && node.allowsOpen()) {
                    // Dirty hack
                    // Get contribution item from menu item and check it's ID
                    for (MenuItem item : m.getItems()) {
                        Object itemData = item.getData();
                        if (itemData instanceof IContributionItem) {
                            String contribId = ((IContributionItem)itemData).getId();
                            if (contribId != null && contribId.equals(ICommandIds.CMD_OBJECT_OPEN)) {
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
                final IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();

                final DBNNode selectedNode = getSelectedNode(selectionProvider);
                if (selectedNode == null || selectedNode.isLocked()) {
                    //manager.
                    return;
                }

                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
                manager.add(new GroupMarker(MB_NAVIGATOR_ADDITIONS));

                if (workbenchSite != null) {
                    // Add "Set active object" menu
                    if (selectedNode.isPersisted() && selectedNode instanceof DBNDatabaseNode && !(selectedNode instanceof DBNDatabaseFolder) && ((DBNDatabaseNode)selectedNode).getObject() != null) {
                        final DBSObjectSelector activeContainer = DBUtils.getParentAdapter(
                            DBSObjectSelector.class, ((DBNDatabaseNode) selectedNode).getObject());
                        if (activeContainer != null && activeContainer.supportsObjectSelect()) {
                            DBSObject activeChild;
                            activeChild = activeContainer.getSelectedObject();
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

                manager.add(new GroupMarker(ICommandIds.GROUP_TOOLS));
            }
        });
        if (menuListener != null) {
            menuMgr.addMenuListener(menuListener);
        }

        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
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
                    List<DBNNode> nodes = new ArrayList<DBNNode>();
                    List<DBPNamedObject> objects = new ArrayList<DBPNamedObject>();
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
                            nodeName = DBUtils.getObjectFullName(object);
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
                return activeContainer.getSelectedObject() == object;
            }
        } else if (element instanceof DBNProject) {
            if (((DBNProject)element).getProject() == DBeaverCore.getInstance().getProjectRegistry().getActiveProject()) {
                return true;
            }
        }
        return false;
    }

}
