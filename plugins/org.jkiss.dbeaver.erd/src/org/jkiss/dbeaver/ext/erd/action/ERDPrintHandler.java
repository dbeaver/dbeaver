/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorAdapter;

public class ERDPrintHandler extends AbstractHandler {
    public ERDPrintHandler() {

    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        Control control = (Control) HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control != null) {
            ERDEditor editor = ERDEditorAdapter.getEditor(control);
            if (editor != null) {
                new PrintAction(editor).run();
            }
        }
        return null;
    }

}
