/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.edit.DBOCommand;
import org.jkiss.dbeaver.model.edit.DBOEditor;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
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
            for (Iterator iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                if (element instanceof DBNDatabaseNode) {
                    deleteObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNDatabaseNode)element);
                }
                if (deleteAll != null && !deleteAll) {
                    break;
                }
            }
        }
        return null;
    }

    private boolean deleteObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node)
    {
        if (!(node.getParentNode() instanceof DBNContainer)) {
            log.error("Node '" + node + "' doesn't have a container");
            return false;
        }

        // Try to delete object using object manager
        DBSObject object = node.getObject();
        if (object == null) {
            log.error("Can't delete node with null object");
            return false;
        }
        EntityManagerDescriptor entityManager = DBeaverCore.getInstance().getEditorsRegistry().getEntityManager(object.getClass());
        if (entityManager == null) {
            log.error("Object manager not found for type '" + object.getClass().getName() + "'");
            return false;
        }
        DBOManager<?> objectManager = entityManager.createManager();
        if (!(objectManager instanceof DBOCreator<?>)) {
            log.error("Object manager '" + objectManager.getClass().getName() + "' do not supports object deletion");
            return false;
        }

        DBOCreator objectCreator = (DBOCreator)objectManager;
        Map<String, Object> deleteOptions = null;

        objectCreator.setObject(node.getObject());

        ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, node, objectManager);
        if (confirmResult == ConfirmResult.NO) {
            return false;
        }

        objectCreator.deleteObject(deleteOptions);

        if (confirmResult == ConfirmResult.DETAILS) {
            if (!showScript(workbenchWindow, (DBOEditor<?>) objectManager)) {
                ((DBOEditor)objectCreator).resetChanges();
                return false;
            }
        }

        if (objectCreator instanceof DBOEditor) {
            // Delete object
            ObjectDeleter deleter = new ObjectDeleter((DBOEditor)objectCreator);
            try {
                workbenchWindow.run(true, true, deleter);
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(workbenchWindow.getShell(), "Can't delete object", null, e.getTargetException());
                return false;
            } catch (InterruptedException e) {
                // do nothing
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
        }

        return true;
    }

    private boolean showScript(IWorkbenchWindow workbenchWindow, DBOEditor<?> objectManager)
    {
        Collection<? extends DBOCommand> commands = objectManager.getCommands();
        StringBuilder script = new StringBuilder();
        for (DBOCommand command : commands) {
            IDatabasePersistAction[] persistActions = command.getPersistActions(objectManager.getObject());
            if (!CommonUtils.isEmpty(persistActions)) {
                for (IDatabasePersistAction action : persistActions) {
                    if (script.length() > 0) {
                        script.append('\n');
                    }
                    script.append(action.getScript());
                    script.append(objectManager.getDataSource().getInfo().getScriptDelimiter());
                }
            }
        }
        DatabaseNavigatorView view = ViewUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
        if (view != null) {
            ViewSQLDialog dialog = new ViewSQLDialog(
                view.getSite(),
                objectManager.getDataSource(),
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
    private ConfirmResult confirmObjectDelete(final IWorkbenchWindow workbenchWindow, final DBNNode node, final DBOManager<?> objectManager)
    {
        if (deleteAll != null) {
            return deleteAll ? ConfirmResult.YES : ConfirmResult.NO;
        }
        ResourceBundle bundle = DBeaverActivator.getInstance().getResourceBundle();
        String titleKey = ConfirmationDialog.RES_CONFIRM_PREFIX + PrefConstants.CONFIRM_ENTITY_DELETE + "." + ConfirmationDialog.RES_KEY_TITLE;
        String messageKey = ConfirmationDialog.RES_CONFIRM_PREFIX + PrefConstants.CONFIRM_ENTITY_DELETE + "." + ConfirmationDialog.RES_KEY_MESSAGE;

        String nodeTypeName;
        if (node instanceof DBNDatabaseNode) {
            nodeTypeName = ((DBNDatabaseNode)node).getMeta().getLabel();
        } else {
            nodeTypeName = "?";
        }

        MessageDialog dialog = new MessageDialog(
            workbenchWindow.getShell(),
            UIUtils.formatMessage(bundle.getString(titleKey), nodeTypeName, node.getNodeName()),
            workbenchWindow.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE),
            UIUtils.formatMessage(bundle.getString(messageKey), nodeTypeName, node.getNodeName()),
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
                if (objectManager instanceof DBOEditor) {
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

    private static class ObjectDeleter implements IRunnableWithProgress {
        private final DBOEditor objectManager;

        public ObjectDeleter(DBOEditor objectCreator)
        {
            this.objectManager = objectCreator;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                objectManager.saveChanges(new DefaultProgressMonitor(monitor));
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

}