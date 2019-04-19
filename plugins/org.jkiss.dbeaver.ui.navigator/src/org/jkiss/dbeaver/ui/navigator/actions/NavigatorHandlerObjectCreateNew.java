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
import org.eclipse.jface.action.Separator;
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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
            element.setText(typeName.toString());
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

                if (node instanceof DBSWrapper && isReadOnly(((DBSWrapper) node).getObject())) {
                    continue;
                }

                if (node instanceof DBNLocalFolder) {
                    if (!addedClasses.contains(DBPDataSourceContainer.class)) {
                        addedClasses.add(DBPDataSourceContainer.class);
                        CommandContributionItem item = makeCreateContributionItem(
                            site, DBPDataSourceContainer.class.getName(), ((DBNLocalFolder) node).getChildrenType(), UIIcon.SQL_NEW_CONNECTION);
                        createActions.add(item);
                    }
                }
                if (node instanceof DBNDatabaseNode) {
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

            if (!createActions.isEmpty() && createActions.get(createActions.size() - 1) instanceof Separator) {
                createActions.remove(createActions.size() - 1);
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
            DBPImage nodeIcon = node.getNodeIconDefault();
            if (node instanceof DBNDataSource) {
                nodeIcon = UIIcon.SQL_NEW_CONNECTION;
            }
            if (isCreateSupported(node, nodeItemClass)) {
                createActions.add(
                    makeCreateContributionItem(
                        site, nodeItemClass.getName(), node.getNodeType(), nodeIcon));
            }

            // Now add all child folders
            createActions.add(new Separator());

            List<DBXTreeNode> childNodeMetas = node.getMeta().getChildren(node);
            if (!CommonUtils.isEmpty(childNodeMetas)) {
                for (DBXTreeNode childMeta : childNodeMetas) {
                    if (childMeta instanceof DBXTreeFolder) {
                        List<DBXTreeNode> folderChildMeta = childMeta.getChildren(node);
                        if (!CommonUtils.isEmpty(folderChildMeta) && folderChildMeta.size() == 1 && folderChildMeta.get(0) instanceof DBXTreeItem) {
                            addChildNodeCreateItem(site, createActions, node, (DBXTreeItem) folderChildMeta.get(0));
                        }
                    } else if (childMeta instanceof DBXTreeItem) {
                        addChildNodeCreateItem(site, createActions, node, (DBXTreeItem) childMeta);
                    }
                }
            }
        }
    }

    private static boolean addChildNodeCreateItem(IWorkbenchPartSite site, List<IContributionItem> createActions, DBNDatabaseNode node, DBXTreeItem childMeta) {
        if (childMeta.isVirtual()) {
            return false;
        }
        Class<?> objectClass = node.getChildrenClass(childMeta);
        if (objectClass != null) {

            if (!isCreateSupported(node, objectClass)) {
                return false;
            }

            String typeName = childMeta.getNodeType(node.getDataSource());
            if (typeName != null) {
                CommandContributionItem item = makeCreateContributionItem(
                    site, objectClass.getName(), typeName, childMeta.getIcon(node));
                createActions.add(item);
                return true;
            }
        }
        return false;
    }

    private static boolean isCreateSupported(DBNDatabaseNode node, Class<?> objectClass) {
        DBEObjectMaker objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(objectClass, DBEObjectMaker.class);
        if (objectMaker == null) {
            return false;
        }
        return objectMaker.canCreateObject(node.getValueObject());
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
        if (objectIcon != null) {
            parameters.put(NavigatorCommands.PARAM_OBJECT_TYPE_ICON, objectIcon.getLocation());
        }
        params.parameters = parameters;

        return new CommandContributionItem(params);
    }

    private static boolean isReadOnly(DBSObject object)
    {
        if (object == null) {
            return true;
        }
        DBPDataSource dataSource = object.getDataSource();
        return dataSource == null || dataSource.getContainer().isConnectionReadOnly();
    }

}
