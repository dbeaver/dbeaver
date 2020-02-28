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
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSInstanceLazy;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolderContainer;
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

public class NavigatorHandlerObjectOpen extends NavigatorHandlerObjectBase implements IElementUpdater {

    private static final Log log = Log.getLog(NavigatorHandlerObjectOpen.class);

    private static final int MAX_OBJECT_SIZE_NO_CONFIRM = 3;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (UIUtils.isInDialog()) {
            // If some modal dialog is open then we don't do this
            return null;
        }
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            if (structSelection.size() > MAX_OBJECT_SIZE_NO_CONFIRM) {
                if (!UIUtils.confirmAction(HandlerUtil.getActiveShell(event), "Open " + structSelection.size() + " editors",
                    "You are about to open " + structSelection.size() + " editors. Are you sure?")) {
                    return null;
                }
            }
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                DBNNode node = null;
                if (element instanceof IResource) {
                    UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                    if (serviceSQL != null) {
                        serviceSQL.openResource((IResource) element);
                    }
                    continue;
                } else if (element instanceof DBNNode) {
                    node = (DBNNode)element;
                } else {
                    DBSObject object = RuntimeUtils.getObjectAdapter(element, DBSObject.class);
                    if (object != null) {
                        node = getNodeByObject(object);
                    }
                }
                if (node != null) {
                    NavigatorUtils.openNavigatorNode(node, HandlerUtil.getActiveWorkbenchWindow(event), event.getParameters());
                }
            }
        }
        return null;
    }

    @Nullable
    public static IEditorPart openEntityEditor(DBSObject object)
    {
        DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject(object);
        if (node != null) {
            return NavigatorHandlerObjectOpen.openEntityEditor(node, null, UIUtils.getActiveWorkbenchWindow());
        }
        return null;
    }

    public static IEditorPart openEntityEditor(
        DBNNode selectedNode,
        @Nullable String defaultPageId,
        IWorkbenchWindow workbenchWindow)
    {
        return openEntityEditor(selectedNode, defaultPageId, null, null, workbenchWindow, true);
    }

    public static IEditorPart openEntityEditor(
        @NotNull DBNNode selectedNode,
        @Nullable String defaultPageId,
        @Nullable String defaultFolderId,
        @Nullable Map<String, Object> attributes,
        IWorkbenchWindow workbenchWindow,
        boolean activate)
    {
        if (selectedNode instanceof DBNDataSource) {
            final DBPDataSourceContainer dataSourceContainer = ((DBNDataSource)selectedNode).getDataSourceContainer();
            openConnectionEditor(workbenchWindow, dataSourceContainer);
            return null;
        }
        try {
            if (selectedNode instanceof DBNDatabaseFolder && !(selectedNode.getParentNode() instanceof DBNDatabaseFolder) && selectedNode.getParentNode() instanceof DBNDatabaseNode) {
                if (defaultFolderId == null) {
                    defaultFolderId = selectedNode.getNodeType();
                }
                selectedNode = selectedNode.getParentNode();
            }

            IEditorPart editor = findEntityEditor(workbenchWindow, selectedNode);
            if (editor != null) {
                boolean settingsChanged = false;
                IEditorInput editorInput = editor.getEditorInput();
                if (editorInput instanceof DatabaseEditorInput) {
                    settingsChanged = setInputAttributes((DatabaseEditorInput<?>) editorInput, defaultPageId, defaultFolderId, attributes);
                }
                if (editor instanceof EntityEditor && defaultPageId != null) {
                    // Set active page
                    IEditorPart pageEditor = ((EntityEditor) editor).getPageEditor(defaultPageId);
                    if (pageEditor != null && pageEditor != ((EntityEditor) editor).getActiveEditor()) {
                        ((EntityEditor) editor).setActiveEditor(pageEditor);
                    }
                }
                if (editor instanceof ITabbedFolderContainer && defaultFolderId != null) {
                    // Activate default folder
                    if (((ITabbedFolderContainer) editor).switchFolder(defaultFolderId)) {
                        settingsChanged = true;
                    }
                }
                if (settingsChanged) {
                    if (editor instanceof IRefreshablePart) {
                        ((IRefreshablePart) editor).refreshPart(selectedNode, true);
                    }
                }
                if (workbenchWindow.getActivePage().getActiveEditor() != editor || activate) {
                    workbenchWindow.getActivePage().activate(editor);
                }
                return editor;
            }

            if (selectedNode instanceof DBNDatabaseNode) {
                DBNDatabaseNode dnNode = (DBNDatabaseNode) selectedNode;
                DBSObject databaseObject = dnNode.getObject();
                if (databaseObject != null) {
                    if (!databaseObject.isPersisted()) {
                        return null;
                    }
                    if (DBUtils.getDefaultContext(databaseObject, false) == null) {
                        // Not connected - try to connect
                        DBSInstance ownerInstance = DBUtils.getObjectOwnerInstance(databaseObject);
                        if (ownerInstance instanceof DBSInstanceLazy && !((DBSInstanceLazy)ownerInstance).isInstanceConnected()) {
                            if (!RuntimeUtils.runTask(monitor -> {
                                try {
                                    ((DBSInstanceLazy) ownerInstance).checkInstanceConnection(monitor);
                                } catch (DBException e) {
                                    throw new InvocationTargetException(e);
                                }
                            }, "Initiate instance connection",
                                dnNode.getDataSourceContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_OPEN_TIMEOUT))) {
                                return null;
                            }
                        }
                        //
                    }

                    if (selectedNode instanceof DBNDatabaseObject) {
                        DBNDatabaseObject objectNode = (DBNDatabaseObject) selectedNode;
                        if (!objectNode.isPersisted()) {
                            return null;
                        }
                        ObjectEditorInput objectInput = new ObjectEditorInput(objectNode);
                        setInputAttributes(objectInput, defaultPageId, defaultFolderId, attributes);
                        return workbenchWindow.getActivePage().openEditor(
                            objectInput,
                            objectNode.getMeta().getEditorId());
                    } else {
                        DatabaseNodeEditorInput editorInput = new DatabaseNodeEditorInput(dnNode);
                        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN)) {
                            if (databaseObject instanceof DBSObjectContainer) {
                                // do not auto-refresh object containers (too expensive)
                            } else {
                                refreshDatabaseNode(dnNode);
                            }
                        }
                        setInputAttributes(editorInput, defaultPageId, defaultFolderId, attributes);
                        return workbenchWindow.getActivePage().openEditor(
                            editorInput,
                            EntityEditor.class.getName());
                    }
                } else {
                    DBWorkbench.getPlatformUI().showError("No object", "Node has no associated database object");
                    return null;
                }
            } else {
                NodeEditorInput folderInput = new NodeEditorInput(selectedNode);
                return workbenchWindow.getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            }
        } catch (Exception ex) {
            DBWorkbench.getPlatformUI().showError(UINavigatorMessages.actions_navigator_error_dialog_open_entity_title, "Can't open entity '" + selectedNode.getNodeName() + "'", ex);
            return null;
        }
    }

    public static IEditorPart findEntityEditor(IWorkbenchWindow workbenchWindow, DBNNode node) {
        if (node == null) {
            return null;
        }
        DatabaseEditorInputFactory.setLookupEditor(true);
        try {
            for (IEditorReference ref : workbenchWindow.getActivePage().getEditorReferences()) {
                IEditorInput editorInput;
                try {
                    editorInput = ref.getEditorInput();
                } catch (Throwable e) {
                    continue;
                }
                if (editorInput instanceof INavigatorEditorInput) {
                    boolean matches;
                    if (editorInput instanceof DatabaseLazyEditorInput) {
                        matches = node.getNodeItemPath().equals(((DatabaseLazyEditorInput) editorInput).getNodePath());
                    } else {
                        matches = ((INavigatorEditorInput) editorInput).getNavigatorNode() == node;
                    }
                    if (matches) {
                        return ref.getEditor(true);
                    }
                }
            }
        }
        finally {
            DatabaseEditorInputFactory.setLookupEditor(false);
        }
        return null;
    }

    private static void refreshDatabaseNode(@NotNull DBNDatabaseNode selectedNode) throws InvocationTargetException, InterruptedException {
        final DBNDatabaseNode nodeToRefresh = selectedNode;
        UIUtils.runInProgressService(monitor -> {
            try {
                nodeToRefresh.refreshNode(monitor, nodeToRefresh);
            } catch (DBException e) {
                log.error("Error refreshing database object", e);
            }
        });
    }

    public static void openConnectionEditor(IWorkbenchWindow workbenchWindow, DBPDataSourceContainer dataSourceContainer) {
        UIServiceConnections serviceConnections = DBWorkbench.getService(UIServiceConnections.class);
        if (serviceConnections != null) {
            serviceConnections.openConnectionEditor(dataSourceContainer);
        }
    }

    private static boolean setInputAttributes(DatabaseEditorInput<?> editorInput, String defaultPageId, String defaultFolderId, Map<String, Object> attributes) {
        boolean changed = false;
        if (defaultFolderId != null && !CommonUtils.equalObjects(defaultFolderId, editorInput.getDefaultFolderId())) {
            editorInput.setDefaultFolderId(defaultFolderId);
            changed = true;
            if (defaultPageId == null) {
                defaultPageId = EntityEditorDescriptor.DEFAULT_OBJECT_EDITOR_ID;

            }
        }
        if (defaultPageId != null && !CommonUtils.equalObjects(defaultPageId, editorInput.getDefaultPageId())) {
            editorInput.setDefaultPageId(defaultPageId);
            changed = true;
        }

        if (!CommonUtils.isEmpty(attributes)) {
            for (Map.Entry<String, Object> attr : attributes.entrySet()) {
                if (!CommonUtils.equalObjects(editorInput.getAttribute(attr.getKey()), attr.getValue())) {
                    editorInput.setAttribute(attr.getKey(), attr.getValue());
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        final ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null) {
            ISelection selection = selectionProvider.getSelection();
            DBNNode node = NavigatorUtils.getSelectedNode(selection);
            if (node != null) {
                String actionName = UINavigatorMessages.actions_navigator_open;
                if (node instanceof DBNDataSource) {
                    actionName = UINavigatorMessages.actions_navigator_edit;
                } else if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    if (object != null) {
                        DBEObjectEditor objectManager = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(
                            object.getClass(),
                            DBEObjectEditor.class);
                        //actionName = objectManager == null || !objectManager.canEditObject(object) ? UINavigatorMessages.actions_navigator_view : UINavigatorMessages.actions_navigator_edit;
                        actionName = UINavigatorMessages.actions_navigator_view;
                    }
                }
                String label;
                if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 1) {
                    label = NLS.bind(actionName, UINavigatorMessages.actions_navigator__objects);
                } else {
                    label = NLS.bind(actionName, node.getNodeType()); //$NON-NLS-1$
                }
                element.setText(label);
            }
        }
    }
}