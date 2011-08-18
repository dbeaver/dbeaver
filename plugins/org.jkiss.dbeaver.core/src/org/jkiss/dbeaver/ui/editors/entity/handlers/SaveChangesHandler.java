/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;

import java.lang.reflect.InvocationTargetException;


public class SaveChangesHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final EntityEditor editor = (EntityEditor)Platform.getAdapterManager().getAdapter(HandlerUtil.getActiveEditor(event), EntityEditor.class);
        if (editor != null) {
            try {
                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        RuntimeUtils.validateAndSave(monitor, editor);
                    }
                });
            } catch (InvocationTargetException e) {
                //UIUtils.showErrorDialog();
            } catch (InterruptedException e) {
                // skip
            }
        }
        return null;
    }

}