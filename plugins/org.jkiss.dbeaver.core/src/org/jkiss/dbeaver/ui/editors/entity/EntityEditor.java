/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.IPropertyChangeReflector;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ext.ui.IProgressControlProvider;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;
import org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.IFolder;
import org.jkiss.dbeaver.ui.controls.folders.IFolderContainer;
import org.jkiss.dbeaver.ui.controls.folders.IFolderListener;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.ErrorEditorInput;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * EntityEditor
 */
public class EntityEditor extends MultiPageDatabaseEditor
    implements IPropertyChangeReflector, IProgressControlProvider, ISaveablePart2, IFolderContainer, INavigatorModelView
{
    static final Log log = Log.getLog(EntityEditor.class);

    private static class EditorDefaults {
        String pageId;
        String folderId;

        private EditorDefaults(String pageId, String folderId)
        {
            this.pageId = pageId;
            this.folderId = folderId;
        }
    }

    private static final Map<String, EditorDefaults> defaultPageMap = new HashMap<String, EditorDefaults>();

    private final Map<String, IEditorPart> editorMap = new LinkedHashMap<String, IEditorPart>();
    private IEditorPart activeEditor;
    private DBECommandAdapter commandListener;
    private IFolderListener folderListener;
    private boolean hasPropertiesEditor;
    private Map<IEditorPart, IEditorActionBarContributor> actionContributors = new HashMap<IEditorPart, IEditorActionBarContributor>();

    public EntityEditor()
    {
        folderListener = new IFolderListener() {
            @Override
            public void folderSelected(String folderId)
            {
                IEditorPart editor = getActiveEditor();
                if (editor != null) {
                    String editorPageId = getEditorPageId(editor);
                    if (editorPageId != null) {
                        updateEditorDefaults(editorPageId, folderId);
                    }
                }
            }
        };
    }

    @Override
    public void handlePropertyChange(int propId)
    {
        super.handlePropertyChange(propId);
    }

    @Nullable
    @Override
    public ProgressPageControl getProgressControl()
    {
        IEditorPart activeEditor = getActiveEditor();
        return activeEditor instanceof IProgressControlProvider ? ((IProgressControlProvider) activeEditor).getProgressControl() : null;
    }

    public DBSObject getDatabaseObject()
    {
        return getEditorInput().getDatabaseObject();
    }

    public DBECommandContext getCommandContext()
    {
        return getEditorInput().getCommandContext();
    }

    @Override
    public void dispose()
    {
        for (Map.Entry<IEditorPart, IEditorActionBarContributor> entry : actionContributors.entrySet()) {
            GlobalContributorManager.getInstance().removeContributor(entry.getValue(), entry.getKey());
        }
        actionContributors.clear();
        //final DBPDataSource dataSource = getDataSource();

//        if (getCommandContext() != null && getCommandContext().isDirty()) {
//            getCommandContext().resetChanges();
//        }
        if (commandListener != null && getCommandContext() != null) {
            getCommandContext().removeCommandListener(commandListener);
            commandListener = null;
        }
        super.dispose();

        if (getDatabaseObject() != null) {
            getCommandContext().resetChanges();
//            // Remove all non-persisted objects
//            for (DBPObject object : getCommandContext().getEditedObjects()) {
//                if (object instanceof DBPPersistedObject && !((DBPPersistedObject)object).isPersisted()) {
//                    dataSource.getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_REMOVE, (DBSObject) object));
//                }
//            }
        }
        this.editorMap.clear();
        this.activeEditor = null;
    }

    @Override
    public boolean isDirty()
    {
        final DBECommandContext commandContext = getCommandContext();
        if (commandContext != null && commandContext.isDirty()) {
            return true;
        }

        for (IEditorPart editor : editorMap.values()) {
            if (editor.isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return this.activeEditor != null && this.activeEditor.isSaveAsAllowed();
    }

    @Override
    public void doSaveAs()
    {
        IEditorPart activeEditor = getActiveEditor();
        if (activeEditor != null && activeEditor.isSaveAsAllowed()) {
            activeEditor.doSaveAs();
        }
    }

    /**
     * Saves data in all nested editors
     * @param monitor progress monitor
     */
    @Override
    public void doSave(IProgressMonitor monitor)
    {
        if (!isDirty()) {
            return;
        }

        for (IEditorPart editor : editorMap.values()) {
            editor.doSave(monitor);
        }

        final DBECommandContext commandContext = getCommandContext();
        if (commandContext != null && commandContext.isDirty()) {
            saveCommandContext(monitor);
        }

        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    private void saveCommandContext(IProgressMonitor monitor)
    {
        monitor.beginTask(CoreMessages.editors_entity_monitor_preview_changes, 1);
        int previewResult = showChanges(true);
        monitor.done();

        final DefaultProgressMonitor monitorWrapper = new DefaultProgressMonitor(monitor);

        if (previewResult == IDialogConstants.PROCEED_ID) {
            Throwable error = null;
            try {
                getCommandContext().saveChanges(monitorWrapper);
            } catch (DBException e) {
                error = e;
            }
            if (getDatabaseObject() instanceof DBSObjectStateful) {
                try {
                    ((DBSObjectStateful) getDatabaseObject()).refreshObjectState(monitorWrapper);
                } catch (DBCException e) {
                    // Just report an error
                    log.error(e);
                }
            }

            if (error == null) {
                // Refresh underlying node
                // It'll refresh database object and all it's descendants
                // So we'll get actual data from database
                final DBNDatabaseNode treeNode = getEditorInput().getTreeNode();
                try {
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            try {
                                treeNode.refreshNode(monitor, getCommandContext());
                            } catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                } catch (InvocationTargetException e) {
                    error = e.getTargetException();
                } catch (InterruptedException e) {
                    // ok
                }
            }
            if (error != null) {
                UIUtils.showErrorDialog(getSite().getShell(), "Could not save '" + getDatabaseObject().getName() + "'", null, error);
            }
        }
    }

    public void revertChanges()
    {
        if (isDirty()) {
            if (ConfirmationDialog.showConfirmDialog(
                null,
                DBeaverPreferences.CONFIRM_ENTITY_REVERT,
                ConfirmationDialog.QUESTION,
                getDatabaseObject().getName()) != IDialogConstants.YES_ID)
            {
                return;
            }
            getCommandContext().resetChanges();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void undoChanges()
    {
        if (getCommandContext() != null && getCommandContext().getUndoCommand() != null) {
            if (!getDatabaseObject().isPersisted() && getCommandContext().getUndoCommands().size() == 1) {
                //getSite().getPage().closeEditor(this, true);
                //return;
                // Undo of last command in command context will close editor
                // Let's ask user about it
                if (ConfirmationDialog.showConfirmDialog(
                    null,
                    DBeaverPreferences.CONFIRM_ENTITY_REJECT,
                    ConfirmationDialog.QUESTION,
                    getDatabaseObject().getName()) != IDialogConstants.YES_ID)
                {
                    return;
                }
            }
            getCommandContext().undoCommand();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void redoChanges()
    {
        if (getCommandContext() != null && getCommandContext().getRedoCommand() != null) {
            getCommandContext().redoCommand();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public int showChanges(boolean allowSave)
    {
        if (getCommandContext() == null) {
            return IDialogConstants.CANCEL_ID;
        }
        Collection<? extends DBECommand> commands = getCommandContext().getFinalCommands();
        StringBuilder script = new StringBuilder();
        for (DBECommand command : commands) {
            try {
                command.validateCommand();
            } catch (final DBException e) {
                UIUtils.runInUI(null, new Runnable() {
                    @Override
                    public void run()
                    {
                        UIUtils.showErrorDialog(getSite().getShell(), "Validation", e.getMessage());
                    }
                });
                return IDialogConstants.CANCEL_ID;
            }
            script.append(DBUtils.generateScript(
                command.getPersistActions()));
        }

        ChangesPreviewer changesPreviewer = new ChangesPreviewer(script, allowSave);
        UIUtils.runInUI(getSite().getShell(), changesPreviewer);
        return changesPreviewer.getResult();
/*

        Shell shell = getSite().getShell();
        EditTextDialog dialog = new EditTextDialog(shell, "Script", script.toString());
        dialog.setTextWidth(0);
        dialog.setTextHeight(0);
        dialog.setImage(DBIcon.SQL_PREVIEW.getImage());
        dialog.open();
*/
    }

    @Override
    protected void createPages()
    {
        if (getEditorInput() instanceof ErrorEditorInput) {
            ErrorEditorInput errorInput = (ErrorEditorInput) getEditorInput();
            try {
                addPage(new ErrorEditorPartEx(errorInput.getError()), errorInput);
                setPageImage(0, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
                setPageText(0, "Error");
            } catch (PartInitException e) {
                log.error(e);
            }
//            return;
        }
        // Command listener
        commandListener = new DBECommandAdapter() {
            @Override
            public void onCommandChange(DBECommand command)
            {
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        };
        getCommandContext().addCommandListener(commandListener);

        // Property listener
        addPropertyListener(new IPropertyListener() {
            @Override
            public void propertyChanged(Object source, int propId)
            {
                if (propId == IEditorPart.PROP_DIRTY) {
                    EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_DIRTY);
                    EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_CAN_UNDO);
                    EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_CAN_REDO);
                }
            }
        });

        super.createPages();

        EditorDefaults editorDefaults;
        synchronized (defaultPageMap) {
            editorDefaults = defaultPageMap.get(getEditorInput().getDatabaseObject().getClass().getName());
        }

        EntityEditorsRegistry editorsRegistry = EntityEditorsRegistry.getInstance();
        DBSObject databaseObject = getEditorInput().getDatabaseObject();

        // Add object editor page
        EntityEditorDescriptor defaultEditor = editorsRegistry.getMainEntityEditor(databaseObject);
        hasPropertiesEditor = false;
        if (defaultEditor != null) {
            hasPropertiesEditor = addEditorTab(defaultEditor);
        }
        if (hasPropertiesEditor) {
            DBNNode node = getEditorInput().getTreeNode();
            int propEditorIndex = getPageCount() - 1;
            setPageText(propEditorIndex, CoreMessages.editors_entity_properties_text);
            setPageToolTip(propEditorIndex, node.getNodeType() + CoreMessages.editors_entity_properties_tooltip_suffix);
            setPageImage(propEditorIndex, node.getNodeIconDefault());
        }
/*
        if (!mainAdded) {
            try {
                DBNNode node = getEditorInput().getTreeNode();
                int index = addPage(new ObjectPropertiesEditor(node), getEditorInput());
                setPageText(index, "Properties");
                if (node instanceof DBNDatabaseNode) {
                    setPageToolTip(index, ((DBNDatabaseNode)node).getMeta().getChildrenType() + " Properties");
                }
                setPageImage(index, node.getNodeIconDefault());
            } catch (PartInitException e) {
                log.error("Error creating object editor");
            }
        }
*/

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_PROPS);
        addContributions(EntityEditorDescriptor.POSITION_START);
        addContributions(EntityEditorDescriptor.POSITION_MIDDLE);

        // Add navigator tabs
        //addNavigatorTabs();

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_END);

        String defPageId = getEditorInput().getDefaultPageId();
        if (defPageId == null && editorDefaults != null) {
            defPageId = editorDefaults.pageId;
        }
        if (defPageId != null) {
            IEditorPart defEditorPage = editorMap.get(defPageId);
            if (defEditorPage != null) {
                setActiveEditor(defEditorPage);
            }
        }
        this.activeEditor = getActiveEditor();
        if (activeEditor instanceof IFolderContainer) {
            String defFolderId = getEditorInput().getDefaultFolderId();
            if (defFolderId == null && editorDefaults != null) {
                defFolderId = editorDefaults.folderId;
            }
            if (defFolderId != null) {
                ((IFolderContainer)activeEditor).switchFolder(defFolderId);
            }
        }

        UIUtils.setHelp(getContainer(), IHelpContextIds.CTX_ENTITY_EDITOR);
    }

/*
    private void addNavigatorTabs()
    {
        // Collect tabs from navigator tree model
        final List<TabInfo> tabs = new ArrayList<TabInfo>();
        DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
            {
                tabs.addAll(collectTabs(monitor));
            }
        };
        DBNDatabaseNode node = getEditorInput().getTreeNode();
        try {
            if (node.needsInitialization()) {
                DBeaverUI.runInProgressService(tabsCollector);
            } else {
                tabsCollector.run(VoidProgressMonitor.INSTANCE);
            }
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // just go further
        }

        for (TabInfo tab : tabs) {
            addNodeTab(tab);
        }
    }
*/

    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);

        activeEditor = getEditor(newPageIndex);

        for (Map.Entry<IEditorPart, IEditorActionBarContributor> entry : actionContributors.entrySet()) {
            if (entry.getKey() == activeEditor) {
                entry.getValue().setActiveEditor(activeEditor);
            } else {
                entry.getValue().setActiveEditor(null);
            }
        }

        String editorPageId = getEditorPageId(activeEditor);
        if (editorPageId != null) {
            updateEditorDefaults(editorPageId, null);
        }
        // Fire dirty flag refresh to re-enable Save-As command (which is enabled only for certain pages)
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    @Nullable
    private String getEditorPageId(IEditorPart editorPart)
    {
        synchronized (editorMap) {
            for (Map.Entry<String,IEditorPart> entry : editorMap.entrySet()) {
                if (entry.getValue() == editorPart) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void updateEditorDefaults(String pageId, @Nullable String folderId)
    {
        IDatabaseEditorInput editorInput = getEditorInput();
        if (editorInput instanceof DatabaseEditorInput) {
            ((DatabaseEditorInput) editorInput).setDefaultPageId(pageId);
            ((DatabaseEditorInput) editorInput).setDefaultFolderId(folderId);
        }
        DBSObject object = editorInput.getDatabaseObject();
        if (object != null) {
            synchronized (defaultPageMap) {
                EditorDefaults editorDefaults = defaultPageMap.get(object.getClass().getName());
                if (editorDefaults == null) {
                    editorDefaults = new EditorDefaults(pageId, folderId);
                    defaultPageMap.put(object.getClass().getName(), editorDefaults);
                } else {
                    if (pageId != null) {
                        editorDefaults.pageId = pageId;
                    }
                    if (folderId != null) {
                        editorDefaults.folderId = folderId;
                    }
                }
            }
        }
    }

    @Override
    public int promptToSaveOnClose()
    {
        final int result = ConfirmationDialog.showConfirmDialog(
            getSite().getShell(),
            DBeaverPreferences.CONFIRM_ENTITY_EDIT_CLOSE,
            ConfirmationDialog.QUESTION_WITH_CANCEL,
            getEditorInput().getTreeNode().getNodeName());
        if (result == IDialogConstants.YES_ID) {
//            getWorkbenchPart().getSite().getPage().saveEditor(this, false);
            return ISaveablePart2.YES;
        } else if (result == IDialogConstants.NO_ID) {
            return ISaveablePart2.NO;
        } else {
            return ISaveablePart2.CANCEL;
        }
    }

    @Nullable
    @Override
    public IFolder getActiveFolder()
    {
        if (getActiveEditor() instanceof IFolderContainer) {
            ((IFolderContainer)getActiveEditor()).getActiveFolder();
        }
        return null;
    }

    @Override
    public void switchFolder(String folderId)
    {
        for (IEditorPart editor : editorMap.values()) {
            if (editor instanceof IFolderContainer) {
                if (getActiveEditor() != editor) {
                    setActiveEditor(editor);
                }
                ((IFolderContainer)editor).switchFolder(folderId);
            }
        }
//        if (getActiveEditor() instanceof IFolderedPart) {
//            ((IFolderedPart)getActiveEditor()).switchFolder(folderId);
//        }
    }

    @Override
    public void addFolderListener(IFolderListener listener)
    {
    }

    @Override
    public void removeFolderListener(IFolderListener listener)
    {
    }
/*

    private static class TabInfo {
        DBNDatabaseNode node;
        DBXTreeNode meta;
        private TabInfo(DBNDatabaseNode node)
        {
            this.node = node;
        }
        private TabInfo(DBNDatabaseNode node, DBXTreeNode meta)
        {
            this.node = node;
            this.meta = meta;
        }
        public String getName()
        {
            return meta == null ? node.getNodeName() : meta.getChildrenType(node.getObject().getDataSource());
        }
    }

    private List<TabInfo> collectTabs(DBRProgressMonitor monitor)
    {
        List<TabInfo> tabs = new ArrayList<TabInfo>();

        // Add all nested folders as tabs
        DBNDatabaseNode node = getEditorInput().getTreeNode();
        if (node instanceof DBNDataSource &&
            (node.getDataSourceContainer() == null || !node.getDataSourceContainer().isConnected()))
        {
            // Do not add children tabs
        } else if (node != null) {
            try {
                List<? extends DBNNode> children = node.getChildren(monitor);
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            monitor.subTask(CoreMessages.editors_entity_monitor_add_folder + child.getNodeName() + "'");
                            tabs.add(new TabInfo((DBNDatabaseFolder)child));
                        }
                    }
                }
            } catch (DBException e) {
                log.error("Error initializing entity editor", e); //$NON-NLS-1$
            }
            // Add itself as tab (if it has child items)
            List<DBXTreeNode> subNodes = node.getMeta().getChildren(node);
            if (subNodes != null) {
                for (DBXTreeNode child : subNodes) {
                    if (child instanceof DBXTreeItem) {
                        try {
                            if (!((DBXTreeItem)child).isOptional() || node.hasChildren(monitor, child)) {
                                monitor.subTask(CoreMessages.editors_entity_monitor_add_node + node.getNodeName() + "'");
                                tabs.add(new TabInfo(node, child));
                            }
                        } catch (DBException e) {
                            log.debug("Can't add child items tab", e); //$NON-NLS-1$
                        }
                    }
                }
            }
        }
        return tabs;
    }
*/

    private void addContributions(String position)
    {
        EntityEditorsRegistry editorsRegistry = EntityEditorsRegistry.getInstance();
        final DBSObject databaseObject = getEditorInput().getDatabaseObject();
        DBPObject object;
        if (databaseObject instanceof DBSDataSourceContainer && databaseObject.getDataSource() != null) {
            object = databaseObject.getDataSource();
        } else {
            object = databaseObject;
        }
        List<EntityEditorDescriptor> descriptors = editorsRegistry.getEntityEditors(
            object,
            position);
        for (EntityEditorDescriptor descriptor : descriptors) {
            if (descriptor.getType() == EntityEditorDescriptor.Type.editor) {
                addEditorTab(descriptor);
            }
        }
    }

    private boolean addEditorTab(EntityEditorDescriptor descriptor)
    {
        try {
            IEditorPart editor = descriptor.createEditor();
            if (editor == null) {
                return false;
            }
            IEditorInput nestedInput = descriptor.getNestedEditorInput(getEditorInput());
            final Class<? extends IEditorActionBarContributor> contributorClass = descriptor.getContributorClass();
            if (contributorClass != null) {
                addActionsContributor(editor, contributorClass);
            }
            int index = addPage(editor, nestedInput);
            setPageText(index, descriptor.getName());
            if (descriptor.getIcon() != null) {
                setPageImage(index, descriptor.getIcon());
            }
            if (!CommonUtils.isEmpty(descriptor.getDescription())) {
                setPageToolTip(index, descriptor.getDescription());
            }
            editorMap.put(descriptor.getId(), editor);

            if (editor instanceof IFolderContainer) {
                ((IFolderContainer) editor).addFolderListener(folderListener);
            }

            return true;
        } catch (Exception ex) {
            log.error("Error adding nested editor", ex); //$NON-NLS-1$
            return false;
        }
    }

    private void addActionsContributor(IEditorPart editor, Class<? extends IEditorActionBarContributor> contributorClass) throws InstantiationException, IllegalAccessException
    {
        GlobalContributorManager contributorManager = GlobalContributorManager.getInstance();
        IEditorActionBarContributor contributor = contributorManager.getContributor(contributorClass);
        if (contributor == null) {
            contributor = contributorClass.newInstance();
        }
        contributorManager.addContributor(contributor, editor);
        actionContributors.put(editor, contributor);
    }

    @Override
    public void refreshPart(final Object source, boolean force)
    {
        // TODO: make smart content refresh
        // Lists and commands should be refreshed only if we make real refresh from remote storage
        // Otherwise just update object's properties
/*
        getEditorInput().getCommandContext().resetChanges();
*/
        DBSObject databaseObject = getEditorInput().getDatabaseObject();
        if (databaseObject != null && databaseObject.isPersisted()) {
            // Refresh visual content in parts
            for (IEditorPart editor : editorMap.values()) {
                if (editor instanceof IRefreshablePart) {
                    ((IRefreshablePart)editor).refreshPart(source, force);
                }
            }
        }

        setPartName(getEditorInput().getName());
        setTitleImage(getEditorInput().getImageDescriptor());

        if (hasPropertiesEditor) {
            // Update main editor image
            setPageImage(0, getEditorInput().getTreeNode().getNodeIconDefault());
        }
    }

    @Override
    public Object getAdapter(Class adapter) {
        Object activeAdapter = getNestedAdapter(adapter);
        return activeAdapter == null ? super.getAdapter(adapter) : activeAdapter;
    }

    public <T> T getNestedAdapter(Class<T> adapter) {
        IEditorPart activeEditor = getActiveEditor();
        if (activeEditor != null) {
            if (adapter.isAssignableFrom(activeEditor.getClass())) {
                return adapter.cast(activeEditor);
            }
            Object result = activeEditor.getAdapter(adapter);
            if (result != null) {
                return adapter.cast(result);
            }
        }
        return null;
    }

    @Override
    protected Control createTopRightControl(Composite composite) {
        // Path
        Composite infoGroup = new Composite(composite, SWT.NONE);//createControlGroup(container, "Path", 3, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        infoGroup.setLayout(new RowLayout());

        DBNDatabaseNode node = getEditorInput().getTreeNode();

        List<DBNDatabaseNode> nodeList = new ArrayList<DBNDatabaseNode>();
        for (DBNNode n = node; n != null; n = n.getParentNode()) {
            if (n instanceof DBNDatabaseNode) {
                nodeList.add(0, (DBNDatabaseNode)n);
            }
        }
        for (final DBNDatabaseNode databaseNode : nodeList) {
            createPathRow(
                infoGroup,
                databaseNode.getNodeIconDefault(),
                databaseNode.getNodeType(),
                databaseNode.getNodeName(),
                databaseNode == node ? null : new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        NavigatorHandlerObjectOpen.openEntityEditor(databaseNode, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                    }
                });
        }
        return infoGroup;
    }

    private void createPathRow(Composite infoGroup, Image image, String label, String value, @Nullable SelectionListener selectionListener)
    {
        UIUtils.createImageLabel(infoGroup, image);
        //UIUtils.createControlLabel(infoGroup, label);

        Link objectLink = new Link(infoGroup, SWT.NONE);
        //objectLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (selectionListener == null) {
            objectLink.setText(value);
            objectLink.setToolTipText(label);
        } else {
            objectLink.setText("<A>" + value + "</A>   ");
            objectLink.addSelectionListener(selectionListener);
            objectLink.setToolTipText("Open " + label + " Editor");
        }
    }

    @Override
    public DBNNode getRootNode() {
        return getEditorInput().getTreeNode();
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer()
    {
        IWorkbenchPart activePart = getActiveEditor();
        if (activePart instanceof INavigatorModelView) {
            return ((INavigatorModelView)activePart).getNavigatorViewer();
        } else if (getActiveFolder() instanceof INavigatorModelView) {
            return ((INavigatorModelView)getActiveFolder()).getNavigatorViewer();
        }
        return null;
    }

    private class ChangesPreviewer implements Runnable {

        private final StringBuilder script;
        private final boolean allowSave;
        private int result;

        public ChangesPreviewer(StringBuilder script, boolean allowSave)
        {
            this.script = script;
            this.allowSave = allowSave;
        }

        @Override
        public void run()
        {
            ViewSQLDialog dialog = new ViewSQLDialog(
                getEditorSite(),
                getDataSource(),
                allowSave ? CoreMessages.editors_entity_dialog_persist_title : CoreMessages.editors_entity_dialog_preview_title,
                DBIcon.SQL_PREVIEW.getImage(),
                script.toString());
            dialog.setShowSaveButton(allowSave);
            result = dialog.open();
        }

        public int getResult()
        {
            return result;
        }
    }

}
