/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.*;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.controls.folders.IFolder;
import org.jkiss.dbeaver.ui.controls.folders.IFolderContainer;
import org.jkiss.dbeaver.ui.controls.folders.IFolderListener;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.ErrorEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * EntityEditor
 */
public class EntityEditor extends MultiPageDatabaseEditor
    implements IPropertyChangeReflector, IProgressControlProvider, ISaveablePart2, IFolderContainer
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

    private static final Map<String, EditorDefaults> defaultPageMap = new HashMap<>();

    private final Map<String, IEditorPart> editorMap = new LinkedHashMap<>();
    private IEditorPart activeEditor;
    private DBECommandAdapter commandListener;
    private IFolderListener folderListener;
    private boolean hasPropertiesEditor;
    private Map<IEditorPart, IEditorActionBarContributor> actionContributors = new HashMap<>();
    private volatile boolean saveInProgress = false;

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

    @Nullable
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

        DBECommandContext commandContext = getCommandContext();
        if (commandListener != null && commandContext != null) {
            commandContext.removeCommandListener(commandListener);
            commandListener = null;
        }
        super.dispose();

        if (getDatabaseObject() != null && commandContext != null) {
            commandContext.resetChanges();
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

        try {
            saveInProgress = true;

            monitor.beginTask("Save changes...", 1);
            try {
                monitor.subTask("Save '" + getPartName() + "' changes...");
                SaveJob saveJob = new SaveJob();
                saveJob.schedule();

                // Wait until job finished
                Display display = Display.getCurrent();
                while (saveJob.finished == null) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                display.update();
                if (!saveJob.finished) {
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

    private boolean saveCommandContext(DBRProgressMonitor monitor)
    {
        monitor.beginTask(CoreMessages.editors_entity_monitor_preview_changes, 1);
        int previewResult = showChanges(true);
        monitor.done();


        if (previewResult == IDialogConstants.PROCEED_ID) {
            Throwable error = null;
            final DBECommandContext commandContext = getCommandContext();
            if (commandContext == null) {
                log.warn("Null command context");
                return true;
            }
            try {
                commandContext.saveChanges(monitor);
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
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            try {
                                treeNode.refreshNode(monitor, commandContext);
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
            if (error == null) {
                return true;
            } else {
                UIUtils.showErrorDialog(getSite().getShell(), "Can't save '" + getDatabaseObject().getName() + "'", null, error);
                return false;
            }
        }
        return true;
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
        DBECommandContext commandContext = getCommandContext();
        if (commandContext != null && commandContext.getUndoCommand() != null) {
            if (!getDatabaseObject().isPersisted() && commandContext.getUndoCommands().size() == 1) {
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
            commandContext.undoCommand();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void redoChanges()
    {
        DBECommandContext commandContext = getCommandContext();
        if (commandContext != null && commandContext.getRedoCommand() != null) {
            commandContext.redoCommand();
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public int showChanges(boolean allowSave)
    {
        DBECommandContext commandContext = getCommandContext();
        if (commandContext == null) {
            return IDialogConstants.CANCEL_ID;
        }
        Collection<? extends DBECommand> commands = commandContext.getFinalCommands();
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
                command.getPersistActions(), false));
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
        final IDatabaseEditorInput editorInput = getEditorInput();
        if (editorInput instanceof ErrorEditorInput) {
            ErrorEditorInput errorInput = (ErrorEditorInput) editorInput;
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
        DBECommandContext commandContext = getCommandContext();
        if (commandContext != null) {
            commandContext.addCommandListener(commandListener);
        }

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
            EntityEditorDescriptor defaultEditor = editorsRegistry.getMainEntityEditor(databaseObject);
            hasPropertiesEditor = false;
            if (defaultEditor != null) {
                hasPropertiesEditor = addEditorTab(defaultEditor);
            }
            if (hasPropertiesEditor) {
                DBNNode node = editorInput.getNavigatorNode();
                int propEditorIndex = getPageCount() - 1;
                setPageText(propEditorIndex, CoreMessages.editors_entity_properties_text);
                setPageToolTip(propEditorIndex, node.getNodeType() + CoreMessages.editors_entity_properties_tooltip_suffix);
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
            String defFolderId = editorInput.getDefaultFolderId();
            if (defFolderId == null && editorDefaults != null) {
                defFolderId = editorDefaults.folderId;
            }
            if (defFolderId != null) {
                ((IFolderContainer)activeEditor).switchFolder(defFolderId);
            }
        }

        UIUtils.setHelp(getContainer(), IHelpContextIds.CTX_ENTITY_EDITOR);
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
            getSite().getShell(),
            DBeaverPreferences.CONFIRM_ENTITY_EDIT_CLOSE,
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
        DBECommandContext commandContext = getCommandContext();
        if (commandContext != null) {
            // FIXME: resetChanges refreshes editor one more time and eventually leads to node reload/close.
            //commandContext.resetChanges();
        }

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
            setPageImage(0, DBeaverIcons.getImage(getEditorInput().getNavigatorNode().getNodeIconDefault()));
        }
    }

    @Override
    public Object getAdapter(Class adapter) {
        Object activeAdapter = getNestedAdapter(adapter);
        if (activeAdapter != null) {
            return activeAdapter;
        }
        if (adapter == IPropertySheetPage.class) {
            return new PropertyPageStandard();
        }
        return super.getAdapter(adapter);
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
        Composite breadcrumbsPanel = new Composite(composite, SWT.NONE);
        breadcrumbsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        breadcrumbsPanel.setLayout(new RowLayout());

        // Cleanup previous
        for (Control child : breadcrumbsPanel.getChildren()) {
            child.dispose();
        }

        // Make base node path
        DBNDatabaseNode node = getEditorInput().getNavigatorNode();

        List<DBNDatabaseNode> nodeList = new ArrayList<>();
        for (DBNNode n = node; n != null; n = n.getParentNode()) {
            if (n instanceof DBNDatabaseNode) {
                nodeList.add(0, (DBNDatabaseNode)n);
            }
        }
        for (final DBNDatabaseNode databaseNode : nodeList) {
            createPathRow(
                breadcrumbsPanel,
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


        return breadcrumbsPanel;
    }

    private void createPathRow(Composite infoGroup, DBPImage image, String label, String value, @Nullable SelectionListener selectionListener)
    {
        UIUtils.createImageLabel(infoGroup, image);

        Link objectLink = new Link(infoGroup, SWT.NONE);
        if (selectionListener == null) {
            objectLink.setText(value);
            objectLink.setToolTipText(label);
        } else {
            objectLink.setText("<A>" + value + "</A>   ");
            objectLink.addSelectionListener(selectionListener);
            objectLink.setToolTipText("Open " + label + " Editor");
        }
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
                getExecutionContext(),
                allowSave ? CoreMessages.editors_entity_dialog_persist_title : CoreMessages.editors_entity_dialog_preview_title,
                UIIcon.SQL_PREVIEW,
                script.toString());
            dialog.setShowSaveButton(allowSave);
            result = dialog.open();
        }

        public int getResult()
        {
            return result;
        }
    }

    private class SaveJob extends AbstractJob {
        private transient Boolean finished = null;

        public SaveJob() {
            super("Save '" + getPartName() + "' changes...");
            setUser(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                {
                    // Save nested editors
                    ProxyProgressMonitor proxyMonitor = new ProxyProgressMonitor(monitor);
                    for (IEditorPart editor : editorMap.values()) {
                        editor.doSave(proxyMonitor);
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                    }
                    if (proxyMonitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                }

                final DBECommandContext commandContext = getCommandContext();
                if (commandContext != null && commandContext.isDirty()) {
                    finished = saveCommandContext(monitor);
                } else {
                    finished = true;
                }

                return finished ? Status.OK_STATUS : Status.CANCEL_STATUS;
            } catch (Throwable e) {
                finished = false;
                log.error(e);
                return Status.CANCEL_STATUS;
            } finally {
                if (finished == null) {
                    finished = true;
                }
            }
        }
    }
}
