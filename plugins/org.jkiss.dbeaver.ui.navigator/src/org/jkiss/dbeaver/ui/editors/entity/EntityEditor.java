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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolder;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolderContainer;
import org.jkiss.dbeaver.ui.controls.folders.ITabbedFolderListener;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditor;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * EntityEditor
 */
public class EntityEditor extends MultiPageDatabaseEditor
    implements IPropertyChangeReflector, IProgressControlProvider, ISaveablePart2, ITabbedFolderContainer, IDataSourceContainerProvider, IEntityEditorContext
{
    public static final String ID = "org.jkiss.dbeaver.ui.editors.entity.EntityEditor"; //$NON-NLS-1$
    
    private static final Log log = Log.getLog(EntityEditor.class);

    private static class EditorDefaults {
        String pageId;
        String folderId;

        private EditorDefaults(String pageId, String folderId)
        {
            this.pageId = pageId;
            this.folderId = folderId;
        }
    }

    private static final Map<String, EditorDefaults> defaultPageMap = new HashMap<>();

    private final Map<String, IEditorPart> editorMap = new LinkedHashMap<>();
    private IEditorPart activeEditor;
    private DBECommandAdapter commandListener;
    private ITabbedFolderListener folderListener;
    private boolean hasPropertiesEditor;
    private Map<IEditorPart, IEditorActionBarContributor> actionContributors = new HashMap<>();
    private volatile boolean saveInProgress = false;
    private Menu breadcrumbsMenu;

    public EntityEditor()
    {
        folderListener = folderId -> {
            IEditorPart editor = getActiveEditor();
            if (editor != null) {
                String editorPageId = getEditorPageId(editor);
                if (editorPageId != null) {
                    updateEditorDefaults(editorPageId, folderId);
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

    @Nullable
    public DBECommandContext getCommandContext()
    {
        return getEditorInput().getCommandContext();
    }

    @Override
    public void dispose()
    {
        if (breadcrumbsMenu != null) {
            breadcrumbsMenu.dispose();
            breadcrumbsMenu = null;
        }
        for (Map.Entry<IEditorPart, IEditorActionBarContributor> entry : actionContributors.entrySet()) {
            GlobalContributorManager.getInstance().removeContributor(entry.getValue(), entry.getKey());
        }
        actionContributors.clear();

        DBECommandContext commandContext = getCommandContext();
        if (commandListener != null && commandContext != null) {
            commandContext.removeCommandListener(commandListener);
            commandListener = null;
        }
        super.dispose();

        if (getDatabaseObject() != null && commandContext != null) {
            commandContext.resetChanges(true);
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

    public boolean isSaveInProgress() {
        return saveInProgress;
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
        if (EditorUtils.isInAutoSaveJob()) {
            // Do not save entity editors in auto-save job (#2408)
            return;
        }

        // Flush all nested object editors and result containers
        for (IEditorPart editor : editorMap.values()) {
            if (editor instanceof ObjectPropertiesEditor || editor instanceof IEntityDataContainer) {
                if (editor.isDirty()) {
                    editor.doSave(monitor);
                }
            }
            if (monitor.isCanceled()) {
                return;
            }
        }

        // Show preview
        int previewResult = IDialogConstants.PROCEED_ID;
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_SQL_PREVIEW)) {
            monitor.beginTask(UINavigatorMessages.editors_entity_monitor_preview_changes, 1);
            previewResult = showChanges(true);
        }

        if (previewResult == IDialogConstants.IGNORE_ID) {
            // There are no changes to save
            // Let's just refresh dirty status
            firePropertyChange(IEditorPart.PROP_DIRTY);
            return;
        }
        if (previewResult != IDialogConstants.PROCEED_ID) {
            monitor.setCanceled(true);
            return;
        }

        try {
            saveInProgress = true;

            monitor.beginTask("Save changes...", 1);
            try {
                monitor.subTask("Save '" + getPartName() + "' changes...");
                SaveJob saveJob = new SaveJob();
                saveJob.schedule();

                // Wait until job finished
                UIUtils.waitJobCompletion(saveJob);
                if (!saveJob.success) {
                    monitor.setCanceled(true);
                    return;
                }
            } finally {
                monitor.done();
            }

            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
        finally {
            saveInProgress = false;
        }
    }

    private boolean saveCommandContext(final DBRProgressMonitor monitor, Map<String, Object> options)
    {
        monitor.beginTask("Save entity", 1);
        Throwable error = null;
        final DBECommandContext commandContext = getCommandContext();
        if (commandContext == null) {
            log.warn("Null command context");
            return true;
        }
        boolean isNewObject = getDatabaseObject() == null || !getDatabaseObject().isPersisted();
        try {
            commandContext.saveChanges(monitor, options);
        } catch (DBException e) {
            error = e;
        }
        if (getDatabaseObject() instanceof DBPStatefulObject) {
            try {
                ((DBPStatefulObject) getDatabaseObject()).refreshObjectState(monitor);
            } catch (DBCException e) {
                // Just report an error
                log.error(e);
            }
        }

        if (error == null) {
            // Refresh underlying node
            // It'll refresh database object and all it's descendants
            // So we'll get actual data from database
            final DBNDatabaseNode treeNode = getEditorInput().getNavigatorNode();
            try {
                UIUtils.runInProgressService(monitor1 -> {
                    try {
                        treeNode.refreshNode(monitor1,
                            isNewObject ? DBNEvent.FORCE_REFRESH : DBNEvent.UPDATE_ON_SAVE);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                error = e.getTargetException();
            } catch (InterruptedException e) {
                // ok
            }
        }
        monitor.done();

        if (error == null) {
            return true;
        } else {
            // Try to handle error in nested editors
            final Throwable vError = error;
            UIUtils.syncExec(() -> {
                final IErrorVisualizer errorVisualizer = getAdapter(IErrorVisualizer.class);
                if (errorVisualizer != null) {
                    errorVisualizer.visualizeError(monitor, vError);
                }
            });

            // Show error dialog

            UIUtils.asyncExec(() ->
                DBWorkbench.getPlatformUI().showError("Can't save '" + getDatabaseObject().getName() + "'", null, vError));
            return false;
        }
    }

    public void revertChanges()
    {
        if (isDirty()) {
            if (ConfirmationDialog.showConfirmDialog(
                ResourceBundle.getBundle(UINavigatorMessages.BUNDLE_NAME),
                null,
                NavigatorPreferences.CONFIRM_ENTITY_REVERT,
                ConfirmationDialog.QUESTION,
                getDatabaseObject().getName()) != IDialogConstants.YES_ID)
            {
                return;
            }
            DBECommandContext commandContext = getCommandContext();
            if (commandContext != null) {
                commandContext.resetChanges(true);
            }
            refreshPart(this, false);
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void undoChanges()
    {
        IUndoManager undoManager = getAdapter(IUndoManager.class);
        if (undoManager != null) {
            undoManager.undo();
        } else {
            DBECommandContext commandContext = getCommandContext();
            if (commandContext != null && commandContext.getUndoCommand() != null) {
                if (!getDatabaseObject().isPersisted() && commandContext.getUndoCommands().size() == 1) {
                    //getSite().getPage().closeEditor(this, true);
                    //return;
                    // Undo of last command in command context will close editor
                    // Let's ask user about it
                    if (ConfirmationDialog.showConfirmDialog(
                        ResourceBundle.getBundle(UINavigatorMessages.BUNDLE_NAME),
                            null,
                            NavigatorPreferences.CONFIRM_ENTITY_REJECT,
                            ConfirmationDialog.QUESTION,
                            getDatabaseObject().getName()) != IDialogConstants.YES_ID) {
                        return;
                    }
                }
                commandContext.undoCommand();
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        }
    }

    public void redoChanges()
    {
        IUndoManager undoManager = getAdapter(IUndoManager.class);
        if (undoManager != null) {
            undoManager.redo();
        } else {
            DBECommandContext commandContext = getCommandContext();
            if (commandContext != null && commandContext.getRedoCommand() != null) {
                commandContext.redoCommand();
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        }
    }

    public int showChanges(boolean allowSave)
    {
        DBECommandContext commandContext = getCommandContext();
        if (commandContext == null) {
            return IDialogConstants.CANCEL_ID;
        }
        Collection<? extends DBECommand> commands = commandContext.getFinalCommands();
        if (CommonUtils.isEmpty(commands)) {
            return IDialogConstants.IGNORE_ID;
        }
        StringBuilder script = new StringBuilder();

        try {
            saveInProgress = true;
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask("Generate SQL script", commands.size());
                for (DBECommand command : commands) {
                    monitor.subTask(command.getTitle());
                    try {
                        command.validateCommand();
                    } catch (final DBException e) {
                        throw new InvocationTargetException(e);
                    }
                    Map<String, Object> options = new HashMap<>();
                    options.put(DBPScriptObject.OPTION_OBJECT_SAVE, true);

                    try {
                        DBEPersistAction[] persistActions = command.getPersistActions(monitor, options);
                        script.append(SQLUtils.generateScript(
                            commandContext.getExecutionContext().getDataSource(),
                            persistActions,
                            false));
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                    monitor.worked(1);
                }
                monitor.done();
            });
        } catch (InterruptedException e) {
            return IDialogConstants.CANCEL_ID;
        } catch (InvocationTargetException e) {
            log.error(e);
            DBWorkbench.getPlatformUI().showError("Script generate error", "Couldn't generate alter script", e.getTargetException());
        } finally {
            saveInProgress = false;
        }

        if (script.length() == 0) {
            return IDialogConstants.PROCEED_ID;
        }
        ChangesPreviewer changesPreviewer = new ChangesPreviewer(script, allowSave);
        UIUtils.syncExec(changesPreviewer);
        return changesPreviewer.getResult();
    }

    @Override
    protected void createPages()
    {
        final IDatabaseEditorInput editorInput = getEditorInput();
        if (editorInput instanceof DatabaseLazyEditorInput) {
            try {
                addPage(new ProgressEditorPart(this), editorInput);
                setPageText(0, "Initializing ...");
                setActivePage(0);
            } catch (PartInitException e) {
                log.error(e);
            }
            return;
        } else if (editorInput instanceof ErrorEditorInput) {
            ErrorEditorInput errorInput = (ErrorEditorInput) editorInput;
            try {
                addPage(new ErrorEditorPartEx(errorInput.getError()), errorInput);
                setPageImage(0, UIUtils.getShardImage(ISharedImages.IMG_OBJS_ERROR_TSK));
                setPageText(0, "Error");
                setActivePage(0);
            } catch (PartInitException e) {
                log.error(e);
            }
            return;
        }

        // Command listener
        commandListener = new DBECommandAdapter() {
            @Override
            public void onCommandChange(DBECommand command)
            {
                UIUtils.syncExec(() -> firePropertyChange(IEditorPart.PROP_DIRTY));
            }
        };
        DBECommandContext commandContext = getCommandContext();
        if (commandContext != null) {
            commandContext.addCommandListener(commandListener);
        }

        // Property listener
        addPropertyListener((source, propId) -> {
            if (propId == IEditorPart.PROP_DIRTY) {
                EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_DIRTY);
                EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_CAN_UNDO);
                EntityEditorPropertyTester.firePropertyChange(EntityEditorPropertyTester.PROP_CAN_REDO);
            }
        });

        super.createPages();

        DBSObject databaseObject = editorInput.getDatabaseObject();
        EditorDefaults editorDefaults = null;
        if (databaseObject == null) {
            // Weird
            log.debug("Null database object in EntityEditor");
        } else {
            synchronized (defaultPageMap) {
                editorDefaults = defaultPageMap.get(databaseObject.getClass().getName());
            }

            EntityEditorsRegistry editorsRegistry = EntityEditorsRegistry.getInstance();

            // Add object editor page
            EntityEditorDescriptor defaultEditor = editorsRegistry.getMainEntityEditor(databaseObject, this);
            hasPropertiesEditor = false;
            if (defaultEditor != null) {
                hasPropertiesEditor = addEditorTab(defaultEditor);
            }
            if (hasPropertiesEditor) {
                DBNNode node = editorInput.getNavigatorNode();
                int propEditorIndex = getPageCount() - 1;
                setPageText(propEditorIndex, UINavigatorMessages.editors_entity_properties_text);
                setPageToolTip(propEditorIndex, node.getNodeType() + UINavigatorMessages.editors_entity_properties_tooltip_suffix);
                setPageImage(propEditorIndex, DBeaverIcons.getImage(node.getNodeIconDefault()));
            }
        }

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_PROPS);
        addContributions(EntityEditorDescriptor.POSITION_START);
        addContributions(EntityEditorDescriptor.POSITION_MIDDLE);

        // Add contributed pages
        addContributions(EntityEditorDescriptor.POSITION_END);

        String defPageId = editorInput.getDefaultPageId();
        String defFolderId = editorInput.getDefaultFolderId();
        if (defPageId == null && editorDefaults != null) {
            defPageId = editorDefaults.pageId;
        }
        if (defPageId != null) {
            IEditorPart defEditorPage = editorMap.get(defPageId);
            if (defEditorPage != null) {
                setActiveEditor(defEditorPage);
            }
        } else {
            setActiveEditor(getEditor(0));
        }
        this.activeEditor = getActiveEditor();
        if (activeEditor instanceof ITabbedFolderContainer) {
            if (defFolderId == null && editorDefaults != null) {
                defFolderId = editorDefaults.folderId;
            }
            if (defFolderId != null) {
                ((ITabbedFolderContainer)activeEditor).switchFolder(defFolderId);
            }
        }

        UIUtils.setHelp(getContainer(), IHelpContextIds.CTX_ENTITY_EDITOR);
    }

    public IEditorPart getPageEditor(String pageId) {
        return editorMap.get(pageId);
    }

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
            ResourceBundle.getBundle(UINavigatorMessages.BUNDLE_NAME),
            getSite().getShell(),
            NavigatorPreferences.CONFIRM_ENTITY_EDIT_CLOSE,
            ConfirmationDialog.QUESTION_WITH_CANCEL,
            getEditorInput().getNavigatorNode().getNodeName());
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
    public ITabbedFolder getActiveFolder()
    {
        if (getActiveEditor() instanceof ITabbedFolderContainer) {
            ((ITabbedFolderContainer)getActiveEditor()).getActiveFolder();
        }
        return null;
    }

    @Override
    public boolean switchFolder(String folderId)
    {
        boolean changed = false;
        for (IEditorPart editor : editorMap.values()) {
            if (editor instanceof ITabbedFolderContainer) {
                if (getActiveEditor() != editor) {
                    setActiveEditor(editor);
                }
                if (((ITabbedFolderContainer)editor).switchFolder(folderId)) {
                    changed = true;
                }
            }
        }
//        if (getActiveEditor() instanceof IFolderedPart) {
//            ((IFolderedPart)getActiveEditor()).switchFolder(folderId);
//        }
        return changed;
    }

    public void setActiveEditor(Class<?> editorInterface) {
        for (int i = 0; i < getPageCount(); i++) {
            if (editorInterface.isAssignableFrom(getEditor(i).getClass())) {
                setActiveEditor(getEditor(i));
                break;
            }
        }
    }

    @Override
    public void addFolderListener(ITabbedFolderListener listener)
    {
    }

    @Override
    public void removeFolderListener(ITabbedFolderListener listener)
    {
    }

    private void addContributions(String position)
    {
        EntityEditorsRegistry editorsRegistry = EntityEditorsRegistry.getInstance();
        final DBSObject databaseObject = getEditorInput().getDatabaseObject();
        DBPObject object;
        if (databaseObject instanceof DBPDataSourceContainer && databaseObject.getDataSource() != null) {
            object = databaseObject.getDataSource();
        } else {
            object = databaseObject;
        }
        List<EntityEditorDescriptor> descriptors = editorsRegistry.getEntityEditors(
            object,
            this,
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
                setPageImage(index, DBeaverIcons.getImage(descriptor.getIcon()));
            }
            if (!CommonUtils.isEmpty(descriptor.getDescription())) {
                setPageToolTip(index, descriptor.getDescription());
            }
            editorMap.put(descriptor.getId(), editor);

            if (editor instanceof ITabbedFolderContainer) {
                ((ITabbedFolderContainer) editor).addFolderListener(folderListener);
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
        if (getContainer() == null || getContainer().isDisposed() || isSaveInProgress()) {
            return;
        }

        if (source instanceof DBNEvent && ((DBNEvent) source).getNodeChange() == DBNEvent.NodeChange.REFRESH) {
            // This may happen if editor was refreshed indirectly (it is a child of refreshed node)
            //force = true;
        }

        if (force && getDatabaseObject().isPersisted()) {
            // Lists and commands should be refreshed only if we make real refresh from remote storage
            // Otherwise just update object's properties
            DBECommandContext commandContext = getCommandContext();
            if (commandContext != null) {
                // Just clear command context. Do not undo because object state was already refreshed
                commandContext.resetChanges(true);
            }
        }

        DBSObject databaseObject = getEditorInput().getDatabaseObject();
        if (databaseObject != null) {
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
            setPageImage(0, DBeaverIcons.getImage(getEditorInput().getNavigatorNode().getNodeIconDefault()));
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        T activeAdapter = getNestedAdapter(adapter);
        if (activeAdapter != null) {
            return activeAdapter;
        }
        if (adapter == IPropertySheetPage.class) {
            return adapter.cast(new PropertyPageStandard());
        }
        if (adapter == DBSObject.class) {
            IDatabaseEditorInput editorInput = getEditorInput();
            DBSObject databaseObject = editorInput.getDatabaseObject();
            return adapter.cast(databaseObject);
        }
        return super.getAdapter(adapter);
    }

    public <T> T getNestedAdapter(Class<T> adapter) {
        // restrict delegating to the UI thread for bug 144851
        if (Display.getCurrent() != null) {
            IEditorPart activeEditor = getActiveEditor();
            if (activeEditor != null) {
                Object result = activeEditor.getAdapter(adapter);
                if (result != null) {
                    return adapter.cast(result);
                }
                if (adapter.isAssignableFrom(activeEditor.getClass())) {
                    return adapter.cast(activeEditor);
                }
            }
        }
        return null;
    }

    @Override
    protected Control createTopRightControl(Composite composite) {
        // Path
        ToolBar breadcrumbsPanel = new ToolBar(composite, SWT.HORIZONTAL | SWT.RIGHT);
        breadcrumbsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Make base node path
        DBNDatabaseNode node = getEditorInput().getNavigatorNode();

        List<DBNDatabaseNode> nodeList = new ArrayList<>();
        for (DBNNode n = node; n != null; n = n.getParentNode()) {
            if (n instanceof DBNDatabaseNode) {
                nodeList.add(0, (DBNDatabaseNode)n);
            }
        }
        for (final DBNDatabaseNode databaseNode : nodeList) {
            createPathRow(breadcrumbsPanel, databaseNode);
        }


        return breadcrumbsPanel;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return DBUtils.getContainer(getDatabaseObject());
    }

    @Override
    public void recreateEditorControl() {
        recreatePages();
    }

    private static final int MAX_BREADCRUMBS_MENU_ITEM = 300;

    private void createPathRow(ToolBar infoGroup, final DBNDatabaseNode databaseNode)
    {
        final DBNDatabaseNode curNode = getEditorInput().getNavigatorNode();

        final ToolItem item = new ToolItem(infoGroup, databaseNode instanceof DBNDatabaseFolder ? SWT.DROP_DOWN : SWT.PUSH);
        item.setText(databaseNode.getNodeName());
        item.setImage(DBeaverIcons.getImage(databaseNode.getNodeIconDefault()));

        if (databaseNode == curNode) {
            item.setToolTipText(databaseNode.getNodeType());
            item.setEnabled(false);
        } else {
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (e.detail == SWT.ARROW) {
                        int itemCount = 0;
                        if (breadcrumbsMenu != null) {
                            breadcrumbsMenu.dispose();
                        }
                        breadcrumbsMenu = new Menu(item.getParent().getShell());
                        try {
                            final DBNNode[] childNodes = NavigatorUtils.getNodeChildrenFiltered(new VoidProgressMonitor(), databaseNode, false);
                            if (!ArrayUtils.isEmpty(childNodes)) {
                                for (final DBNNode folderItem : childNodes) {
                                    MenuItem childItem = new MenuItem(breadcrumbsMenu, SWT.NONE);
                                    childItem.setText(folderItem.getName());
                                    //                                childItem.setImage(DBeaverIcons.getImage(folderItem.getNodeIconDefault()));
                                    if (folderItem == curNode) {
                                        childItem.setEnabled(false);
                                    }
                                    childItem.addSelectionListener(new SelectionAdapter() {
                                        @Override
                                        public void widgetSelected(SelectionEvent e) {
                                            NavigatorHandlerObjectOpen.openEntityEditor(folderItem, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                                        }
                                    });
                                    itemCount++;
                                    if (itemCount >= MAX_BREADCRUMBS_MENU_ITEM) {
                                        break;
                                    }
                                }
                            }
                        } catch (Throwable e1) {
                            log.error(e1);
                        }

                        Rectangle rect = item.getBounds();
                        Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
                        breadcrumbsMenu.setLocation(pt.x, pt.y + rect.height);
                        breadcrumbsMenu.setVisible(true);
                    } else {
                        NavigatorHandlerObjectOpen.openEntityEditor(databaseNode, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                    }
                }
            });
            item.setToolTipText(NLS.bind(UINavigatorMessages.actions_navigator_open, databaseNode.getNodeType()));
        }
    }

    private class ChangesPreviewer implements Runnable {

        private final StringBuilder script;
        private final boolean allowSave;
        private int result;

        ChangesPreviewer(StringBuilder script, boolean allowSave)
        {
            this.script = script;
            this.allowSave = allowSave;
        }

        @Override
        public void run()
        {
            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
            if (serviceSQL != null) {
                result = serviceSQL.openSQLViewer(
                    getExecutionContext(),
                    allowSave ? UINavigatorMessages.editors_entity_dialog_persist_title : UINavigatorMessages.editors_entity_dialog_preview_title,
                    UIIcon.SQL_PREVIEW,
                    script.toString(),
                    allowSave);
            } else {
                result = IDialogConstants.OK_ID;
            }
        }

        public int getResult()
        {
            return result;
        }
    }

    private class SaveJob extends AbstractJob {
        private transient Boolean success = null;

        SaveJob() {
            super("Save '" + getPartName() + "' changes...");
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                final DBECommandContext commandContext = getCommandContext();
                if (commandContext != null && commandContext.isDirty()) {
                    Map<String, Object> options = new HashMap<>();
                    options.put(DBPScriptObject.OPTION_OBJECT_SAVE, true);
                    success = saveCommandContext(monitor, options);
                } else {
                    success = true;
                }

                if (success) {
                    // Save nested editors
                    ProxyProgressMonitor proxyMonitor = new ProxyProgressMonitor(monitor);
                    for (IEditorPart editor : editorMap.values()) {
                        editor.doSave(proxyMonitor);
                        if (monitor.isCanceled()) {
                            success = false;
                            return Status.CANCEL_STATUS;
                        }
                    }
                    if (proxyMonitor.isCanceled()) {
                        success = false;
                        return Status.CANCEL_STATUS;
                    }
                }

                return success ? Status.OK_STATUS : Status.CANCEL_STATUS;
            } catch (Throwable e) {
                success = false;
                log.error(e);
                return GeneralUtils.makeExceptionStatus(e);
            } finally {
                if (success == null) {
                    success = true;
                }
            }
        }
    }

    // This is used by extensions to determine whether this entity is another entity container (e.g. for ERD)
    @Override
    public boolean isEntityContainer(DBSObjectContainer object) {
        try {
            Class<? extends DBSObject> childType = object.getChildType(new VoidProgressMonitor());
            return childType != null && DBSEntity.class.isAssignableFrom(childType);
        } catch (DBException e) {
            log.error(e);
            return false;
        }
    }

}
