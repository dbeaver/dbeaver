/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPResourceCreator;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
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

        if (selection.isEmpty()) {
            return null;
        }
        DBNNode node = getNodeFromSelection(selection);

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

    @Nullable
    static DBNNode getNodeFromSelection(ISelection selection) {
        DBNNode node = null;
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
            node = RuntimeUtils.getObjectAdapter(selectedObject, DBNNode.class);
        }
        return node;
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
        ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider == null) {
            return;
        }

        Object typeName = parameters.get(NavigatorCommands.PARAM_OBJECT_TYPE_NAME);
        Object objectIcon = parameters.get(NavigatorCommands.PARAM_OBJECT_TYPE_ICON);
        if (typeName == null) {
            // Try to get type from active selection
            DBNNode node = getNodeFromSelection(selectionProvider.getSelection());
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
            DBPImage image = getObjectTypeIcon(selectionProvider);
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

    public static DBPImage getObjectTypeIcon(ISelectionProvider selectionProvider) {
        DBNNode node = getNodeFromSelection(selectionProvider.getSelection());
        if (node != null) {
            // In case of nested folder, we don't want to unwrap it because the parent's icon will be used instead
            if (!(node instanceof DBNDatabaseFolder) && node.getParentNode() instanceof DBNDatabaseFolder) {
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
        boolean projectResourceEditable =
            node == null || ObjectPropertyTester.nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);
        boolean projectConnectionEditable =
            node == null || ObjectPropertyTester.nodeProjectHasPermission(node, RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);

        if ((node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases) && projectConnectionEditable) {
            IContributionItem item = makeCreateContributionItem(
                site, DBPDataSourceContainer.class.getName(), ModelMessages.model_navigator_Connection, UIIcon.SQL_NEW_CONNECTION, false);
            createActions.add(item);
        }
        if (node instanceof DBNDatabaseNode) {
            addDatabaseNodeCreateItems(site, createActions, (DBNDatabaseNode) node);
        }

        if ((node instanceof DBNLocalFolder || node instanceof DBNProjectDatabases || node instanceof DBNDataSource)
            && projectConnectionEditable
        ) {
            createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_LOCAL_FOLDER));
        } else {
            final DBPWorkspaceDesktop workspace = DBPPlatformDesktop.getInstance().getWorkspace();
            final IResource resource = GeneralUtils.adapt(node, IResource.class);
            if (resource != null) {
                if (resource instanceof IProject && !DBWorkbench.isDistributed()) {
                    createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_PROJECT));
                }
                DBPResourceHandler handler = workspace.getResourceHandler(resource);
                if (handler instanceof DBPResourceCreator
                    && (handler.getFeatures(resource) & DBPResourceCreator.FEATURE_CREATE_FILE) != 0 && projectResourceEditable
                ) {
                    createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_RESOURCE_FILE));
                }
                if (handler != null
                    && (handler.getFeatures(resource) & DBPResourceHandler.FEATURE_CREATE_FOLDER) != 0 && projectResourceEditable
                ) {
                    createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_RESOURCE_FOLDER));
                }
                if (resource instanceof IContainer && projectResourceEditable) {
                    createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_FILE_LINK));
                    createActions.add(makeCommandContributionItem(site, NavigatorCommands.CMD_CREATE_FOLDER_LINK));
                }
            }
        }

        if (site != null) {
            if (!createActions.isEmpty() && !(createActions.get(createActions.size() - 1) instanceof Separator)) {
                createActions.add(new Separator());
            }
            createActions.add(ActionUtils.makeCommandContribution(
                site, IWorkbenchCommandConstants.FILE_NEW, UINavigatorMessages.navigator_handler_object_create_file_other_text, null
            ));
        }
        return createActions;
    }

    private static void addDatabaseNodeCreateItems(@Nullable IWorkbenchPartSite site, List<IContributionItem> createActions, DBNDatabaseNode node) {
        if (node instanceof DBNDatabaseFolder) {
            DBXTreeFolder folderMeta = ((DBNDatabaseFolder) node).getMeta();
            final List<DBXTreeNode> metaChildren = folderMeta.getChildren(node);
            if (!CommonUtils.isEmpty(metaChildren)) {
                // Test direct child node items
                Class<?> nodeClass = null;
                if (metaChildren.size() == 1 && metaChildren.get(0) instanceof DBXTreeItem) {
                    nodeClass = node.getChildrenClass((DBXTreeItem)metaChildren.get(0));
                }
                {
                    Class<?> childrenClass = ((DBNDatabaseFolder) node).getChildrenClass();
                    if (nodeClass == null || (childrenClass != null && nodeClass.isAssignableFrom(childrenClass))) {
                        // folder.getChildrenClass may return more precise type than node.getChildrenClass
                        nodeClass = childrenClass;
                    }
                }
                if (nodeClass == null) {
                    nodeClass = ((DBNDatabaseFolder) node).getChildrenClass();
                }
                String nodeType = metaChildren.get(0).getChildrenTypeLabel(node.getDataSource(), null);
                if (nodeClass != null && nodeType != null) {
                    if (isCreateSupported(node, nodeClass)) {
                        DBPImage nodeIcon = node.getNodeIconDefault();//metaChildren.get(0).getIcon(node);
                        IContributionItem item = makeCreateContributionItem(
                            site, nodeClass.getName(), nodeType, nodeIcon, false);
                        createActions.add(item);
                    }
                }
            }
            // Test explicit create types
            DBXTreeFolder.ItemType[] itemTypes = folderMeta.getItemTypes();
            if (itemTypes != null) {
                for (DBXTreeFolder.ItemType itemType : itemTypes) {
                    Class<Object> itemClass = folderMeta.getSource().getObjectClass(itemType.getClassName(), Object.class);
                    if (itemClass != null) {
                        if (isCreateSupported(node, itemClass)) {
                            IContributionItem item = makeCreateContributionItem(
                                site, itemType.getClassName(), itemType.getItemType(), itemType.getItemIcon(), false);
                            createActions.add(item);
                        }
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
                parentNode,
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

    private static boolean isCreateSupported(DBNNode parentNode, Class<?> objectClass) {
        DBEObjectMaker objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(objectClass, DBEObjectMaker.class);
        return objectMaker != null && objectMaker.canCreateObject(
            parentNode instanceof DBNDatabaseNode ?
                ((DBNDatabaseNode) parentNode).getValueObject() : parentNode.getOwnerProject());
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
            DBNNode node = getNodeFromSelection(site.getSelectionProvider().getSelection());

            List<IContributionItem> createActions = fillCreateMenuItems(site, node);
            return createActions.toArray(new IContributionItem[0]);
        }
    }

}
