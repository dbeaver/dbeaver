/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.impl.edit.DBECommandContextImpl;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

public class NavigatorHandlerObjectDelete extends NavigatorHandlerObjectBase {
    private IStructuredSelection structSelection;
    private Boolean deleteAll;

    public Object execute(ExecutionEvent event) throws ExecutionException {
        this.structSelection = null;
        this.deleteAll = null;

        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            structSelection = (IStructuredSelection)selection;
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                if (element instanceof DBNDatabaseNode) {
                    deleteObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNDatabaseNode)element);
                } else if (element instanceof DBNResource) {
                    deleteResource(HandlerUtil.getActiveWorkbenchWindow(event), (DBNResource)element);
                } else {
                    log.warn("Don't know how to delete element '" + element + "'");
                }
                if (deleteAll != null && !deleteAll) {
                    break;
                }
            }
        }
        return null;
    }

    private boolean deleteResource(IWorkbenchWindow workbenchWindow, final DBNResource resourceNode)
    {
        ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, resourceNode, false);
        if (confirmResult == ConfirmResult.NO) {
            return false;
        }

        final IResource resource = resourceNode.getResource();
        try {
            if (resource instanceof IFolder) {
                ((IFolder)resource).delete(true, false, new NullProgressMonitor());
            } else if (resource instanceof IProject) {
                final String projectId = ProjectRegistry.getProjectId((IProject) resource);
                // Delete project (with all contents)
                ((IProject) resource).delete(true, true, new NullProgressMonitor());
                // Manually remove this project from registry
                DBeaverCore.getInstance().getProjectRegistry().removeProject(projectId);
            } else {
                resource.delete(true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    private boolean deleteObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node)
    {
        try {
            if (!(node.getParentNode() instanceof DBNContainer)) {
                throw new DBException("Node '" + node + "' doesn't have a container");
            }

            // Try to delete object using object manager
            DBSObject object = node.getObject();
            if (object == null) {
                throw new DBException("Can't delete node with null object");
            }
            if (!object.isPersisted()) {
                // Not a real object delete because it's not persisted
                // There should be command context somewhere
                return deleteNewObject(workbenchWindow, node);
            }
            DBEObjectManager<?> objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(object.getClass());
            if (objectManager == null) {
                throw new DBException("Object manager not found for type '" + object.getClass().getName() + "'");
            }
            if (!(objectManager instanceof DBEObjectMaker<?>)) {
                throw new DBException("Object manager '" + objectManager.getClass().getName() + "' do not supports object deletion");
            }

            DBEObjectMaker objectMaker = (DBEObjectMaker)objectManager;
            Map<String, Object> deleteOptions = null;

            ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, node, true);
            if (confirmResult == ConfirmResult.NO) {
                return false;
            }
            DBECommandContext commander = null;
            if (!(node instanceof DBNDataSource)) {
                DBPDataSource dataSource = node.getObject().getDataSource();
                if (dataSource == null) {
                    log.error("Node '" + node.getNodeName() + "' object is notify() attached toString() datasource");
                    return false;
                }
                commander = new DBECommandContextImpl(dataSource.getContainer());
            }
            objectMaker.deleteObject(commander, node.getObject(), deleteOptions);
            if (commander == null) {
                return true;
            }
            if (confirmResult == ConfirmResult.DETAILS) {
                if (!showScript(workbenchWindow, commander)) {
                    commander.resetChanges();
                    return false;
                }
            }

            // Delete object
            ObjectDeleter deleter = new ObjectDeleter(commander);
            DBeaverCore.getInstance().runInProgressService(deleter);
        } catch (InterruptedException e) {
            // do nothing
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            UIUtils.showErrorDialog(workbenchWindow.getShell(), "Delete object", "Can't delete object '" + node.getNodeName() + "'", e);
            return false;
        }

        // Remove node
        if (!node.isDisposed()) {
            DBNNode parent = node.getParentNode();
            if (parent instanceof DBNContainer) {
                try {
                    ((DBNContainer)parent).removeChildItem(node);
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }

        return true;
    }

    private boolean deleteNewObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node) throws DBException
    {
        for (final IEditorReference editorRef : workbenchWindow.getActivePage().getEditorReferences()) {
            final IEditorPart editor = editorRef.getEditor(false);

            if (editor instanceof IDatabaseNodeEditor) {
                final IDatabaseNodeEditorInput editorInput = ((IDatabaseNodeEditor)editor).getEditorInput();
                if (editorInput.getDatabaseObject() == node.getObject()) {
                    // Just close editor
                    // It should dismiss new object and remove navigator node
                    ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, node, false);
                    if (confirmResult == ConfirmResult.NO) {
                        return false;
                    }
                    workbenchWindow.getActivePage().closeEditor(editor, false);
                    return true;
                } else {
                    // May be it is a nested object
                    editorInput.getCommandContext();
                    //throw new DBException("Can't detect object '" + node.getNodeName() + "' owner host");
                }
            }
        }
        throw new DBException("Can't detect object '" + node.getNodeName() + "' host editor");
    }

    private boolean showScript(IWorkbenchWindow workbenchWindow, DBECommandContext commander)
    {
        Collection<? extends DBECommand> commands = commander.getCommands();
        StringBuilder script = new StringBuilder();
        for (DBECommand command : commands) {
            IDatabasePersistAction[] persistActions = command.getPersistActions();
            if (!CommonUtils.isEmpty(persistActions)) {
                for (IDatabasePersistAction action : persistActions) {
                    if (script.length() > 0) {
                        script.append('\n');
                    }
                    script.append(action.getScript());
                    script.append(commander.getDataSourceContainer().getDataSource().getInfo().getScriptDelimiter());
                }
            }
        }
        DatabaseNavigatorView view = ViewUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
        if (view != null) {
            ViewSQLDialog dialog = new ViewSQLDialog(
                view.getSite(),
                commander.getDataSourceContainer(),
                "Delete script",
                script.toString());
            dialog.setImage(DBIcon.SQL_PREVIEW.getImage());
            dialog.setShowSaveButton(true);
            return dialog.open() == IDialogConstants.PROCEED_ID;
        } else {
            return false;
        }
    }

    enum ConfirmResult {
        YES,
        NO,
        DETAILS,
    }

    private ConfirmResult confirmObjectDelete(final IWorkbenchWindow workbenchWindow, final DBNNode node, final boolean viewScript)
    {
        if (deleteAll != null) {
            return deleteAll ? ConfirmResult.YES : ConfirmResult.NO;
        }
        ResourceBundle bundle = DBeaverActivator.getInstance().getResourceBundle();
        String titleKey = ConfirmationDialog.RES_CONFIRM_PREFIX + PrefConstants.CONFIRM_ENTITY_DELETE + "." + ConfirmationDialog.RES_KEY_TITLE;
        String messageKey = ConfirmationDialog.RES_CONFIRM_PREFIX + PrefConstants.CONFIRM_ENTITY_DELETE + "." + ConfirmationDialog.RES_KEY_MESSAGE;

        String nodeTypeName = node.getNodeType();

        MessageDialog dialog = new MessageDialog(
            workbenchWindow.getShell(),
            UIUtils.formatMessage(bundle.getString(titleKey), nodeTypeName, node.getNodeName()),
            DBIcon.REJECT.getImage(),
            UIUtils.formatMessage(bundle.getString(messageKey), nodeTypeName.toLowerCase(), node.getNodeName()),
            MessageDialog.CONFIRM, null, 0)
        {
            @Override
            protected void createButtonsForButtonBar(Composite parent)
            {
                createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
                createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, false);
                if (structSelection.size() > 1) {
                    createButton(parent, IDialogConstants.YES_TO_ALL_ID, IDialogConstants.YES_TO_ALL_LABEL, false);
                    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
                }
                if (viewScript) {
                    createButton(parent, IDialogConstants.DETAILS_ID, "View Script", false);
                }
            }
        };
        int result = dialog.open();
        switch (result) {
            case IDialogConstants.YES_ID:
                return ConfirmResult.YES;
            case IDialogConstants.YES_TO_ALL_ID:
                deleteAll = true;
                return ConfirmResult.YES;
            case IDialogConstants.NO_ID:
                return ConfirmResult.NO;
            case IDialogConstants.CANCEL_ID:
            case -1:
                deleteAll = false;
                return ConfirmResult.NO;
            case IDialogConstants.DETAILS_ID:
                return ConfirmResult.DETAILS;
            default:
                log.warn("Unsupported confirmation dialog result: " + result);
                return ConfirmResult.NO;
        }
    }

    private static class ObjectDeleter implements DBRRunnableWithProgress {
        private final DBECommandContext commander;

        public ObjectDeleter(DBECommandContext commandContext)
        {
            this.commander = commandContext;
        }

        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                commander.saveChanges(monitor);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

}