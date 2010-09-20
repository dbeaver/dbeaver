/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverManagerDialog;

public class DriverManagerHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {

        DriverManagerDialog dialog = new DriverManagerDialog(HandlerUtil.getActiveShell(event));
        dialog.open();

        return null;
    }
}