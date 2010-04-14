/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshableView;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Iterator;

/**
 * NavigatorUtils
 */
public class ViewUtils
{

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
/*
                    IAction defaultAction = dbmNode.getDefaultAction();
                    if (defaultAction != null) {
                        for (MenuItem item : m.getItems()) {
                            if (item.getText().equals(defaultAction.getText())) {
                                m.setDefaultItem(item);
                                break;
                            }
                        }
                    }
*/
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
            public void menuAboutToShow(IMenuManager manager)
            {
                Viewer viewer = metaModelView.getViewer();
                if (viewer == null) {
                    return;
                }
                IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
/*
                if (selection.isEmpty()) {
                    manager.add(new NewConnectionAction(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()));
                }
*/
                if (selection.size() == 1) {
                    Object selectedObject = selection.getFirstElement();
                    if (selectedObject instanceof DBSObject) {
                        DBMNode selectedNode = metaModelView.getMetaModel().getNodeByObject((DBSObject)selectedObject);
                        if (selectedNode != null) {
/*
                            IAction defaultAction = selectedNode.getDefaultAction();
                            if (defaultAction != null) {
                                initAction(defaultAction, metaModelView.getWorkbenchPart(), selection);
                                manager.add(defaultAction);
                            }
*/
                        }
                    }
                }
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
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
/*
                    RefreshTreeAction refreshAction = new RefreshTreeAction(metaModelView.getWorkbenchPart(), null);
                    refreshAction.setEnabled(true);
                    manager.add(refreshAction);
*/
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

    public static void initAction(Action actionImpl, IActionDelegate action, IWorkbenchPart part, ISelection selection)
    {
        action.selectionChanged(actionImpl, selection);

        if (action instanceof IObjectActionDelegate && part != null) {
            ((IObjectActionDelegate)action).setActivePart(actionImpl, part);
        }
    }

    public static void runAction(final IActionDelegate action, IWorkbenchPart part, ISelection selection)
    {
        if (action != null) {
            Action actionImpl = new Action() {
                @Override
                public void run() {
                    action.run(this);
                }
            };
            initAction(actionImpl, action, part, selection);
            actionImpl.run();
        }
    }

}
