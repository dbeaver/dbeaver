/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.qm;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.utils.ViewUtils;

public class QueryManagerFilterHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        QueryManagerView view = ViewUtils.findView(HandlerUtil.getActiveWorkbenchWindow(event), QueryManagerView.class);
        if (view != null) {
            view.openFilterDialog();
        }
        return null;
    }
}