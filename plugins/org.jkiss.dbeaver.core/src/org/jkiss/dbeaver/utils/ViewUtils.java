/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorActionSetActiveObject;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * NavigatorUtils
 */
public class ViewUtils
{
    static final Log log = LogFactory.getLog(ViewUtils.class);
    //public static final String MENU_ID = "org.jkiss.dbeaver.core.navigationMenu";
    public static final String MB_ADDITIONS_END = "additions_end";

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

    public static DBNNode getSelectedNode(Viewer viewer)
    {
        if (viewer == null) {
            return null;
        }
        return getSelectedNode((IStructuredSelection)viewer.getSelection());
    }

    public static DBNNode getSelectedNode(IStructuredSelection selection)
    {
        if (selection.isEmpty()) {
            return null;
        }
        Object selectedObject = selection.getFirstElement();
        if (selectedObject instanceof DBNNode) {
            return (DBNNode) selectedObject;
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
        return new CommandContributionItem(new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            null,
            imageDescriptor,
            null,
            null,
            name,
            null,
            null,
            CommandContributionItem.STYLE_PUSH,
            null,
            false));
    }

    public static void addContextMenu(final IWorkbenchPart workbenchPart, final Viewer viewer)
    {
        if (workbenchPart == null) {
            // No menu for such views (e.g. control embedded in some dialog)
            return;
        }

        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        menu.addMenuListener(new MenuListener()
        {
            public void menuHidden(MenuEvent e)
            {
            }

            public void menuShown(MenuEvent e)
            {
                Menu m = (Menu)e.widget;
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                DBNNode node = ViewUtils.getSelectedNode(selection);
                boolean multipleSelection = selection.size() > 1;
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
                            if (ICommandIds.CMD_OBJECT_OPEN.equals(contribId)) {
                                if (node instanceof DBNDatabaseNode) {
                                    EntityManagerDescriptor objectManager = DBeaverCore.getInstance().getEditorsRegistry().getEntityManager(((DBNDatabaseNode)node).getObject().getClass());
                                    String actionName = objectManager == null ? "View" : "Edit";
                                    if (multipleSelection) {
                                        item.setText(actionName + " objects");
                                    } else if (node instanceof DBNDatabaseNode) {
                                        item.setText(actionName + " " + ((DBNDatabaseNode)node).getMeta().getLabel());
                                    }
                                }
                            } else if (ICommandIds.CMD_OBJECT_CREATE.equals(contribId)) {
                                String objectName = "";
                                if (node instanceof DBNContainer) {
                                    objectName = ((DBNContainer)node).getItemsLabel();
                                } else if (node instanceof DBNDatabaseNode) {
                                    objectName = ((DBNDatabaseNode)node).getMeta().getLabel();
                                } else {
                                    objectName = node.getNodeName();
                                }
                                item.setText("Create new " + objectName);
                            } else if (ICommandIds.CMD_OBJECT_DELETE.equals(contribId) || IWorkbenchCommandConstants.EDIT_DELETE.equals(contribId)) {
                                if (multipleSelection) {
                                    item.setText("Delete objects");
                                } else if (node instanceof DBNDatabaseNode) {
                                    item.setText("Delete " + ((DBNDatabaseNode)node).getMeta().getLabel());
                                } else {
                                    item.setText("Delete '" + node.getNodeName() + "'");
                                }
                            }
                        }
                    }
                }
            }
        });
        menuMgr.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(final IMenuManager manager)
            {
                // Fill context menu
                final IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();

                final DBNNode selectedNode = ViewUtils.getSelectedNode(viewer);
                if (selectedNode == null || selectedNode.isLocked()) {
                    //manager.
                    return;
                }

                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

                // Add "Set active object" menu
                if (selectedNode instanceof DBNDatabaseNode && !(selectedNode instanceof DBNDatabaseFolder) && ((DBNDatabaseNode)selectedNode).getObject() != null) {
                    final DBSEntitySelector activeContainer = DBUtils.getParentAdapter(
                        DBSEntitySelector.class, ((DBNDatabaseNode)selectedNode).getObject());
                    if (activeContainer != null && activeContainer.supportsActiveChildChange()) {
                        try {
                            // Extract active child with void monitor
                            // Otherwise context menu will be broken by GUI used by progress service
                            // TODO: do something with that and use real progress monitor
                            DBSObject activeChild;
                            try {
                                activeChild = activeContainer.getActiveChild(VoidProgressMonitor.INSTANCE);
                            }
                            catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                            if (activeChild != ((DBNDatabaseNode)selectedNode).getObject() && activeChild != null) {
                                DBNDatabaseNode databaseNode = (DBNDatabaseNode)selectedNode;
                                if (databaseNode.getObject() != null && activeChild.getClass() == databaseNode.getObject().getClass()) {
                                    DBXTreeNode nodeMeta = databaseNode.getMeta();
                                    String text = "Set Active";
                                    if (nodeMeta instanceof DBXTreeItem) {
                                        DBXTreeItem itemMeta = (DBXTreeItem)nodeMeta;
                                        text += " " + itemMeta.getLabel();
                                    }
                                    IAction action = makeAction(new NavigatorActionSetActiveObject(), workbenchPart, selection, text, null, null);

                                    manager.add(action);
                                }
                            }
                        } catch (InvocationTargetException e) {
                            log.warn(e.getTargetException());
                        }
                    }
                }

                manager.add(new Separator());
                manager.add(new GroupMarker(MB_ADDITIONS_END));

                // Add properties button
                if (!selection.isEmpty()) {
                    if (PreferencesUtil.hasPropertiesContributors(selection.getFirstElement())) {
                        //propertyDialogAction.selectionChanged(selection);
                        //manager.add(propertyDialogAction);
                        manager.add(makeCommandContribution(workbenchPart.getSite(), IWorkbenchCommandConstants.FILE_PROPERTIES));
                    }
                }

                // Add refresh button
                manager.add(makeCommandContribution(workbenchPart.getSite(), IWorkbenchCommandConstants.FILE_REFRESH));
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        viewer.getControl().setMenu(menu);
        workbenchPart.getSite().registerContextMenu(menuMgr, viewer);
    }

    public static void addDragAndDropSupport(final Viewer viewer)
    {
        Transfer[] types = new Transfer[] {TextTransfer.getInstance(), TreeNodeTransfer.getInstance()};
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
                    String lineSeparator = CommonUtils.getLineSeparator();
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> i = selection.iterator(); i.hasNext(); ) {
                        Object nextSelected = i.next();
                        if (!(nextSelected instanceof DBNDatabaseNode)) {
                            continue;
                        }
                        nodes.add((DBNNode)nextSelected);
                        DBSObject object = ((DBNDatabaseNode)nextSelected).getObject();
                        if (object == null) {
                            continue;
                        }
                        if (buf.length() > 0) {
                            buf.append(lineSeparator);
                        }
                        buf.append(object instanceof DBSEntityQualified ? ((DBSEntityQualified)object).getFullQualifiedName() : object.getName());
                    }
                    if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = buf.toString();
                    } else {
                        event.data = nodes;
                    }
                } else {
                    if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        event.data = "";
                    } else {
                        event.data = new ArrayList<DBNNode>();
                    }
                }
            }
            public void dragFinished(DragSourceEvent event) {
            }
        });

        DropTarget dropTarget = new DropTarget(viewer.getControl(), DND.DROP_COPY);
        dropTarget.setTransfer(new Transfer[] {TreeNodeTransfer.getInstance()});
        dropTarget.addDropListener(new DropTargetListener() {
            public void dragEnter(DropTargetEvent event)
            {
                event.detail = DND.DROP_COPY;
                event.feedback = DND.FEEDBACK_NONE;
            }

            public void dragLeave(DropTargetEvent event)
            {
                event.detail = DND.DROP_COPY;
                event.feedback = DND.FEEDBACK_NONE;
            }

            public void dragOperationChanged(DropTargetEvent event)
            {
                event.detail = DND.DROP_COPY;
                event.feedback = DND.FEEDBACK_NONE;
            }

            public void dragOver(DropTargetEvent event)
            {
                event.detail = DND.DROP_COPY;
                event.feedback = DND.FEEDBACK_NONE;
            }

            public void drop(DropTargetEvent event)
            {
                int x = 0;
            }

            public void dropAccept(DropTargetEvent event)
            {
                event.detail = DND.DROP_COPY;
                event.feedback = DND.FEEDBACK_NONE;
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

    public static void runCommand(String commandId, IWorkbenchPart part)
    {
        if (commandId != null) {
            IHandlerService handlerService = (IHandlerService) part.getSite().getService(IHandlerService.class);
            try {
                handlerService.executeCommand(commandId, null);
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
