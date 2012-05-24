/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.qm;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.UIUtils;

public class QueryManagerFilterHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        QueryManagerView view = UIUtils.findView(HandlerUtil.getActiveWorkbenchWindow(event), QueryManagerView.class);
        if (view != null) {
            view.openFilterDialog();
        }
        return null;
    }
}