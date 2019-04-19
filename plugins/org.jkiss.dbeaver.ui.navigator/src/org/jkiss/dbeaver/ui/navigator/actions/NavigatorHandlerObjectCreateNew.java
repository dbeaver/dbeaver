/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class NavigatorHandlerObjectCreateNew extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String objectType = event.getParameter(NavigatorCommands.PARAM_OBJECT_TYPE);

        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node != null) {
            createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), node, null);
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        Object typeName = parameters.get(NavigatorCommands.PARAM_OBJECT_TYPE_NAME);
        Object objectIcon = parameters.get(NavigatorCommands.PARAM_OBJECT_TYPE_ICON);
        if (typeName != null) {
            element.setText(NLS.bind(UINavigatorMessages.actions_navigator_create_new, typeName));
        } else {
            element.setText(NLS.bind(UINavigatorMessages.actions_navigator_create_new, getObjectTypeName(element)));
        }
        if (objectIcon != null) {
            element.setIcon(DBeaverIcons.getImageDescriptor(new DBIcon(objectIcon.toString())));
        } else {
            DBPImage image = getObjectTypeIcon(element);
            if (image == null) {
                image = DBIcon.TYPE_OBJECT;
            }
            element.setIcon(DBeaverIcons.getImageDescriptor(image));
        }
    }

    public static String getObjectTypeName(UIElement element) {
        DBNNode node = NavigatorUtils.getSelectedNode(element);
        if (node != null) {
            if (node instanceof DBNContainer && !(node instanceof DBNDataSource)) {
                return ((DBNContainer)node).getChildrenType();
            } else {
                return node.getNodeType();
            }
        }
        return null;
    }

    public static DBPImage getObjectTypeIcon(UIElement element) {
        DBNNode node = NavigatorUtils.getSelectedNode(element);
        if (node != null) {
            if (node instanceof DBNDatabaseNode && node.getParentNode() instanceof DBNDatabaseFolder) {
                node = node.getParentNode();
            }
            if (node instanceof DBNDataSource) {
                return UIIcon.SQL_CONNECT;
            } else if (node instanceof DBNDatabaseFolder) {
                final List<DBXTreeNode> metaChildren = ((DBNDatabaseFolder)node).getMeta().getChildren(node);
                if (!CommonUtils.isEmpty(metaChildren)) {
                    return metaChildren.get(0).getIcon(null);
                }
                return null;
            } else {
                return node.getNodeIconDefault();
            }
        }
        return null;
    }

    public static class MenuCreateContributor extends CompoundContributionItem {

        private static final IContributionItem[] EMPTY_MENU = new IContributionItem[0];

        @Override
        protected IContributionItem[] getContributionItems() {

            IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
            IWorkbenchPart activePart = activePage.getActivePart();
            if (activePart == null) {
                return EMPTY_MENU;
            }
            IWorkbenchPartSite site = activePart.getSite();
            final ISelectionProvider selectionProvider = site.getSelectionProvider();
            if (selectionProvider == null) {
                return EMPTY_MENU;
            }
            ISelection selection = selectionProvider.getSelection();
            if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
                return EMPTY_MENU;
            }

            List<IContributionItem> createActions = new ArrayList<>();

            Set<Class<?>> addedClasses = new HashSet<>();
            for (Object element : ((IStructuredSelection) selection).toArray()) {
                if (!(element instanceof DBNNode)) {
                    continue;
                }
                DBNNode node = (DBNNode) element;

                if (node instanceof DBNDataSource || node instanceof DBNLocalFolder) {
                    if (!addedClasses.contains(DBPDataSourceContainer.class)) {
                        addedClasses.add(DBPDataSourceContainer.class);

                        CommandContributionItem item;
                        if (node instanceof DBNLocalFolder) {
                            item = makeCreateContributionItem(
                                site, DBPDataSourceContainer.class.getName(), ((DBNLocalFolder) node).getChildrenType(), UIIcon.SQL_NEW_CONNECTION);
                        } else {
                            item = makeCreateContributionItem(
                                site, DBPDataSourceContainer.class.getName(), node.getNodeType(), UIIcon.SQL_NEW_CONNECTION);
                        }
                        createActions.add(item);
                    }
                } else if (node instanceof DBNDatabaseNode) {
                    addDatabaseNodeCreateItems(site, createActions, (DBNDatabaseNode) node);
                }

                if (node instanceof DBNLocalFolder || node instanceof DBNDataSource) {
                    if (!addedClasses.contains(DBNLocalFolder.class)) {
                        addedClasses.add(DBNLocalFolder.class);
                        createActions.add(ActionUtils.makeCommandContribution(site, NavigatorCommands.CMD_CREATE_LOCAL_FOLDER));
                    }
                } else if (node instanceof DBNResource) {

                }

            }

            return createActions.toArray(new IContributionItem[0]);
        }
    }

    private static void addDatabaseNodeCreateItems(IWorkbenchPartSite site, List<IContributionItem> createActions, DBNDatabaseNode node) {
        if (node instanceof DBNDatabaseFolder) {
            final List<DBXTreeNode> metaChildren = ((DBNDatabaseFolder) node).getMeta().getChildren(node);
            if (!CommonUtils.isEmpty(metaChildren)) {
                Class<?> nodeClass = ((DBNContainer) node).getChildrenClass();
                String nodeType = metaChildren.get(0).getChildrenType(node.getDataSource());
                DBPImage nodeIcon = metaChildren.get(0).getIcon(node);
                if (nodeClass != null && nodeType != null) {
                    CommandContributionItem item = makeCreateContributionItem(
                        site, nodeClass.getName(), nodeType, nodeIcon);
                    createActions.add(item);
                }
            }
        } else {
            Class<?> nodeItemClass = node.getObject().getClass();
            CommandContributionItem item = makeCreateContributionItem(
                site, nodeItemClass.getName(), node.getNodeType(), node.getNodeIconDefault());
            createActions.add(item);
            // Now add all child folders
        }
    }

    private static CommandContributionItem makeCreateContributionItem(IWorkbenchPartSite site, String objectType, String objectTypeName, DBPImage objectIcon) {
        CommandContributionItemParameter params = new CommandContributionItemParameter(
            site,
            NavigatorCommands.CMD_OBJECT_CREATE,
            NavigatorCommands.CMD_OBJECT_CREATE,
            CommandContributionItem.STYLE_PUSH
        );
        Map<String, String> parameters = new HashMap<>();
        parameters.put(NavigatorCommands.PARAM_OBJECT_TYPE, objectType);
        parameters.put(NavigatorCommands.PARAM_OBJECT_TYPE_NAME, objectTypeName);
        parameters.put(NavigatorCommands.PARAM_OBJECT_TYPE_ICON, objectIcon.getLocation());
        params.parameters = parameters;

        return new CommandContributionItem(params);
    }

}
