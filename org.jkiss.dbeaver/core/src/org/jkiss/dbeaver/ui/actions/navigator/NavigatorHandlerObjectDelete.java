/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
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
                if (element instanceof DBNTreeNode) {
                    deleteObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNTreeNode)element);
                }
                if (deleteAll != null && !deleteAll) {
                    break;
                }
            }
        }
        return null;
    }

    private boolean deleteObject(IWorkbenchWindow workbenchWindow, DBNNode node)
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
        IDatabaseObjectManager<?> objectManager = entityManager.createManager();
        if (!(objectManager instanceof IDatabaseObjectManagerEx<?>)) {
            log.error("Object manager '" + objectManager.getClass().getName() + "' do not supports object deletion");
            return false;
        }

        IDatabaseObjectManagerEx objectManagerEx = (IDatabaseObjectManagerEx)objectManager;
        Map<String, Object> deleteOptions = null;

        objectManagerEx.setObject(node.getObject());
        objectManagerEx.deleteObject(deleteOptions);

        if (!confirmObjectDelete(workbenchWindow, node, objectManager)) {
            objectManagerEx.resetChanges();
            return false;
        }

        // Delete object
        ObjectDeleter deleter = new ObjectDeleter(objectManagerEx);
        try {
            workbenchWindow.run(true, true, deleter);
        } catch (InvocationTargetException e) {
            log.error("Can't delete object", e);
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

        return true;
    }

    private boolean confirmObjectDelete(final IWorkbenchWindow workbenchWindow, final DBNNode node, final IDatabaseObjectManager<?> objectManager)
    {
        if (deleteAll != null) {
            return deleteAll;
        }
        ResourceBundle bundle = DBeaverActivator.getInstance().getResourceBundle();
        String titleKey = ConfirmationDialog.RES_CONFIRM_PREFIX + PrefConstants.CONFIRM_ENTITY_DELETE + "." + ConfirmationDialog.RES_KEY_TITLE;
        String messageKey = ConfirmationDialog.RES_CONFIRM_PREFIX + PrefConstants.CONFIRM_ENTITY_DELETE + "." + ConfirmationDialog.RES_KEY_MESSAGE;

        String nodeTypeName;
        if (node instanceof DBNTreeNode) {
            nodeTypeName = ((DBNTreeNode)node).getMeta().getLabel();
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
                if (objectManager != null) {
                    boolean hasScript = false;
                    for (IDatabaseObjectCommand cmd : objectManager.getCommands()) {
                        if (!CommonUtils.isEmpty(cmd.getPersistActions(objectManager.getObject()))) {
                            hasScript = true;
                            break;
                        }
                    }
                    if (hasScript) {
                        createButton(parent, IDialogConstants.DETAILS_ID, "View Script", false);
                    }
                }
            }
            @Override
            protected void buttonPressed(int buttonId)
            {
                if (buttonId == IDialogConstants.DETAILS_ID) {
                    Collection<? extends IDatabaseObjectCommand> commands = objectManager.getCommands();
                    StringBuilder script = new StringBuilder();
                    for (IDatabaseObjectCommand command : commands) {
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
                        dialog.open();
                    }

                } else {
                    super.buttonPressed(buttonId);
                }
            }
        };
        int result = dialog.open();
        switch (result) {
            case IDialogConstants.YES_ID:
                return true;
            case IDialogConstants.YES_TO_ALL_ID:
                deleteAll = true;
                return true;
            case IDialogConstants.NO_ID:
                return false;
            case IDialogConstants.CANCEL_ID:
            case -1:
                deleteAll = false;
                return false;
            default:
                log.warn("Unsupported confirmation dialog result: " + result);
                return false;
        }
    }

    private static class ObjectDeleter implements IRunnableWithProgress {
        private final IDatabaseObjectManagerEx objectManager;

        public ObjectDeleter(IDatabaseObjectManagerEx objectManagerEx)
        {
            this.objectManager = objectManagerEx;
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