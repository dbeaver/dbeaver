/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshableView;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.actions.SetActiveObjectAction;

import java.util.Iterator;
import java.lang.reflect.InvocationTargetException;

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

    public static DBMNode getSelectedNode(IMetaModelView metaModelView)
    {
        Viewer viewer = metaModelView.getViewer();
        if (viewer == null) {
            return null;
        }
        IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        Object selectedObject = selection.getFirstElement();
        if (selectedObject instanceof DBSObject) {
            return metaModelView.getMetaModel().getNodeByObject((DBSObject)selectedObject);
        }
        return null;
    }

    public static void addContextMenu(final IMetaModelView metaModelView)
    {
        final PropertyDialogAction propertyDialogAction = new PropertyDialogAction(
            metaModelView.getWorkbenchPart().getSite(),
            metaModelView.getViewer());

        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(metaModelView.getViewer().getControl());
        menu.addMenuListener(new MenuListener()
        {
            public void menuHidden(MenuEvent e)
            {
            }

            public void menuShown(MenuEvent e)
            {
                Menu m = (Menu)e.widget;
                DBMNode dbmNode = ViewUtils.getSelectedNode(metaModelView);
                if (dbmNode != null) {
                    Class<? extends IActionDelegate> defaultActionClass = dbmNode.getDefaultAction();
                    if (defaultActionClass != null) {
                        // Dirty hack
                        // Get contribution item from menu item and check it's ID
                        // In DBeaver all action's IDs are equals to action class names
                        // So we can compare it with our default action's class
                        for (MenuItem item : m.getItems()) {
                            Object itemData = item.getData();
                            if (itemData instanceof IContributionItem) {
                                String contribId = ((IContributionItem)itemData).getId();
                                if (contribId != null && contribId.equals(defaultActionClass.getName())) {
                                    m.setDefaultItem(item);
                                }
                            }
                        }
                    }
                }
            }
        });
        menu.addDisposeListener(new DisposeListener()
        {
            public void widgetDisposed(DisposeEvent e)
            {
                propertyDialogAction.dispose();
            }
        });
        menuMgr.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(final IMenuManager manager)
            {
                // Fill context menu
                Viewer viewer = metaModelView.getViewer();
                if (viewer == null) {
                    return;
                }
                final IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
/*
                if (selection.isEmpty()) {
                    manager.add(new NewConnectionAction(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()));
                }
*/
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

                // Add "Set active object" menu
                final DBMNode dbmNode = ViewUtils.getSelectedNode(metaModelView);
                if (dbmNode instanceof DBMTreeNode && dbmNode.getObject() != null) {
                    final DBSEntitySelector activeContainer = DBUtils.queryParentInterface(
                        DBSEntitySelector.class, dbmNode.getObject());
                    if (activeContainer != null && activeContainer.supportsActiveChildChange()) {
                        try {
                            // Extract active child with void monitor
                            // Otherwise context menu will be broken by GUI used by progress service
                            // TODO: do something with that and use real progress monitor
                            new DBRRunnableWithProgress() {
                                public void run(DBRProgressMonitor monitor)
                                    throws InvocationTargetException
                                {
                                    DBSObject activeChild;
                                    try {
                                        activeChild = activeContainer.getActiveChild(monitor);
                                    }
                                    catch (DBException e) {
                                        throw new InvocationTargetException(e);
                                    }
                                    if (activeChild != dbmNode.getObject()) {
                                        DBMTreeNode treeNode = (DBMTreeNode)dbmNode;
                                        DBXTreeNode nodeMeta = treeNode.getMeta();
                                        String text = "Set active";
                                        if (nodeMeta instanceof DBXTreeItem) {
                                            DBXTreeItem itemMeta = (DBXTreeItem)nodeMeta;
                                            text += " " + itemMeta.getPath();
                                        }
                                        IAction action = makeAction(new SetActiveObjectAction(), metaModelView.getWorkbenchPart(), selection, text, null, null);

                                        manager.add(action);
                                    }
                                }
                            }.run(VoidProgressMonitor.INSTANCE);
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
                        propertyDialogAction.selectionChanged(selection);
                        manager.add(propertyDialogAction);
                    }
                }

                // Add refresh button
                if (!selection.isEmpty() && metaModelView instanceof IRefreshableView) {
                    IRefreshableView rv = (IRefreshableView)metaModelView;
                    if (rv.getRefreshAction() != null) {
                        manager.add(rv.getRefreshAction());
                    }
                }
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        if (metaModelView.getViewer() != null) {
            metaModelView.getViewer().getControl().setMenu(menu);
            metaModelView.getWorkbenchPart().getSite().registerContextMenu(menuMgr, metaModelView.getViewer());
        }
    }

    public static void addDragAndDropSupport(final IMetaModelView metaModelView)
    {
        Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(metaModelView.getViewer().getControl(), operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceListener() {

            private IStructuredSelection selection;

            public void dragStart(DragSourceEvent event) {
                selection = (IStructuredSelection) metaModelView.getViewer().getSelection();
            }
            public void dragSetData (DragSourceEvent event) {
                if (!selection.isEmpty()) {
                    String lineSeparator = CommonUtils.getLineSeparator();
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> i = selection.iterator(); i.hasNext(); ) {
                        DBSObject nextSelected = (DBSObject)i.next();
                        if (buf.length() > 0) {
                            buf.append(lineSeparator);
                        }
                        buf.append(nextSelected.getName());
                    }
                    event.data = buf.toString();
                } else {
                    event.data = "";
                }
            }
            public void dragFinished(DragSourceEvent event) {
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

    public static void runAction(Class<? extends IActionDelegate> actionClass, IWorkbenchPart part, ISelection selection)
    {
        if (actionClass != null) {
            try {
                final IActionDelegate actionDelegate = actionClass.newInstance();
                IAction actionImpl = makeAction(actionDelegate, part, selection, actionDelegate.getClass().getName(), null, null);
                actionImpl.run();
            } catch (InstantiationException e) {
                log.error("Could not instantiate action delegate", e);
            } catch (IllegalAccessException e) {
                log.error(e);
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
