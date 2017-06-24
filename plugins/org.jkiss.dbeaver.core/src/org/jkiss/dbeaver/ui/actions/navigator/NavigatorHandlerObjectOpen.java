/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolderContainer;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionWizard;
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.ui.editors.entity.NodeEditorInput;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

public class NavigatorHandlerObjectOpen extends NavigatorHandlerObjectBase implements IElementUpdater {

    private static final Log log = Log.getLog(NavigatorHandlerObjectOpen.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                DBNNode node = null;
                if (element instanceof IResource) {
                    openResource((IResource)element, HandlerUtil.getActiveWorkbenchWindow(event));
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
                    NavigatorUtils.openNavigatorNode(node, HandlerUtil.getActiveWorkbenchWindow(event));
                }
            }
        }
        return null;
    }

    public static void openResource(IResource resource, IWorkbenchWindow window)
    {
        try {
            DBPResourceHandler handler = DBeaverCore.getInstance().getProjectRegistry().getResourceHandler(resource);
            if (handler != null) {
                handler.openResource(resource);
            }
        } catch (Exception e) {
            DBUserInterface.getInstance().showError(CoreMessages.actions_navigator_error_dialog_open_resource_title, "Can't open resource '" + resource.getName() + "'", e); //$NON-NLS-3$
        }
    }

    @Nullable
    public static IEditorPart openEntityEditor(DBSObject object)
    {
        DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject(object);
        if (node != null) {
            return NavigatorHandlerObjectOpen.openEntityEditor(node, null, DBeaverUI.getActiveWorkbenchWindow());
        }
        return null;
    }

    public static IEditorPart openEntityEditor(
        DBNNode selectedNode,
        @Nullable String defaultPageId,
        IWorkbenchWindow workbenchWindow)
    {
        return openEntityEditor(selectedNode, defaultPageId, null , workbenchWindow);
    }

    public static IEditorPart openEntityEditor(
        @NotNull DBNNode selectedNode,
        @Nullable String defaultPageId,
        @Nullable Map<String, Object> attributes,
        IWorkbenchWindow workbenchWindow)
    {
        if (selectedNode instanceof DBNDataSource) {
            final DataSourceDescriptor dataSourceContainer = (DataSourceDescriptor) ((DBNDataSource)selectedNode).getDataSourceContainer();
            openConnectionEditor(workbenchWindow, dataSourceContainer);
            return null;
        }
        if (!selectedNode.isPersisted()) {
            log.debug("Node '" + selectedNode.getNodeName() + "' s not persisted. Open not possible.");
            return null;
        }
        try {
            String defaultFolderId = null;
            if (selectedNode instanceof DBNDatabaseFolder && !(selectedNode.getParentNode() instanceof DBNDatabaseFolder) && selectedNode.getParentNode() instanceof DBNDatabaseNode) {
                defaultFolderId = selectedNode.getNodeType();
                selectedNode = selectedNode.getParentNode();
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
                            matches = selectedNode.getNodeItemPath().equals(((DatabaseLazyEditorInput) editorInput).getNodePath());
                        } else {
                            matches = ((INavigatorEditorInput) editorInput).getNavigatorNode() == selectedNode;
                        }
                        if (matches) {
                            final IEditorPart editor = ref.getEditor(true);
                            if (editor instanceof ITabbedFolderContainer && defaultFolderId != null) {
                                // Activate default folder
                                ((ITabbedFolderContainer) editor).switchFolder(defaultFolderId);
                            }
                            workbenchWindow.getActivePage().activate(editor);
                            return editor;
                        }
                    }
                }
            }
            finally {
                DatabaseEditorInputFactory.setLookupEditor(false);
            }

            if (selectedNode instanceof DBNDatabaseObject) {
                DBNDatabaseObject objectNode = (DBNDatabaseObject) selectedNode;
                ObjectEditorInput objectInput = new ObjectEditorInput(objectNode);
                setInputAttributes(objectInput, defaultPageId, defaultFolderId, attributes);
                return workbenchWindow.getActivePage().openEditor(
                    objectInput,
                    objectNode.getMeta().getEditorId());
            } else if (selectedNode instanceof DBNDatabaseNode) {
                DBNDatabaseNode dnNode = (DBNDatabaseNode) selectedNode;
                if (dnNode.getObject() != null) {
                    EntityEditorInput editorInput = new EntityEditorInput(dnNode);
                    if (DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN)) {
                        if (dnNode.getObject() instanceof DBSObjectContainer) {
                            // do not auto-refresh object containers (too expensive)
                        } else {
                            refreshDatabaseNode(dnNode);
                        }
                    }
                    setInputAttributes(editorInput, defaultPageId, defaultFolderId, attributes);
                    return workbenchWindow.getActivePage().openEditor(
                        editorInput,
                        EntityEditor.class.getName());
                } else {
                    DBUserInterface.getInstance().showError("No object", "Node do not has associated database object");
                    return null;
                }
            } else {
                NodeEditorInput folderInput = new NodeEditorInput(selectedNode);
                return workbenchWindow.getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            }
        } catch (Exception ex) {
            DBUserInterface.getInstance().showError(CoreMessages.actions_navigator_error_dialog_open_entity_title, "Can't open entity '" + selectedNode.getNodeName() + "'", ex);
            return null;
        }
    }

    private static void refreshDatabaseNode(@NotNull DBNDatabaseNode selectedNode) throws InvocationTargetException, InterruptedException {
        final DBNDatabaseNode nodeToRefresh = selectedNode;
        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    nodeToRefresh.refreshNode(monitor, nodeToRefresh);
                } catch (DBException e) {
                    log.error("Error refreshing database object", e);
                }
            }
        });
    }

    private static void openConnectionEditor(IWorkbenchWindow workbenchWindow, DataSourceDescriptor dataSourceContainer) {
        EditConnectionDialog dialog = new EditConnectionDialog(
            workbenchWindow,
            new EditConnectionWizard(dataSourceContainer));
        dialog.open();
    }

    public static void openResourceEditor(IWorkbenchWindow workbenchWindow, ResourceUtils.ResourceInfo resourceInfo) {
        if (resourceInfo.getResource() != null) {
            openResource(resourceInfo.getResource(), workbenchWindow);
        } else if (resourceInfo.getLocalFile() != null) {
            EditorUtils.openExternalFileEditor(resourceInfo.getLocalFile(), workbenchWindow);
        }
    }

    private static void setInputAttributes(DatabaseEditorInput<?> editorInput, String defaultPageId, String defaultFolderId, Map<String, Object> attributes) {
        editorInput.setDefaultPageId(defaultPageId);
        editorInput.setDefaultFolderId(defaultFolderId);
        if (!CommonUtils.isEmpty(attributes)) {
            for (Map.Entry<String, Object> attr : attributes.entrySet()) {
                editorInput.setAttribute(attr.getKey(), attr.getValue());
            }
        }
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
                String actionName = CoreMessages.actions_navigator_open;
                if (node instanceof DBNDataSource) {
                    actionName = CoreMessages.actions_navigator_edit;
                } else if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    if (object != null) {
                        DBEObjectEditor objectManager = EntityEditorsRegistry.getInstance().getObjectManager(
                            object.getClass(),
                            DBEObjectEditor.class);
                        actionName = objectManager == null || !objectManager.canEditObject(object) ? CoreMessages.actions_navigator_view : CoreMessages.actions_navigator_edit;
                    }
                }
                String label;
                if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 1) {
                    label = actionName + CoreMessages.actions_navigator__objects;
                } else {
                    label = actionName + " " + node.getNodeType(); //$NON-NLS-1$
                }
                element.setText(label);
            }
        }
    }
}