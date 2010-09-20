/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.Iterator;

public class NavigatorCopyHandler extends AbstractHandler {

    public NavigatorCopyHandler() {

    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;

            workbenchWindow.getShell().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                        if (buf.length() > 0) {
                            buf.append('\n');
                        }
                        Object adapted = Platform.getAdapterManager().getAdapter(iter.next(), DBPNamedObject.class);
                        if (adapted != null) {
                            buf.append(ViewUtils.convertObjectToString(adapted));
                        }
                    }
                    if (buf.length() > 0) {
                        Clipboard clipboard = new Clipboard(workbenchWindow.getShell().getDisplay());
                        TextTransfer textTransfer = TextTransfer.getInstance();
                        clipboard.setContents(
                            new Object[]{buf.toString()},
                            new Transfer[]{textTransfer});
                    }
                }
            });
        }
        return null;
    }
}
