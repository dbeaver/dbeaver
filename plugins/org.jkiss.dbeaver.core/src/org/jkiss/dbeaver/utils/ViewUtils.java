/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.Command;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionSetActiveObject;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.util.*;

/**
 * NavigatorUtils
 */
public class ViewUtils
{
    static final Log log = LogFactory.getLog(ViewUtils.class);

    public static <T> T findView(IWorkbenchWindow workbenchWindow, Class<T> viewClass)
    {
        IViewReference[] references = workbenchWindow.getActivePage().getViewReferences();
        for (IViewReference ref : references) {
            IViewPart view = ref.getView(false);
            if (view != null && viewClass.isAssignableFrom(view.getClass())) {
                return viewClass.cast(view);
            }
        }
        return null;
    }

    public static IViewPart findView(IWorkbenchWindow workbenchWindow, String viewId)
    {
        IViewReference[] references = workbenchWindow.getActivePage().getViewReferences();
        for (IViewReference ref : references) {
            if (ref.getId().equals(viewId)) {
                return ref.getView(false);
            }
        }
        return null;
    }

    public static DBNNode getSelectedNode(ISelectionProvider viewer)
    {
        if (viewer == null) {
            return null;
        }
        return getSelectedNode(viewer.getSelection());
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

    public static String convertObjectToString(Object object)
    {
        String strValue;
        if (object instanceof DBPNamedObject) {
            strValue = ((DBPNamedObject)object).getName();
        } else {
            strValue = String.valueOf(object);
        }
        return strValue;
    }

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId)
    {
        return new CommandContributionItem(new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            CommandContributionItem.STYLE_PUSH));
    }

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId, String name, ImageDescriptor imageDescriptor)
    {
        return makeCommandContribution(serviceLocator, commandId, name, imageDescriptor, null, false);
    }

    public static ContributionItem makeActionContribution(
        IAction action,
        boolean showText)
    {
        ActionContributionItem item = new ActionContributionItem(action);
        if (showText) {
            item.setMode(ActionContributionItem.MODE_FORCE_TEXT);
        }
        return item;
    }

    public static CommandContributionItem makeCommandContribution(
        IServiceLocator serviceLocator,
        String commandId,
        String name,
        ImageDescriptor imageDescriptor,
        String toolTip,
        boolean showText)
    {
        final CommandContributionItemParameter contributionParameters = new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            null,
            imageDescriptor,
            null,
            null,
            name,
            null,
            toolTip,
            CommandContributionItem.STYLE_PUSH,
            null,
            false);
        if (showText) {
            contributionParameters.mode = CommandContributionItem.MODE_FORCE_TEXT;
        }
        return new CommandContributionItem(contributionParameters);
    }

    public static void addContextMenu(final IWorkbenchPart workbenchPart, final ISelectionProvider selectionProvider, final Control control)
    {
        addContextMenu(workbenchPart, selectionProvider, control, null);
    }

    public static void addContextMenu(final IWorkbenchPart workbenchPart, final ISelectionProvider selectionProvider, final Control control, final IMenuListener menuListener)
    {
        if (workbenchPart == null) {
            // No menu for such views (e.g. control embedded in some dialog)
            return;
        }

        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menu.addMenuListener(new MenuListener()
        {
            public void menuHidden(MenuEvent e)
            {
            }

            public void menuShown(MenuEvent e)
            {
                Menu m = (Menu)e.widget;
                DBNNode node = ViewUtils.getSelectedNode(selectionProvider.getSelection());
                if (node != null && !node.isLocked()) {
                    String defaultCommandId = node.getDefaultCommandId();

                    // Dirty hack
                    // Get contribution item from menu item and check it's ID
                    for (MenuItem item : m.getItems()) {
                        Object itemData = item.getData();
                        if (itemData instanceof IContributionItem) {
                            String contribId = ((IContributionItem)itemData).getId();
                            if (contribId != null && defaultCommandId != null && contribId.equals(defaultCommandId)) {
                                m.setDefaultItem(item);
                            }
                        }
                    }
                }
            }
        });
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(final IMenuManager manager)
            {
                // Fill context menu
                final IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();

                final DBNNode selectedNode = ViewUtils.getSelectedNode(selectionProvider);
                if (selectedNode == null || selectedNode.isLocked()) {
                    //manager.
                    return;
                }

                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

                // Add "Set active object" menu
                if (selectedNode.isPersisted() && selectedNode instanceof DBNDatabaseNode && !(selectedNode instanceof DBNDatabaseFolder) && ((DBNDatabaseNode)selectedNode).getObject() != null) {
                    final DBSEntitySelector activeContainer = DBUtils.getParentAdapter(
                        DBSEntitySelector.class, ((DBNDatabaseNode)selectedNode).getObject());
                    if (activeContainer != null && activeContainer.supportsEntitySelect()) {
                        DBSObject activeChild;
                        activeChild = activeContainer.getSelectedEntity();
                        if (activeChild != ((DBNDatabaseNode)selectedNode).getObject()) {
                            DBNDatabaseNode databaseNode = (DBNDatabaseNode)selectedNode;
                            if (databaseNode.getObject() != null && (activeChild == null || activeChild.getClass() == databaseNode.getObject().getClass())) {
                                String text = "Set Active " + databaseNode.getNodeType();
                                IAction action = makeAction(new NavigatorActionSetActiveObject(), workbenchPart, selection, text, null, null);

                                manager.add(action);
                            }
                        }
                    }
                }

                manager.add(new Separator());
                manager.add(new GroupMarker(IActionConstants.MB_ADDITIONS_END));

                // Add properties button
                if (PreferencesUtil.hasPropertiesContributors(selection.getFirstElement())) {
                    //propertyDialogAction.selectionChanged(selection);
                    //manager.add(propertyDialogAction);
                    manager.add(makeCommandContribution(workbenchPart.getSite(), IWorkbenchCommandConstants.FILE_PROPERTIES));
                }

                if (selectedNode.isPersisted()) {
                    // Add refresh button
                    manager.add(makeCommandContribution(workbenchPart.getSite(), IWorkbenchCommandConstants.FILE_REFRESH));
                }
            }
        });
        if (menuListener != null) {
            menuMgr.addMenuListener(menuListener);
        }

        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
        workbenchPart.getSite().registerContextMenu(menuMgr, selectionProvider);
    }

    public static void addDragAndDropSupport(final Viewer viewer)
    {
        Transfer[] types = new Transfer[] {TextTransfer.getInstance(), TreeNodeTransfer.getInstance(), DatabaseObjectTransfer.getInstance()};
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(viewer.getControl(), operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceListener() {

            private IStructuredSelection selection;

            public void dragStart(DragSourceEvent event) {
                selection = (IStructuredSelection) viewer.getSelection();
            }

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
                            nodeName = object instanceof DBSEntityQualified ? ((DBSEntityQualified)object).getFullQualifiedName() : object.getName();
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
            public void dragFinished(DragSourceEvent event) {
            }
        });

        DropTarget dropTarget = new DropTarget(viewer.getControl(), DND.DROP_MOVE);
        dropTarget.setTransfer(new Transfer[] {TreeNodeTransfer.getInstance()});
        dropTarget.addDropListener(new DropTargetListener() {
            public void dragEnter(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            public void dragLeave(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            public void dragOperationChanged(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            public void dragOver(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            public void drop(DropTargetEvent event)
            {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    moveNodes(event);
                }
            }

            public void dropAccept(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event)
            {
                event.detail = isDropSupported(event) ? DND.DROP_MOVE : DND.DROP_NONE;
                event.feedback = DND.FEEDBACK_NONE;
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
                        for (DBNNode node : nodesToDrop) {
                            try {
                                ((DBNNode)curObject).dropNode(node);
                            } catch (DBException e) {
                                UIUtils.showErrorDialog(viewer.getControl().getShell(), "Drop error", "Can't drop node", e);
                            }
                        }
                    }
                }
            }
        });
    }

    public static void initAction(IAction actionImpl, IActionDelegate action, IWorkbenchPart part, ISelection selection)
    {
        action.selectionChanged(actionImpl, selection);

        if (part != null) {
            if (action instanceof IObjectActionDelegate) {
                ((IObjectActionDelegate)action).setActivePart(actionImpl, part);
            } else if (action instanceof IWorkbenchWindowActionDelegate) {
                ((IWorkbenchWindowActionDelegate)action).init(part.getSite().getWorkbenchWindow());
            }
        }
    }

    public static boolean isCommandEnabled(String commandId, IWorkbenchPart part)
    {
        if (commandId != null) {
            try {
                //Command cmd = new Command();
                ICommandService commandService = (ICommandService)part.getSite().getService(ICommandService.class);
                if (commandService != null) {
                    Command command = commandService.getCommand(commandId);
                    return command != null && command.isEnabled();
                }
            } catch (Exception e) {
                log.error("Could not execute command '" + commandId + "'", e);
            }
        }
        return false;
    }

    public static void runCommand(String commandId, IWorkbenchPart part)
    {
        if (commandId != null) {
            try {
                //Command cmd = new Command();
                ICommandService commandService = (ICommandService)part.getSite().getService(ICommandService.class);
                if (commandService != null) {
                    Command command = commandService.getCommand(commandId);
                    if (command != null && command.isEnabled()) {
                        IHandlerService handlerService = (IHandlerService) part.getSite().getService(IHandlerService.class);
                        handlerService.executeCommand(commandId, null);
                    }
                }
            } catch (Exception e) {
                log.error("Could not execute command '" + commandId + "'", e);
            }
        }
    }

    public static IAction makeAction(final IActionDelegate actionDelegate, IWorkbenchPart part, ISelection selection, String text, ImageDescriptor image, String toolTip)
    {
        Action actionImpl = new Action() {
            @Override
            public void run() {
                actionDelegate.run(this);
            }
        };
        if (text != null) {
            actionImpl.setText(text);
        }
        if (image != null) {
            actionImpl.setImageDescriptor(image);
        }
        if (toolTip != null) {
            actionImpl.setToolTipText(toolTip);
        }
        initAction(actionImpl, actionDelegate, part, selection);
        return actionImpl;
    }
}
