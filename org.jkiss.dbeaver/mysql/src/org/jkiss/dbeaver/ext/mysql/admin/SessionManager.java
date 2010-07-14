/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.DBException;

import java.lang.reflect.InvocationTargetException;

/**
 * FolderEditor
 */
public class SessionManager extends SinglePageDatabaseEditor<IDatabaseEditorInput>
{
    static final Log log = LogFactory.getLog(SessionManager.class);

    private ProgressPageControl pageControl;

    public void createPartControl(Composite parent) {

        pageControl = new ProgressPageControl(parent, SWT.NONE, getSite().getPart());
        
        pageControl.createProgressPanel();

        refreshSessions();
    }

    private void refreshSessions()
    {
        LoadingUtils.executeService(
            new AbstractLoadService<Object>("Load active session list") {
                public Object evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    return null;
                }
            },
            pageControl.createVisualizer());
    }

}