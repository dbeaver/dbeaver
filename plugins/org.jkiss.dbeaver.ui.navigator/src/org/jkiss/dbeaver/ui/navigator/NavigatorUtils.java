/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.IDataSourceContainerUpdate;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerRefresh;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorContent;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Navigator utils
 */
public class NavigatorUtils {

    private static final Log log = Log.getLog(NavigatorUtils.class);

    public static DBNNode getSelectedNode(ISelectionProvider selectionProvider)
    {
        if (selectionProvider == null) {
            return null;
        }
        return getSelectedNode(selectionProvider.getSelection());
    }

    public static DBNNode getSelectedNode(ISelection selection)
    {
        if (selection.isEmpty()) {
            return null;
        }
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
            if (selectedObject instanceof DBNNode) {
                return (DBNNode) selectedObject;
            } else if (selectedObject != null) {
                return RuntimeUtils.getObjectAdapter(selectedObject, DBNNode.class);
            }
        }
        return null;
    }

    @NotNull
    public static List<DBNNode> getSelectedNodes(@NotNull ISelection selection) {
        if (selection.isEmpty()) {
            return Collections.emptyList();
        }
        final List<DBNNode> nodes = new ArrayList<>();
        if (selection instanceof IStructuredSelection) {
            for (Object selectedObject : (IStructuredSelection) selection) {
                if (selectedObject instanceof DBNNode) {
                    nodes.add((DBNNode) selectedObject);
                } else {
                    DBNNode node = RuntimeUtils.getObjectAdapter(selectedObject, DBNNode.class);
                    if (node != null) {
                        nodes.add(node);
                    }
                }
            }
        }
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Find selected node for specified UI element
     * @param element ui element
     * @return ndoe or null
     */
    public static DBNNode getSelectedNode(UIElement element)
    {
        ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null) {
            return NavigatorUtils.getSelectedNode(selectionProvider);
        } else {
            return null;
        }
    }

    public static DBSObject getSelectedObject(ISelection selection)
    {
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return null;
        }
        return DBUtils.getFromObject(((IStructuredSelection)selection).getFirstElement());
    }

    public static List<DBSObject> getSelectedObjects(ISelection selection)
    {
        if (selection.isEmpty()) {
            return Collections.emptyList();
        }
        List<DBSObject> result = new ArrayList<>();
        if (selection instanceof IStructuredSelection) {
            for (Iterator iter = ((IStructuredSelection)selection).iterator(); iter.hasNext(); ) {
                DBSObject selectedObject = DBUtils.getFromObject(iter.next());
                if (selectedObject != null) {
                    result.add(selectedObject);
                }
            }
        }
        return result;
    }

    public static void addContextMenu(final IWorkbenchSite workbenchSite, final Viewer viewer) {
        addContextMenu(workbenchSite, viewer, viewer);
    }

    public static void addContextMenu(
        @Nullable final IWorkbenchSite workbenchSite,
        @NotNull final Viewer viewer,
        @NotNull ISelectionProvider selectionProvider)
    {
        MenuManager menuMgr = createContextMenu(workbenchSite, viewer, selectionProvider, null);
        if (workbenchSite instanceof IWorkbenchPartSite) {
            ((IWorkbenchPartSite)workbenchSite).registerContextMenu(menuMgr, viewer);
        } else if (workbenchSite instanceof IPageSite) {
            ((IPageSite)workbenchSite).registerContextMenu("navigatorMenu", menuMgr, viewer);
        }
    }

    public static MenuManager createContextMenu(
        @Nullable final IWorkbenchSite workbenchSite,
        @NotNull final Viewer viewer,
        @NotNull final IMenuListener menuListener)
    {
        return createContextMenu(workbenchSite, viewer, viewer, menuListener);
    }

    public static MenuManager createContextMenu(
        @Nullable final IWorkbenchSite workbenchSite,
        @NotNull final Viewer viewer,
        @NotNull final ISelectionProvider selectionProvider,
        @Nullable final IMenuListener menuListener)
    {
        final Control control = viewer.getControl();
        final MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menu.addMenuListener(new MenuListener()
        {
            @Override
            public void menuHidden(MenuEvent e)
            {
            }

            @Override
            public void menuShown(MenuEvent e)
            {
                Menu m = (Menu)e.widget;
                DBNNode node = getSelectedNode(viewer.getSelection());
                if (node != null && !node.isLocked() && node.allowsOpen()) {
                    String commandID = NavigatorUtils.getNodeActionCommand(DBXTreeNodeHandler.Action.open, node, NavigatorCommands.CMD_OBJECT_OPEN);
                    // Dirty hack
                    // Get contribution item from menu item and check it's ID
                    try {
                        for (MenuItem item : m.getItems()) {
                            Object itemData = item.getData();
                            if (itemData instanceof IContributionItem) {
                                String contribId = ((IContributionItem)itemData).getId();
                                if (contribId != null && contribId.equals(commandID)) {
                                    m.setDefaultItem(item);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log.debug(ex);
                    }
                }
            }
        });
        menuMgr.addMenuListener(manager -> {
            ViewerColumnController<?, ?> columnController = ViewerColumnController.getFromControl(control);
            if (columnController != null && columnController.isClickOnHeader()) {
                columnController.fillConfigMenu(manager);
                manager.add(new Separator());
                return;
            }

            manager.add(new Separator());

            addStandardMenuItem(workbenchSite, manager, selectionProvider);

            if (menuListener != null) {
                menuListener.menuAboutToShow(manager);
            }
        });

        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);

        return menuMgr;
    }

    public static void addStandardMenuItem(@Nullable IWorkbenchSite workbenchSite, @NotNull IMenuManager manager, @NotNull ISelectionProvider selectionProvider) {
        final ISelection selection = selectionProvider.getSelection();
        final DBNNode selectedNode = getSelectedNode(selectionProvider);
        if (selectedNode != null && !selectedNode.isLocked() && workbenchSite != null) {
            addSetDefaultObjectAction(workbenchSite, manager, selectedNode);
        }

        manager.add(new GroupMarker(NavigatorCommands.GROUP_NAVIGATOR_ADDITIONS));

        manager.add(new GroupMarker(NavigatorCommands.GROUP_TOOLS));
        manager.add(new GroupMarker(NavigatorCommands.GROUP_TOOLS_END));

        manager.add(new GroupMarker(NavigatorCommands.GROUP_NAVIGATOR_ADDITIONS_END));
        manager.add(new GroupMarker(IActionConstants.MB_ADDITIONS_END));

        if (selectedNode != null && !selectedNode.isLocked() && workbenchSite != null) {
            manager.add(new Separator());
            // Add properties button
            if (selection instanceof IStructuredSelection) {
                Object firstElement = ((IStructuredSelection) selection).getFirstElement();
                if (PreferencesUtil.hasPropertiesContributors(firstElement) && firstElement instanceof DBNResource) {
                    manager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_PROPERTIES));
                }
            }

            if (selectedNode.isPersisted()) {
                // Add refresh button
                manager.add(ActionUtils.makeCommandContribution(workbenchSite, IWorkbenchCommandConstants.FILE_REFRESH));
            }
        }
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private static void addSetDefaultObjectAction(IWorkbenchSite workbenchSite, IMenuManager manager, DBNNode selectedNode) {
        // Add "Set active object" menu
        boolean addSetActive = false;
        if (selectedNode.isPersisted() && selectedNode instanceof DBNDatabaseNode && !(selectedNode instanceof DBNDatabaseFolder) && ((DBNDatabaseNode)selectedNode).getObject() != null) {
            DBSObject selectedObject = ((DBNDatabaseNode) selectedNode).getObject();
            DBPDataSource dataSource = ((DBNDatabaseNode) selectedNode).getDataSource();
            if (dataSource != null) {
                DBCExecutionContext defaultContext = dataSource.getDefaultInstance().getDefaultContext(new VoidProgressMonitor(), false);
                DBCExecutionContextDefaults contextDefaults = defaultContext.getContextDefaults();
                if (contextDefaults != null) {
                    if ((selectedObject instanceof DBSCatalog && contextDefaults.supportsCatalogChange() && contextDefaults.getDefaultCatalog() != selectedObject) ||
                        (selectedObject instanceof DBSSchema && contextDefaults.supportsSchemaChange() && contextDefaults.getDefaultSchema() != selectedObject))
                    {
                        addSetActive = true;
                    }
                }
            }
        }

        if (addSetActive) {
            manager.add(ActionUtils.makeCommandContribution(workbenchSite, NavigatorCommands.CMD_OBJECT_SET_DEFAULT));
        }

        manager.add(new Separator());
    }

    public static void executeNodeAction(DBXTreeNodeHandler.Action action, Object node, IServiceLocator serviceLocator) {
        executeNodeAction(action, node, null, serviceLocator);
    }

    public static void executeNodeAction(DBXTreeNodeHandler.Action action, Object node, Map<String, Object> parameters, IServiceLocator serviceLocator) {
        String defCommandId = null;
        if (action == DBXTreeNodeHandler.Action.open) {
            defCommandId = NavigatorCommands.CMD_OBJECT_OPEN;
        }
        String actionCommand = getNodeActionCommand(action, node, defCommandId);
        if (actionCommand != null) {
            ActionUtils.runCommand(actionCommand, new StructuredSelection(node), parameters, serviceLocator);
        } else {
            // do nothing
            // TODO: implement some other behavior
        }

    }

    public static String getNodeActionCommand(DBXTreeNodeHandler.Action action, Object node, String defCommand) {
        if (node instanceof DBNDatabaseNode) {
            DBXTreeNodeHandler handler = ((DBNDatabaseNode) node).getMeta().getHandler(action);
            if (handler != null && handler.getPerform() == DBXTreeNodeHandler.Perform.command && !CommonUtils.isEmpty(handler.getCommand())) {
                return handler.getCommand();
            }
        }
        return defCommand;
    }

    public static void addDragAndDropSupport(final Viewer viewer) {
        addDragAndDropSupport(viewer, true, true);
    }

    public static void addDragAndDropSupport(final Viewer viewer, boolean enableDrag, boolean enableDrop) {
        if (enableDrag) {
            Transfer[] dragTransferTypes = new Transfer[] {
                TextTransfer.getInstance(),
                TreeNodeTransfer.getInstance(),
                DatabaseObjectTransfer.getInstance(),
                EditorInputTransfer.getInstance(),
                FileTransfer.getInstance()
            };
            
            if (RuntimeUtils.isGtk()) { 
                // TextTransfer should be the last on GTK due to platform' DND implementation inconsistency
                ArrayUtils.reverse(dragTransferTypes);
            }
            
            int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

            final DragSource source = new DragSource(viewer.getControl(), operations);
            source.setTransfer(dragTransferTypes);
            source.addDragListener(new DragSourceListener() {
                private IStructuredSelection selection;

                @Override
                public void dragStart(DragSourceEvent event) {
                    selection = (IStructuredSelection) viewer.getSelection();
                }

                @Override
                public void dragSetData(DragSourceEvent event) {
                    if (!selection.isEmpty()) {
                        final Map<DBNNode, TransferInfo> info = new LinkedHashMap<>();

                        for (Object nextSelected : selection) {
                            if (!(nextSelected instanceof DBNNode)) {
                                continue;
                            }
                            DBNNode node = (DBNNode) nextSelected;

                            String nodeName;
                            DBPNamedObject nodeObject = null;

                            if (nextSelected instanceof DBNDatabaseNode && !(nextSelected instanceof DBNDataSource)) {
                                DBSObject object = ((DBNDatabaseNode) nextSelected).getObject();
                                if (object == null) {
                                    continue;
                                }
                                nodeName = DBUtils.getObjectFullName(object, DBPEvaluationContext.UI);
                                nodeObject = object;
                            } else if (nextSelected instanceof DBNDataSource) {
                                DBPDataSourceContainer object = ((DBNDataSource) nextSelected).getDataSourceContainer();
                                nodeName = object.getName();
                                nodeObject = object;
                            } else if (nextSelected instanceof DBNStreamData
                                && ((DBNStreamData) nextSelected).supportsStreamData()
                                && (EditorInputTransfer.getInstance().isSupportedType(event.dataType)
                                    || FileTransfer.getInstance().isSupportedType(event.dataType)))
                            {
                                String fileName = node.getNodeName();
                                try {
                                    Path tmpFile = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "dnd-files").resolve(fileName);
                                    if (!Files.exists(tmpFile)) {
                                        try {
                                            Files.createFile(tmpFile);
                                        } catch (IOException e) {
                                            log.error("Can't create new file" + tmpFile.toAbsolutePath(), e);
                                            continue;
                                        }
                                        UIUtils.runInProgressService(monitor -> {
                                            try {
                                                long streamSize = ((DBNStreamData) nextSelected).getStreamSize();
                                                try (InputStream is = ((DBNStreamData) nextSelected).openInputStream()) {
                                                    try (OutputStream out = Files.newOutputStream(tmpFile)) {
                                                        ContentUtils.copyStreams(is, streamSize, out, monitor);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                try {
                                                    Files.delete(tmpFile);
                                                } catch (IOException ex) {
                                                    log.error("Error deleting temp file " + tmpFile.toAbsolutePath(), e);
                                                }
                                                throw new InvocationTargetException(e);
                                            }
                                        });
                                    }
                                    nodeName = tmpFile.toAbsolutePath().toString();
                                } catch (Exception e) {
                                    log.error(e);
                                    continue;
                                }
                            } else {
                                nodeName = node.getNodeTargetName();
                            }

                            info.put(node, new TransferInfo(nodeName, node, nodeObject));
                        }
                        if (TreeNodeTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = info.keySet();
                        } else if (DatabaseObjectTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = info.values().stream()
                                .map(TransferInfo::getObject)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = info.values().stream()
                                .map(TransferInfo::getName)
                                .collect(Collectors.joining(CommonUtils.getLineSeparator()));
                        } else if (EditorInputTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = info.values().stream()
                                .map(TransferInfo::createEditorInputData)
                                .filter(Objects::nonNull)
                                .toArray(EditorInputTransfer.EditorInputData[]::new);
                        } else if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = info.values().stream()
                                .map(TransferInfo::getName)
                                .filter(name -> Files.exists(Path.of(name)))
                                .toArray(String[]::new);
                        }
                    } else {
                        if (TreeNodeTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = Collections.emptyList();
                        } else if (DatabaseObjectTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = Collections.emptyList();
                        } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = "";
                        } else if (EditorInputTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = new EditorInputTransfer.EditorInputData[0];
                        } else if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
                            event.data = new String[0];
                        }
                    }
                }

                @Override
                public void dragFinished(DragSourceEvent event) {
                    // We don't want to delete any temporary file right after the drag is finished
                    // because we delete the file faster than the OS can copy/move it. In the case
                    // of a partially succeeded move, it produces a corrupted file.
                    //
                    // Any temporary file will be automatically deleted upon application exit.
                }
            });
        }

        if (enableDrop) {
            DropTarget dropTarget = new DropTarget(viewer.getControl(), DND.DROP_MOVE);
            dropTarget.setTransfer(TreeNodeTransfer.getInstance(), FileTransfer.getInstance());
            dropTarget.addDropListener(new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetEvent event) {
                    handleDragEvent(event);
                }

                @Override
                public void dragLeave(DropTargetEvent event) {
                    handleDragEvent(event);
                }

                @Override
                public void dragOperationChanged(DropTargetEvent event) {
                    handleDragEvent(event);
                }

                @Override
                public void dragOver(DropTargetEvent event) {
                    handleDragEvent(event);
                }

                @Override
                public void drop(DropTargetEvent event) {
                    handleDragEvent(event);
                    if (event.detail == DND.DROP_MOVE) {
                        moveNodes(event);
                    }
                }

                @Override
                public void dropAccept(DropTargetEvent event) {
                    handleDragEvent(event);
                }

                private void handleDragEvent(DropTargetEvent event) {
                    event.detail = isDropSupported(event) ? DND.DROP_MOVE : DND.DROP_NONE;
                    event.feedback = DND.FEEDBACK_SELECT;
                }

                private boolean isDropSupported(DropTargetEvent event) {
                    final Object curObject = getDropTarget(event, viewer);
                    if (TreeNodeTransfer.getInstance().isSupportedType(event.currentDataType)) {
                        @SuppressWarnings("unchecked")
                        Collection<DBNNode> nodesToDrop = (Collection<DBNNode>) event.data;
                        if (curObject instanceof DBNNode) {
                            if (!CommonUtils.isEmpty(nodesToDrop)) {
                                for (DBNNode node : nodesToDrop) {
                                    if (!((DBNNode) curObject).supportsDrop(node)) {
                                        return false;
                                    }
                                }
                                return true;
                            } else {
                                return ((DBNNode) curObject).supportsDrop(null);
                            }
                        } else if (curObject == null) {
                            // Drop to empty area
                            if (!CommonUtils.isEmpty(nodesToDrop)) {
                                for (DBNNode node : nodesToDrop) {
                                    if (!(node instanceof DBNDataSource)) {
                                        return false;
                                    }
                                }
                                return true;
                            } else {
                                Widget widget = event.widget;
                                if (widget instanceof DropTarget) {
                                    widget = ((DropTarget) widget).getControl();
                                }
                                return widget == viewer.getControl();
                            }
                        }
                    }
                    // Drop file - over resources
                    if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
                        if (curObject instanceof IAdaptable) {
                            IResource curResource = ((IAdaptable) curObject).getAdapter(IResource.class);
                            return curResource != null;
                        }
                    }

                    return false;
                }

                private void moveNodes(DropTargetEvent event) {
                    final Object curObject = getDropTarget(event, viewer);
                    if (TreeNodeTransfer.getInstance().isSupportedType(event.currentDataType)) {
                        if (curObject instanceof DBNNode) {
                            Collection<DBNNode> nodesToDrop = TreeNodeTransfer.getInstance().getObject();
                            try {
                                ((DBNNode) curObject).dropNodes(nodesToDrop);
                            } catch (DBException e) {
                                DBWorkbench.getPlatformUI().showError("Drop error", "Can't drop node", e);
                                return;
                            }
                        } else if (curObject == null) {
                            for (DBNNode node : TreeNodeTransfer.getInstance().getObject()) {
                                if (node instanceof DBNDataSource) {
                                    // Drop datasource on a view
                                    // We need target project
                                    if (viewer.getInput() instanceof DatabaseNavigatorContent) {
                                        DBNNode rootNode = ((DatabaseNavigatorContent) viewer.getInput()).getRootNode();
                                        if (rootNode != null && rootNode.getOwnerProject() != null) {
                                            ((DBNDataSource) node).moveToFolder(rootNode.getOwnerProject(), null);
                                        }
                                    }
                                } else if (node instanceof DBNLocalFolder) {
                                    ((DBNLocalFolder) node).getFolder().setParent(null);
                                } else {
                                    continue;
                                }
                                DBNModel.updateConfigAndRefreshDatabases(node);
                            }
                        }
                    }
                    if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
                        if (curObject instanceof IAdaptable) {
                            IResource curResource = ((IAdaptable) curObject).getAdapter(IResource.class);
                            if (curResource != null) {
                                if (curResource instanceof IFile) {
                                    curResource = curResource.getParent();
                                }
                                if (curResource instanceof IContainer) {
                                    IContainer toFolder = (IContainer) curResource;
                                    new AbstractJob("Copy files to workspace") {
                                        {
                                            setUser(true);
                                        }
                                        @Override
                                        protected IStatus run(DBRProgressMonitor monitor) {
                                            String[] fileNames = (String[]) event.data;
                                            monitor.beginTask("Copy files", fileNames.length);
                                            try {
                                                dropFilesIntoFolder(monitor, toFolder, fileNames);
                                            } catch (Exception e) {
                                                return GeneralUtils.makeExceptionStatus(e);
                                            } finally {
                                                monitor.done();
                                            }
                                            return Status.OK_STATUS;
                                        }
                                    }.schedule();
                                } else {
                                    DBWorkbench.getPlatformUI().showError("Drop error", "Can't drop file into '" + curResource.getName() + "'. Files can be dropped only into folders.");
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @Nullable
    private static Object getDropTarget(@NotNull DropTargetEvent event, @NotNull Viewer viewer) {
        if (event.item instanceof Item) {
            return event.item.getData();
        } else {
            Object input = viewer.getInput();
            if (input instanceof DatabaseNavigatorContent) {
                return ((DatabaseNavigatorContent) input).getRootNode();
            } else if (input instanceof List) {
                if (!((List<?>) input).isEmpty())
                    return ((List<?>) input).get(0);
            }
        }
        return null;
    }

    private static void dropFilesIntoFolder(DBRProgressMonitor monitor, IContainer toFolder, String[] data) throws Exception {
        for (String extFileName : data) {
            Path extFile = Path.of(extFileName);
            dropFileIntoContainer(monitor, toFolder, extFile);
        }
    }

    private static void dropFileIntoContainer(DBRProgressMonitor monitor, IContainer toFolder, Path extFile) throws CoreException, IOException {
        if (Files.exists(extFile)) {
            org.eclipse.core.runtime.Path ePath = new org.eclipse.core.runtime.Path(extFile.getFileName().toString());

            if (Files.isDirectory(extFile)) {
                IFolder subFolder = toFolder.getFolder(ePath);
                if (!subFolder.exists()) {
                    subFolder.create(true, true, monitor.getNestedMonitor());
                }
                List<Path> sourceFolderContents;
                try (Stream<Path> list = Files.list(extFile)) {
                    sourceFolderContents = list.collect(Collectors.toList());
                }
                for (Path folderFile : sourceFolderContents) {
                    dropFileIntoContainer(monitor, subFolder, folderFile);
                }
            } else {
                monitor.subTask("Copy file " + extFile);
                try {
                    if (toFolder instanceof IFolder && !toFolder.exists()) {
                        ((IFolder) toFolder).create(true, true, monitor.getNestedMonitor());
                    }
                    IFile targetFile = toFolder.getFile(ePath);
                    if (targetFile.exists()) {
                        if (!UIUtils.confirmAction("File exists", "File '" + targetFile.getName() + "' exists. Do you want to overwrite it?")) {
                            return;
                        }
                    }
                    try (InputStream is = Files.newInputStream(extFile)) {
                        if (targetFile.exists()) {
                            targetFile.setContents(is, true, false, monitor.getNestedMonitor());
                        } else {
                            targetFile.create(is, true, monitor.getNestedMonitor());
                        }
                    }
                } finally {
                    monitor.worked(1);
                }
            }
        }
    }

    public static NavigatorViewBase getActiveNavigatorView(ExecutionEvent event) {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof NavigatorViewBase) {
            return (NavigatorViewBase) activePart;
        }
        final IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        activePart = activePage.findView(DatabaseNavigatorView.VIEW_ID);
        if (activePart instanceof NavigatorViewBase && activePage.isPartVisible(activePart)) {
            return (NavigatorViewBase) activePart;
        }
        activePart = activePage.findView(ProjectNavigatorView.VIEW_ID);
        if (activePart instanceof NavigatorViewBase && activePage.isPartVisible(activePart)) {
            return (NavigatorViewBase) activePart;
        }
        return null;
    }

    public static void filterSelection(final ISelection selection, boolean exclude)
    {
        if (selection instanceof IStructuredSelection) {
            Map<DBNDatabaseFolder, DBSObjectFilter> folders = new HashMap<>();
            for (Object item : ((IStructuredSelection)selection).toArray()) {
                if (item instanceof DBNNode) {
                    final DBNNode node = (DBNNode) item;
                    DBNDatabaseFolder folder = (DBNDatabaseFolder) node.getParentNode();
                    DBSObjectFilter nodeFilter = folders.get(folder);
                    if (nodeFilter == null) {
                        nodeFilter = folder.getNodeFilter(folder.getItemsMeta(), true);
                        if (nodeFilter == null) {
                            nodeFilter = new DBSObjectFilter();
                        }
                        folders.put(folder, nodeFilter);
                    }
                    if (exclude) {
                        nodeFilter.addExclude(node.getNodeName());
                    } else {
                        nodeFilter.addInclude(node.getNodeName());
                    }
                    nodeFilter.setEnabled(true);
                }
            }
            // Save folders
            for (Map.Entry<DBNDatabaseFolder, DBSObjectFilter> entry : folders.entrySet()) {
                entry.getKey().setNodeFilter(entry.getKey().getItemsMeta(), entry.getValue());
            }
            // Refresh all folders
            NavigatorHandlerRefresh.refreshNavigator(folders.keySet());
        }
    }

    public static boolean syncEditorWithNavigator(INavigatorModelView navigatorView, IEditorPart activeEditor) {
        if (!(activeEditor instanceof IDataSourceContainerUpdate)) {
            return false;
        }
        IDataSourceContainerUpdate dsProvider = (IDataSourceContainerUpdate) activeEditor;
        Viewer navigatorViewer = navigatorView.getNavigatorViewer();
        if (navigatorViewer == null) {
            return false;
        }
        DBNNode selectedNode = getSelectedNode(navigatorViewer.getSelection());
        DBPProject nodeProject = selectedNode.getOwnerProject();
        if (!(selectedNode instanceof DBNDatabaseNode)
            || (nodeProject != null && !nodeProject.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT))
        ) {
            return false;
        }
        DBNDatabaseNode databaseNode = (DBNDatabaseNode) selectedNode;
        DBSObject dbsObject = databaseNode.getObject();
        if (!(dbsObject instanceof DBSStructContainer)) {
            dbsObject = DBUtils.getParentOfType(DBSStructContainer.class, dbsObject);
        }
        DBPDataSourceContainer ds = databaseNode.getDataSourceContainer();
        if (dsProvider.getDataSourceContainer() != ds) {
            dsProvider.setDataSourceContainer(ds);
            DatabaseEditorContext editorContext = new DatabaseEditorContextBase(ds, dbsObject);
            EditorUtils.setInputDataSource(activeEditor.getEditorInput(), editorContext);
        }

        if (activeEditor instanceof DBPContextProvider && dbsObject != null) {
            DBCExecutionContext navExecutionContext = null;
            try {
                navExecutionContext = DBUtils.getOrOpenDefaultContext(dbsObject, false);
            } catch (DBCException ignored) {
            }
            DBCExecutionContext editorExecutionContext = ((DBPContextProvider) activeEditor).getExecutionContext();
            if (navExecutionContext != null && editorExecutionContext != null) {
                DBCExecutionContextDefaults editorContextDefaults = editorExecutionContext.getContextDefaults();
                if (editorContextDefaults != null) {
                    final DBSObject dbObject = dbsObject;
                    RuntimeUtils.runTask(monitor -> {
                            try {
                                monitor.beginTask("Change default object", 1);
                                if (dbObject instanceof DBSCatalog && dbObject != editorContextDefaults.getDefaultCatalog()) {
                                    monitor.subTask("Change default catalog");
                                    editorContextDefaults.setDefaultCatalog(monitor, (DBSCatalog) dbObject, null);
                                } else if (dbObject instanceof DBSSchema && dbObject != editorContextDefaults.getDefaultSchema()) {
                                    monitor.subTask("Change default schema");
                                    editorContextDefaults.setDefaultSchema(monitor, (DBSSchema) dbObject);
                                }
                                monitor.worked(1);
                                monitor.done();
                            } catch (DBCException e) {
                                throw new InvocationTargetException(e);
                            }
                        }, "Set active object",
                        dbObject.getDataSource().getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_OPEN_TIMEOUT));
                }
            }
        }

        return true;
    }

    public static void openNavigatorNode(Object node, IWorkbenchWindow window) {
        openNavigatorNode(node, window, null);
    }

    public static void openNavigatorNode(Object node, IWorkbenchWindow window, Map<?, ?> parameters) {
        IResource resource = node instanceof IAdaptable ? ((IAdaptable) node).getAdapter(IResource.class) : null;
        if (resource instanceof IFile) {
            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
            if (serviceSQL != null) {
                serviceSQL.openResource(resource);
            }
        } else if (node instanceof DBNNode && ((DBNNode) node).allowsOpen()) {
            if (node instanceof DBNObjectNode) {
                INavigatorObjectManager objectManager = GeneralUtils.adapt(((DBNObjectNode) node).getNodeObject(), INavigatorObjectManager.class);
                if (objectManager != null) {
                    if (((objectManager.getSupportedFeatures() & INavigatorObjectManager.FEATURE_OPEN)) != 0) {
                        try {
                            objectManager.openObjectEditor(window, (DBNObjectNode) node);
                        } catch (Exception e) {
                            DBWorkbench.getPlatformUI().showError(
                                "Error opening object",
                                "Error while opening object '" + ((DBNObjectNode) node).getNodeObject() + "'",
                                e);
                        }
                    }
                    return;
                }
            }
            Object activePage = parameters == null ? null : parameters.get(MultiPageDatabaseEditor.PARAMETER_ACTIVE_PAGE);
            NavigatorHandlerObjectOpen.openEntityEditor(
                (DBNNode) node,
                CommonUtils.toString(activePage, null),
                window);
        }
    }

    @Nullable
    public static IStructuredSelection getSelectionFromPart(IWorkbenchPart part)
    {
        if (part == null) {
            return null;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return null;
        }
        return (IStructuredSelection)selection;
    }

    public static DBPProject getSelectedProject() {
        IWorkbenchPart activePart = UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart();
        ISelection selection;
        if (activePart == null) {
            selection = null;
        } else {
            ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
            selection = selectionProvider == null ? null : selectionProvider.getSelection();
        }
        return NavigatorUtils.getSelectedProject(selection, activePart);

    }

    public static DBPProject getSelectedProject(ISelection currentSelection, IWorkbenchPart activePart) {
        DBPProject activeProject = null;
        if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
            Object selItem = ((IStructuredSelection) currentSelection).getFirstElement();
            if (selItem instanceof DBNNode) {
                activeProject = ((DBNNode) selItem).getOwnerProject();
            }
        }
        if (activeProject == null) {
            if (activePart instanceof DBPContextProvider) {
                DBCExecutionContext executionContext = ((DBPContextProvider) activePart).getExecutionContext();
                if (executionContext != null) {
                    activeProject = executionContext.getDataSource().getContainer().getRegistry().getProject();
                } else if (activePart instanceof DBPDataSourceContainerProvider) {
                    DBPDataSourceContainer container = ((DBPDataSourceContainerProvider) activePart).getDataSourceContainer();
                    if (container != null) {
                        activeProject = container.getProject();
                    }
                }
            }
        }
        if (activeProject == null) {
            activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        }
        return activeProject;
    }

    public static void showNodeInNavigator(DBNDatabaseNode dsNode) {
        IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
        NavigatorViewBase nodeView;
        try {
            if (dsNode.getOwnerProject() == DBWorkbench.getPlatform().getWorkspace().getActiveProject()) {
                nodeView = UIUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
                if (nodeView == null) {
                    nodeView = (NavigatorViewBase) workbenchWindow.getActivePage().showView(DatabaseNavigatorView.VIEW_ID);
                }
            } else {
                nodeView = UIUtils.findView(workbenchWindow, ProjectNavigatorView.class);
                if (nodeView == null) {
                    nodeView = (NavigatorViewBase) workbenchWindow.getActivePage().showView(ProjectNavigatorView.VIEW_ID);
                }
            }
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Can't open view", "Error opening navigator view", e);
            return;
        }
        if (nodeView != null) {
            if (!workbenchWindow.getActivePage().isPartVisible(nodeView)) {
                workbenchWindow.getActivePage().bringToTop(nodeView);
            }
            nodeView.showNode(dsNode);
        }
    }

    private static class TransferInfo {
        private final String name;
        private final DBNNode node;
        private final DBPNamedObject object;

        public TransferInfo(@NotNull String name, @NotNull DBNNode node, @Nullable DBPNamedObject object) {
            this.node = node;
            this.object = object;
            this.name = name;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @Nullable
        public DBPNamedObject getObject() {
            return object;
        }

        @Nullable
        public EditorInputTransfer.EditorInputData createEditorInputData() {
            if (node instanceof DBNDatabaseNode) {
                final DatabaseNodeEditorInput input = new DatabaseNodeEditorInput((DBNDatabaseNode) node);
                return EditorInputTransfer.createEditorInputData(EntityEditor.ID, input);
            }

            final File file = new File(name);

            if (file.exists()) {
                try {
                    final IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
                    final IEditorDescriptor editor = EditorUtils.getFileEditorDescriptor(file, window);
                    final IEditorInput input = new FileStoreEditorInput(EFS.getStore(file.toURI()));
                    return EditorInputTransfer.createEditorInputData(editor.getId(), input);
                } catch (Exception e) {
                    log.warn("Error creating editor input for file '" + file + "'", e);
                }
            }

            return null;
        }
    }
}
