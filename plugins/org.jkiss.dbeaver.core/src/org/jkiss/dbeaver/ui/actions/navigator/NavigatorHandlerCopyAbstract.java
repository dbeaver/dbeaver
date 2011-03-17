/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class NavigatorHandlerCopyAbstract extends AbstractHandler {

    public NavigatorHandlerCopyAbstract() {

    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;

            workbenchWindow.getShell().getDisplay().syncExec(new Runnable() {
                public void run() {
                    List<DBNNode> selectedNodes = new ArrayList<DBNNode>();
                    List<DBPNamedObject> selectedObjects = new ArrayList<DBPNamedObject>();
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                        Object object = iter.next();
                        String objectValue = getObjectDisplayString(object);
                        if (objectValue == null) {
                            continue;
                        }
                        DBNNode node = (DBNNode)Platform.getAdapterManager().getAdapter(object, DBNNode.class);
                        DBPNamedObject dbObject = null;
                        if (node instanceof DBNDatabaseNode) {
                            dbObject = ((DBNDatabaseNode)node).getObject();
                        }
                        if (dbObject == null) {
                            dbObject = (DBPNamedObject)Platform.getAdapterManager().getAdapter(object, DBPNamedObject.class);
                        }
                        if (node != null) {
                            selectedNodes.add(node);
                        }
                        if (dbObject != null) {
                            selectedObjects.add(dbObject);
                        }
                        if (buf.length() > 0) {
                            buf.append(ContentUtils.getDefaultLineSeparator());
                        }
                        buf.append(objectValue);
                    }
                    if (buf.length() > 0) {
                        Clipboard clipboard = new Clipboard(workbenchWindow.getShell().getDisplay());
                        clipboard.setContents(
                            new Object[]{
                                buf.toString(),
                                selectedNodes,
                                selectedObjects},
                            new Transfer[]{
                                TextTransfer.getInstance(),
                                TreeNodeTransfer.getInstance(),
                                DatabaseObjectTransfer.getInstance()});
                        ObjectPropertyTester.firePropertyChange(ObjectPropertyTester.PROP_CAN_PASTE);
                    }
                }
            });
        }
        return null;
    }

    protected abstract String getObjectDisplayString(Object object);

}