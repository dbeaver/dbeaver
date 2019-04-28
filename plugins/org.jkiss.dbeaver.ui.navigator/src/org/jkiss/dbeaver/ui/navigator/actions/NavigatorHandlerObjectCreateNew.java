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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProjectManager;
import org.jkiss.dbeaver.model.app.DBPResourceCreator;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigatorHandlerObjectCreateNew extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    private static final Log log = Log.getLog(NavigatorHandlerObjectCreateNew.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String objectType = event.getParameter(NavigatorCommands.PARAM_OBJECT_TYPE);
        boolean isFolder = CommonUtils.toBoolean(event.getParameter(NavigatorCommands.PARAM_OBJECT_TYPE_FOLDER));

        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node != null) {
            Class<?> newObjectType = null;
            if (objectType != null) {
                if (node instanceof DBNDatabaseNode) {
                    newObjectType = ((DBNDatabaseNode) node).getMeta().getSource().getObjectClass(objectType);
                } else {
                    try {
                        newObjectType = Class.forName(objectType);
                    } catch (ClassNotFoundException e) {
                        log.error("Error detecting new object type " + objectType, e);
                    }
                }
            }
            createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), node, newObjectType, null, isFolder);
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
            DBNNode node = NavigatorUtils.getSelectedNode(site.getSelectionProvider());

            List<IContributionItem> createActions = new ArrayList<>();

            if (node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases) {
                CommandContributionItem item = makeCreateContributionItem(
                    site, DBPDataSourceContainer.class.getName(), ModelMessages.model_navigator_Connection, UIIcon.SQL_NEW_CONNECTION, false);
                createActions.add(item);
            }
            if (node instanceof DBNDatabaseNode) {
                addDatabaseNodeCreateItems(site, createActions, (DBNDatabaseNode) node);
            }

            if (node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases || node instanceof DBNDataSource) {
                createActions.add(ActionUtils.makeCommandContribution(site, NavigatorCommands.CMD_CREATE_LOCAL_FOLDER));
            } else if (node instanceof DBNResource) {
                final DBPProjectManager projectRegistry = DBWorkbench.getPlatform().getProjectManager();
                IResource resource = ((DBNResource) node).getResource();
                DBPResourceHandler handler = projectRegistry.getResourceHandler(resource);
                if (handler instanceof DBPResourceCreator && (handler.getFeatures(resource) & DBPResourceCreator.FEATURE_CREATE_FILE) != 0) {
                    createActions.add(ActionUtils.makeCommandContribution(site, NavigatorCommands.CMD_CREATE_RESOURCE_FILE));
                }
                if (handler != null && (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0) {
                    createActions.add(ActionUtils.makeCommandContribution(site, NavigatorCommands.CMD_CREATE_RESOURCE_FOLDER));
                }
                if (resource instanceof IFolder) {
                    createActions.add(new Separator());
                    createActions.add(ActionUtils.makeCommandContribution(site, NavigatorCommands.CMD_CREATE_FILE_LINK));
                    createActions.add(ActionUtils.makeCommandContribution(site, NavigatorCommands.CMD_CREATE_FOLDER_LINK));
                }
            }

            if (!createActions.isEmpty() && !(createActions.get(createActions.size() - 1) instanceof Separator)) {
                createActions.add(new Separator());
            }
            createActions.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_NEW, "Other ...", null));
            return createActions.toArray(new IContributionItem[0]);
        }
    }

    private static void addDatabaseNodeCreateItems(IWorkbenchPartSite site, List<IContributionItem> createActions, DBNDatabaseNode node) {
        if (node instanceof DBNDatabaseFolder) {
            final List<DBXTreeNode> metaChildren = ((DBNDatabaseFolder) node).getMeta().getChildren(node);
            if (!CommonUtils.isEmpty(metaChildren)) {
                Class<?> nodeClass = ((DBNContainer) node).getChildrenClass();
                String nodeType = metaChildren.get(0).getChildrenType(node.getDataSource(), null);
                DBPImage nodeIcon = metaChildren.get(0).getIcon(node);
                if (nodeClass != null && nodeType != null) {
                    if (isCreateSupported(node, nodeClass)) {
                        CommandContributionItem item = makeCreateContributionItem(
                            site, nodeClass.getName(), nodeType, nodeIcon, false);
                        createActions.add(item);
                    }
                }
            }
        } else {
            Class<?> nodeItemClass = node.getObject().getClass();
            DBPImage nodeIcon = node.getNodeIconDefault();
            if (node instanceof DBNDataSource) {
                nodeIcon = UIIcon.SQL_NEW_CONNECTION;
            }
            if (isCreateSupported(
                node.getParentNode() instanceof DBNDatabaseNode ? (DBNDatabaseNode) node.getParentNode() : null,
                nodeItemClass))
            {
                createActions.add(
                    makeCreateContributionItem(
                        site, nodeItemClass.getName(), node.getNodeType(), nodeIcon, false));
            }

            if (isReadOnly(node.getObject())) {
                // Do not add child folders
                return;
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

            String typeName = childMeta.getNodeType(node.getDataSource(), null);
            if (typeName != null) {
                CommandContributionItem item = makeCreateContributionItem(
                    site, objectClass.getName(), typeName, childMeta.getIcon(node), true);
                createActions.add(item);
                return true;
            }
        }
        return false;
    }

    private static boolean isCreateSupported(DBNDatabaseNode parentNode, Class<?> objectClass) {
        DBEObjectMaker objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(objectClass, DBEObjectMaker.class);
        return objectMaker != null && objectMaker.canCreateObject(parentNode == null ? null : parentNode.getValueObject());
    }

    private static CommandContributionItem makeCreateContributionItem(
        IWorkbenchPartSite site, String objectType, String objectTypeName, DBPImage objectIcon, boolean isFolder)
    {
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
        if (isFolder) {
            parameters.put(NavigatorCommands.PARAM_OBJECT_TYPE_FOLDER, String.valueOf(true));
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
