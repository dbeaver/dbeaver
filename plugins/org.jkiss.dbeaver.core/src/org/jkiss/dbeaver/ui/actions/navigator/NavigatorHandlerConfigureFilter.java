/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.EditObjectFilterDialog;

import java.util.Collections;
import java.util.Map;

public class NavigatorHandlerConfigureFilter extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDatabaseFolder) {
            final DBNDatabaseFolder folder = (DBNDatabaseFolder) node;
            DBXTreeItem itemsMeta = folder.getItemsMeta();
            if (itemsMeta != null) {
                DBSObjectFilter objectFilter = folder.getNodeFilter(itemsMeta, true);
                if (objectFilter == null) {
                    objectFilter = new DBSObjectFilter();
                }
                boolean globalFilter = folder.getValueObject() instanceof DBPDataSource;
                String parentName = "?";
                if (folder.getValueObject() instanceof DBSObject) {
                    parentName = ((DBSObject) folder.getValueObject()).getName();
                }
                EditObjectFilterDialog dialog = new EditObjectFilterDialog(
                    HandlerUtil.getActiveShell(event),
                    globalFilter ? "All " + node.getNodeType() : node.getNodeType() + " of " + parentName,
                    objectFilter,
                    globalFilter);
                switch (dialog.open()) {
                    case IDialogConstants.OK_ID:
                        folder.setNodeFilter(itemsMeta, dialog.getFilter());
                        NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(folder));
                        break;
                    case EditObjectFilterDialog.SHOW_GLOBAL_FILTERS_ID:
                        objectFilter = folder.getDataSource().getContainer().getObjectFilter(folder.getChildrenClass(), null, false);
                        dialog = new EditObjectFilterDialog(
                            HandlerUtil.getActiveShell(event),
                            "All " + node.getNodeType(),
                            objectFilter != null  ?objectFilter : new DBSObjectFilter(),
                            true);
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            folder.setNodeFilter(itemsMeta, dialog.getFilter());
                            NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(folder));
                        }
                        break;
                }
            }
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator().getService(IWorkbenchPartSite.class);
        if (partSite != null) {
            DBNNode node = NavigatorUtils.getSelectedNode(partSite.getSelectionProvider());
            if (node != null) {
                element.setText("Filter " + node.getNodeType() + " ...");
            }
        }
    }

}