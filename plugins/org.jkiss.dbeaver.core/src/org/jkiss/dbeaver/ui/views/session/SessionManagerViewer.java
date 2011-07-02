/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * SessionManagerViewer
 */
public class SessionManagerViewer
{
    static final Log log = LogFactory.getLog(SessionManagerViewer.class);

    private SessionTable pageControl;
    private Text sessionInfo;
    private Font boldFont;

    public void dispose()
    {
        pageControl.dispose();
        UIUtils.dispose(boldFont);
    }

    public SessionManagerViewer(IWorkbenchPart part, Composite parent, DBAServerSessionManager sessionManager) {
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SashForm sash = UIUtils.createPartDivider(part, composite, SWT.HORIZONTAL | SWT.SMOOTH);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        pageControl = new SessionTable(sash, SWT.NONE, sessionManager);
        pageControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        pageControl.createProgressPanel();

        sessionInfo = new Text(sash, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
        sessionInfo.setEditable(false);
        sessionInfo.setLayoutData(new GridData(GridData.FILL_BOTH));

        sash.setWeights(new int[]{70, 30});
    }

}