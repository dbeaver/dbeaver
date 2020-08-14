/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPResourceCreator;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
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

/**
 * Sorry, this is a bit over-complicated handler. Historical reasons.
 * It can create object of specified type (in parameters) or for current selection.
 * Dynamic menu "Create" fills elements with parameters. Direct contribution of create command will create nearest
 * object type according to navigator selection.
 */
public class NavigatorHandlerObjectCreateNew extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    private static final Log log = Log.getLog(NavigatorHandlerObjectCreateNew.class);
    public static final Separator DUMMY_CONTRIBUTION_ITEM = new Separator();

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
            } else {
                // No explicit object type. Try to detect from selection
                IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
                if (activePart != null) {
                    List<IContributionItem> actions = fillCreateMenuItems(activePart.getSite(), node);
                    for (IContributionItem item : actions) {
                        if (item instanceof CommandContributionItem) {
                            ParameterizedCommand command = ((CommandContributionItem) item).getCommand();
                            if (command != null) {
                                ActionUtils.runCommand(command.getId(), selection, command.getParameterMap(), activePart.getSite());
                                return null;
                            }
                        }
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
        IWorkbenchWindow workbenchWindow = element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }

        Object typeName = parameters.get(NavigatorCommands.PARAM_OBJECT_TYPE_NAME);
        Object objectIcon = parameters.get(NavigatorCommands.PARAM_OBJECT_TYPE_ICON);
        if (typeName == null) {
            // Try to get type from active selection
            DBNNode node = NavigatorUtils.getSelectedNode(element);
            if (node != null && !node.isDisposed()) {
                List<IContributionItem> actions = fillCreateMenuItems(workbenchWindow.getActivePage().getActivePart().getSite(), node);
                for (IContributionItem item : actions) {
                    if (item instanceof CommandContributionItem) {
                        ParameterizedCommand command = ((CommandContributionItem) item).getCommand();
                        if (command != null) {
                            typeName = command.getParameterMap().get(NavigatorCommands.PARAM_OBJECT_TYPE_NAME);
                            if (typeName != null) {
                                // Prepend "Create new" as it is a single node
                                typeName = NLS.bind(UINavigatorMessages.actions_navigator_create_new, typeName);
                                // Do not use object icon ()
//                                if (!(node instanceof DBNDatabaseFolder)) {
//                                    objectIcon = command.getParameterMap().get(NavigatorCommands.PARAM_OBJECT_TYPE_ICON);
//                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
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

    // If site is null then we need only item count. BAD CODE.
    public static List<IContributionItem> fillCreateMenuItems(@Nullable IWorkbenchPartSite site, DBNNode node) {
        List<IContributionItem> createActions = new ArrayList<>();

        if (node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases) {
            IContributionItem item = makeCreateContributionItem(
                site, DBPDataSourceContainer.class.getName(), ModelMessages.model_navigator_Connection, UIIcon.SQL_NEW_CONNECTION, false);
            createActions.add(item);
        }
        if (node instanceof DBNDatabaseNode) {
            addDatabaseNodeCreateItems(site, createActions, (DBNDatabaseNode) node);
        }

        if (node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases || node instanceof DBNDataSource) {
            createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_LOCAL_FOLDER));
        } else if (node instanceof DBNResource) {
            final DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
            IResource resource = ((DBNResource) node).getResource();
            if (resource instanceof IProject) {
                createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_PROJECT));
            }
            DBPResourceHandler handler = workspace.getResourceHandler(resource);
            if (handler instanceof DBPResourceCreator && (handler.getFeatures(resource) & DBPResourceCreator.FEATURE_CREATE_FILE) != 0) {
                createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_RESOURCE_FILE));
            }
            if (handler != null && (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0) {
                createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_RESOURCE_FOLDER));
            }
            if (resource instanceof IContainer) {
                createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_FILE_LINK));
                createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_FOLDER_LINK));
            }
        }

        if (site != null) {
            if (!createActions.isEmpty() && !(createActions.get(createActions.size() - 1) instanceof Separator)) {
                createActions.add(new Separator());
            }
            createActions.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_NEW, "Other ...", null));
        }
        return createActions;
    }

    private static void addDatabaseNodeCreateItems(@Nullable IWorkbenchPartSite site, List<IContributionItem> createActions, DBNDatabaseNode node) {
        if (node instanceof DBNDatabaseFolder) {
            final List<DBXTreeNode> metaChildren = ((DBNDatabaseFolder) node).getMeta().getChildren(node);
            if (!CommonUtils.isEmpty(metaChildren)) {
                Class<?> nodeClass;
                if (metaChildren.size() == 1 && metaChildren.get(0) instanceof DBXTreeItem) {
                    nodeClass = node.getChildrenClass((DBXTreeItem)metaChildren.get(0));
                } else {
                    nodeClass = ((DBNContainer) node).getChildrenClass();
                }
                String nodeType = metaChildren.get(0).getChildrenTypeLabel(node.getDataSource(), null);
                DBPImage nodeIcon = node.getNodeIconDefault();//metaChildren.get(0).getIcon(node);
                if (nodeClass != null && nodeType != null) {
                    if (isCreateSupported(node, nodeClass)) {
                        IContributionItem item = makeCreateContributionItem(
                            site, nodeClass.getName(), nodeType, nodeIcon, false);
                        createActions.add(item);
                    }
                }
            }
        } else {
            if (node.getObject() == null) {
                return;
            }
            Class<?> nodeItemClass = node.getObject().getClass();
            DBNNode parentNode = node.getParentNode();
            if (isCreateSupported(
                parentNode instanceof DBNDatabaseNode ? (DBNDatabaseNode) parentNode : null,
                nodeItemClass))
            {
                if (site == null) {
                    createActions.add(DUMMY_CONTRIBUTION_ITEM);
                } else {
                    DBPImage nodeIcon = node instanceof DBNDataSource ?
                        UIIcon.SQL_NEW_CONNECTION : node.getNodeIconDefault();
                    createActions.add(
                        makeCreateContributionItem(
                            site, nodeItemClass.getName(), node.getNodeType(), nodeIcon, false));
                }
            }

            if (!node.getDataSourceContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA)) {
                // Do not add child folders
                return;
            }

            if (site != null) {
                // Now add all child folders
                createActions.add(new Separator());
            }

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

    private static boolean addChildNodeCreateItem(@Nullable IWorkbenchPartSite site, List<IContributionItem> createActions, DBNDatabaseNode node, DBXTreeItem childMeta) {
        if (childMeta.isVirtual()) {
            return false;
        }
        Class<?> objectClass = node.getChildrenClass(childMeta);
        if (objectClass != null) {

            if (!isCreateSupported(node, objectClass)) {
                return false;
            }

            String typeName = childMeta.getNodeTypeLabel(node.getDataSource(), null);
            if (typeName != null) {
                IContributionItem item = makeCreateContributionItem(
                    site, objectClass.getName(), typeName, childMeta.getIcon(null), true);
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

    private static IContributionItem makeCommandContributionItem(@Nullable IWorkbenchPartSite site, String commandId)
    {
        if (site == null) {
            // Dummy item. We need only count
            return DUMMY_CONTRIBUTION_ITEM;
        } else {
            return ActionUtils.makeCommandContribution(site, commandId);
        }
    }

    private static IContributionItem makeCreateContributionItem(
        @Nullable IWorkbenchPartSite site, String objectType, String objectTypeName, DBPImage objectIcon, boolean isFolder)
    {
        if (site == null) {
            return DUMMY_CONTRIBUTION_ITEM;
        }
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

            List<IContributionItem> createActions = fillCreateMenuItems(site, node);
            return createActions.toArray(new IContributionItem[0]);
        }
    }

}
