/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

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
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                        Object object = iter.next();
                        if (object instanceof DBNNode) {
                            selectedNodes.add((DBNNode) object);
                        }
                        String objectValue = getObjectDisplayString(object);
                        if (objectValue == null) {
                            continue;
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
                                selectedNodes},
                            new Transfer[]{
                                TextTransfer.getInstance(),
                                TreeNodeTransfer.getInstance()});
                        ObjectPropertyTester.firePropertyChange(ObjectPropertyTester.PROP_CAN_PASTE);
                    }
                }
            });
        }
        return null;
    }

    protected abstract String getObjectDisplayString(Object object);

}