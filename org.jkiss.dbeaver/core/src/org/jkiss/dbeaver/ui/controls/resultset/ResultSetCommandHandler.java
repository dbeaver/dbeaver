/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * ResultSetCommandHandler
 */
public class ResultSetCommandHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (!(control instanceof LightGrid)) {
            return null;
        }
        return null;
    }

}